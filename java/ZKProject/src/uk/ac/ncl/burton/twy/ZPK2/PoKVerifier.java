package uk.ac.ncl.burton.twy.ZPK2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import uk.ac.ncl.burton.twy.ZPK.PKConfig;
import uk.ac.ncl.burton.twy.ZPK.components.verifier.PKComponentVerifierBasic;
import uk.ac.ncl.burton.twy.ZPK2.components.PoKComponentProver;
import uk.ac.ncl.burton.twy.ZPK2.components.PoKComponentVerifier;
import uk.ac.ncl.burton.twy.maths.CyclicGroup;

public class PoKVerifier {

	private CyclicGroup G;
	
	public PoKVerifier(CyclicGroup G, int numberComponents){
		this.G = G;
		
		for( int i = 0 ; i < numberComponents; i++ ){
			components.add( new PoKComponentVerifier(G));
		}
	}
	
	private List<PoKComponentVerifier> components = new ArrayList<PoKComponentVerifier>();
	
	/**
	 * Get the challenge value. Also ensures the challenge value is set for all components
	 * @return
	 */
	private BigInteger getChallenge(){
		
		BigInteger challenge = components.get(0).getChallenge();
		
		for( int i = 1; i < components.size(); i++ ){
			components.get(i).setChallenge(challenge);
		}
		
		return challenge;
	}
	
	
	private String PK_id;
	
	
	// == JSON Text ==
	public String getJSONChallenge( String JSONcommitment ){
		
		try {
			// == JSON PROCESS ==
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject) parser.parse(JSONcommitment);
			
			PK_id =  (String)obj.get("PK_id") ;
			
			// == JSON output ==
			String json = "";
			
			json += "{\n";
				json += "\t\"PK_id\":\"" + PK_id + "\",\n";
				json += "\t\"protocol_version\":" + Arrays.toString(PoKConfig.PROTOCOL_VERSION) + ",\n";
				json += "\t\"step\":\"challenge\",\n";
				json += "\t\"components\":[\n";
			
						json += "\t\t{\n";
							json += "\t\t\t\"c\":\"" +  getChallenge() + "\",\n";
							json += "\t\t\t\"component_id\":\"" + components.get(0).getComponentID() + "\"\n";
						json += "\t\t}";
						
				json += "\t],\n";
				json += "\t\"time\":" + (System.currentTimeMillis()/1000) + "\n";
			json += "}\n";
			
			return json;
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	public String getJSONOutcome( String JSONcommitment, String JSONresponse, String JSONpassing ){
		
		boolean successful = true;
		
		try {
			// == JSON PROCESS ==
			JSONParser parser = new JSONParser();
			JSONObject jCommitment = (JSONObject) parser.parse(JSONcommitment);
			JSONObject jResponse = (JSONObject) parser.parse(JSONresponse);
			JSONObject jPassing = (JSONObject) parser.parse(JSONpassing);
			
			String PK_id_commitment =  (String)jCommitment.get("PK_id") ;
			String PK_id_response =  (String)jCommitment.get("PK_id") ;
			String PK_id_passing =  (String)jCommitment.get("PK_id") ;
			
			
			if( !( PK_id.equals(PK_id_commitment) && PK_id.equals(PK_id_response) && PK_id.equals(PK_id_passing)) ){
				successful = false;
			}
			
			
			for( int i = 0 ; i < components.size() ; i++){
				
				JSONObject commitmentComps = (JSONObject) ((JSONArray)jCommitment.get("components")).get(i);
				JSONObject responseComps = (JSONObject) ((JSONArray)jResponse.get("components")).get(i);
				
				BigInteger t = new BigInteger((String)commitmentComps.get("t"));
				// Ger response list
				JSONArray sList =  (JSONArray)responseComps.get("s");
				List<BigInteger> responseList = new ArrayList<BigInteger>();
				for( int j = 0 ; j < sList.size(); j++){
					responseList.add( new BigInteger((String) sList.get(j)) );
				}
				
				// Get base list
				JSONObject passingComps =  (JSONObject) ((JSONArray)jPassing.get("components")).get(i);
				JSONArray psArr =  (JSONArray)passingComps.get("bases");
				BigInteger passingValue = new BigInteger((String)passingComps.get("value"));

				List<BigInteger> passingBasesList = new ArrayList<BigInteger>();
				for( int j = 0 ; j < psArr.size(); j++){
					passingBasesList.add( new BigInteger((String) psArr.get(j)) );
				}
				
				if (! components.get(i).verify(passingBasesList, responseList, t, passingValue ) ){
					successful = false;
				}
				
				//List<BigInteger> bases, List<BigInteger> responses, BigInteger commitment, BigInteger value 
				
			}
			
			
			int outcome = 0;
			if( successful ){
				outcome = 1;
				if( PKConfig.PRINT_PK_LOG ) System.out.println("Proof Succeeded!");
			} else {
				if( PKConfig.PRINT_PK_LOG ) System.out.println("Proof Failed!");
			}
			
			// == JSON output ==
			String json = "";
			
			json += "{\n";
				json += "\t\"PK_id\":\"" + PK_id + "\",\n";
				json += "\t\"protocol_version\":" + Arrays.toString(PoKConfig.PROTOCOL_VERSION) + ",\n";
				json += "\t\"step\":\"outcome\",\n";
				json += "\t\"outcome\":" + outcome + ",\n";
				json += "\t\"time\":" + (System.currentTimeMillis()/1000) + "\n";
			json += "}\n";
			
			return json;
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}