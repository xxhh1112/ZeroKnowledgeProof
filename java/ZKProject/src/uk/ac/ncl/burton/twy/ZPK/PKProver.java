package uk.ac.ncl.burton.twy.ZPK;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import uk.ac.ncl.burton.twy.ZPK.components.PKComponentType;
import uk.ac.ncl.burton.twy.ZPK.components.prover.PKComponentProver;
import uk.ac.ncl.burton.twy.ZPK.components.prover.PKComponentProverBasic;

/**
 * 
 * The PKProver class combines multiple PKComponents into a single proof of knowledge function
 * 
 * @author twyburton
 *
 */
public class PKProver {

	private UUID PK_id = UUID.randomUUID();
	
	private List<PKComponentProver> components = new ArrayList<PKComponentProver>();
	
	public PKProver(){
	}
	
	/**
	 * Add a PKComponent to the components to be prove
	 * @param comp
	 */
	public void addPKComponent( PKComponentProver comp ){
		components.add( comp );
	}
	
	/**
	 * Get the list of commitments to pass to the verifier
	 * @return List of commitments
	 */
	public List<BigInteger> getCommitmentList(){
		if( PKConfig.PRINT_PK_LOG ) System.out.println("Prover Generating Commitments...");
		
		List<BigInteger> commitmentList = new ArrayList<BigInteger>();
		
		for( int i = 0; i < components.size(); i++ ){
			commitmentList.add( components.get(i).getCommitment() );
		}
		
		return commitmentList;
	}
	
	/**
	 * Get the list of responses to send to the verifier 
	 * @param challenge The challenge sent by the verifier
	 * @return list of responses
	 */
	public List<BigInteger> getResponseList( BigInteger challenge ){
		if( PKConfig.PRINT_PK_LOG ) System.out.println("Prover Generating Responses...");
		List<BigInteger> responseList = new ArrayList<BigInteger>();
		
		for( int i = 0; i < components.size(); i++ ){
			responseList.add( components.get(i).getResponse( challenge ) );
		}
		
		return responseList;
	}
	
	/**
	 * Get the list of passing variables to give to the verifier
	 * @return A list of lists of passing variables
	 */
	public List<List<BigInteger>> getPassingVariablesList(){
		List<List<BigInteger>> passingVariablesList = new ArrayList<List<BigInteger>>();
		
		for( int i = 0 ; i < components.size() ; i++ ){
			passingVariablesList.add(components.get(i).getPassingVariables());
		}
		
		return passingVariablesList;
	}
	
	
	/**
	 * Get a list of component types that make up the PK
	 * @return list of component types
	 */
	public List<PKComponentType> getComponentTypeList(){
		
		List<PKComponentType> types = new ArrayList<PKComponentType>();
		for( int i = 0 ; i < components.size(); i++ ){
			types.add( ((PKComponentProverBasic)components.get(i)).getComponentType() );
		}
		
		return types;
	}
	
	
	
	// == JSON Text ==
	public String getJSONCommitment(){
		String json = "";
		
		json += "{\n";
			json += "\t\"PK_id\":\"" + PK_id + "\",\n";
			json += "\t\"protocol_version\":" + Arrays.toString(PKConfig.PROTOCOL_VERSION) + ",\n";
			json += "\t\"step\":\"commitment\",\n";
			json += "\t\"components\":[\n";
				for( int i = 0 ; i < components.size(); i++ ){
					
					json += "\t\t{\n";
						json += "\t\t\t\"type\":\"" +  ((PKComponentProverBasic)components.get(i)).getComponentType()  + "\",\n";
						json += "\t\t\t\"t\":\"" +  components.get(i).getCommitment() + "\",\n";
						json += "\t\t\t\"component_id\":\"" + ((PKComponentProverBasic)components.get(i)).getComponentID() + "\"\n";
					json += "\t\t}";
					
					if( i < components.size()-1 ) json += ",";
					json += "\n";
				}
			json += "\t],\n";
			json += "\t\"time\":" + (System.currentTimeMillis()/1000) + "\n";
		json += "}\n";
		
		return json;
	}
	
	public String getJSONResponse( String JSONchallenge ){
		
		
		try {
			// == JSON PROCESS ==
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(JSONchallenge);
			
		
			JSONArray jComponents = (JSONArray) ((JSONObject)obj).get("components");
			JSONObject JComp0 = (JSONObject) jComponents.get(0);
			BigInteger challenge = new BigInteger( (String)JComp0.get("c") );
			
			// == JSON output ==
			String json = "";
			
			json += "{\n";
				json += "\t\"PK_id\":\"" + PK_id + "\",\n";
				json += "\t\"protocol_version\":" + Arrays.toString(PKConfig.PROTOCOL_VERSION) + ",\n";
				json += "\t\"step\":\"response\",\n";
				json += "\t\"components\":[\n";
					for( int i = 0 ; i < components.size(); i++ ){
						
						json += "\t\t{\n";
							json += "\t\t\t\"type\":\"" +  ((PKComponentProverBasic)components.get(i)).getComponentType()  + "\",\n";
							json += "\t\t\t\"s\":\"" +  components.get(i).getResponse(challenge) + "\",\n";
							json += "\t\t\t\"component_id\":\"" + ((PKComponentProverBasic)components.get(i)).getComponentID() + "\"\n";
						json += "\t\t}";
						
						if( i < components.size()-1 ) json += ",";
						json += "\n";
					}
				json += "\t],\n";
				json += "\t\"time\":" + (System.currentTimeMillis()/1000) + "\n";
			json += "}\n";
			
			return json;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String getJSONPassingVariables(){
		String json = "";
		
		json += "{\n";
			json += "\t\"PK_id\":\"" + PK_id + "\",\n";
			json += "\t\"protocol_version\":" + Arrays.toString(PKConfig.PROTOCOL_VERSION) + ",\n";
			json += "\t\"step\":\"passing\",\n";
			json += "\t\"components\":[\n";
				for( int i = 0 ; i < components.size(); i++ ){
					
					json += "\t\t{\n";
						json += "\t\t\t\"type\":\"" +  ((PKComponentProverBasic)components.get(i)).getComponentType()  + "\",\n";
						json += "\t\t\t\"component_id\":\"" + ((PKComponentProverBasic)components.get(i)).getComponentID() + "\",\n";
						
						json += "\t\t\t\"values\":[\n";
							
							List<BigInteger> passingVs = components.get(i).getPassingVariables();
							for( int j = 0 ; j < passingVs.size(); j++ ){
								json += "\t\t\t\t";
								
								json += "\"" + passingVs.get(j) + "\"";
								
								if( j < passingVs.size()-1 ) json += ",";
								json += "\n";
							}
						json += "\t\t\t]\n";
						
					json += "\t\t}";
					
					if( i < components.size()-1 ) json += ",";
					json += "\n";
				}
			json += "\t],\n";
			json += "\t\"time\":" + (System.currentTimeMillis()/1000) + "\n";
		json += "}\n";
		
		return json;
	}
}
