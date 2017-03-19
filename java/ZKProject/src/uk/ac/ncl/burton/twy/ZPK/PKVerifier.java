package uk.ac.ncl.burton.twy.ZPK;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import uk.ac.ncl.burton.twy.ZPK.components.PKComponentType;
import uk.ac.ncl.burton.twy.ZPK.components.prover.PKComponentProverBasic;
import uk.ac.ncl.burton.twy.ZPK.components.verifier.PKComponentVerifier;
import uk.ac.ncl.burton.twy.ZPK.components.verifier.PKComponentVerifierBasic;

/**
 * 
 * The PKVerifier class combines multiple PKComponents into a single proof of knowledge function
 * 
 * @author twyburton
 *
 */
public class PKVerifier {

	
	private List<PKComponentVerifier> components = new ArrayList<PKComponentVerifier>();
	
	public PKVerifier(){
	}
	
	
	public void addPKComponent( PKComponentVerifier comp ){
		components.add( comp );
	}
	
	
	public BigInteger getChallenge(){
		if( PKConfig.PRINT_PK_LOG ) System.out.println("Verifier Generating Challenge...");
		
		BigInteger challenge = components.get(0).getChallenge();
		
		for( int i = 1; i < components.size(); i++ ){
			components.get(i).setChallenge(challenge);
		}
		
		return challenge;
	}
	
	public boolean verify( List<BigInteger> commitmentList, List<BigInteger> responseList, List<List<BigInteger>> passingVariablesList ){
		if( PKConfig.PRINT_PK_LOG ) System.out.println("Verifier Verifying Transcripts...");
		
		for( int i = 0 ; i < components.size() ; i++){
			if (! components.get(i).verify(commitmentList.get(i), responseList.get(i), passingVariablesList.get(i) ) ){
				if( PKConfig.PRINT_PK_LOG ) System.out.println("Proof Failed!");
				return false;
			}
		}
		
		if( PKConfig.PRINT_PK_LOG ) System.out.println("Proof Succeeded!");
		return true;
	}
	
	
	/**
	 * Get a list of component types that make up the PK
	 * @return list of component types
	 */
	public List<PKComponentType> getComponentTypeList(){
		
		List<PKComponentType> types = new ArrayList<PKComponentType>();
		for( int i = 0 ; i < components.size(); i++ ){
			types.add( ((PKComponentVerifierBasic)components.get(i)).getComponentType() );
		}
		
		return types;
	}
	
	
	
	private String PK_id;
	
	// == JSON Text ==
	public String getJSONCommitment( String JSONcommitment ){
		
		try {
			// == JSON PROCESS ==
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject) parser.parse(JSONcommitment);
			
			PK_id =  (String)obj.get("PK_id") ;
			
			// == JSON output ==
			String json = "";
			
			json += "{\n";
				json += "\t\"PK_id\":\"" + PK_id + "\",\n";
				json += "\t\"protocol_version\":" + Arrays.toString(PKConfig.PROTOCOL_VERSION) + ",\n";
				json += "\t\"step\":\"challenge\",\n";
				json += "\t\"components\":[\n";
			
						json += "\t\t{\n";
							json += "\t\t\t\"type\":\"" +  ((PKComponentVerifierBasic)components.get(0)).getComponentType()  + "\",\n";
							json += "\t\t\t\"c\":\"" +  getChallenge() + "\",\n";
							json += "\t\t\t\"component_id\":\"" + ((PKComponentVerifierBasic)components.get(0)).getComponentID() + "\"\n";
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
				BigInteger s = new BigInteger((String)responseComps.get("s"));
				
				JSONObject passingComps =  (JSONObject) ((JSONArray)jPassing.get("components")).get(i);
				JSONArray psArr =  (JSONArray)passingComps.get("values");

				List<BigInteger> passingVariablesList = new ArrayList<BigInteger>();
				for( int j = 0 ; j < psArr.size(); j++){
					passingVariablesList.add( new BigInteger((String) psArr.get(j)) );
				}
				
				if (! components.get(i).verify( t, s, passingVariablesList ) ){
					successful = false;
				}
				
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
				json += "\t\"protocol_version\":" + Arrays.toString(PKConfig.PROTOCOL_VERSION) + ",\n";
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
