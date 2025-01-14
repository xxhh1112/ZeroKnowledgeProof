package uk.ac.ncl.burton.twy.ZKPoK;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import uk.ac.ncl.burton.twy.ZKPoK.components.PKComponentProver;
import uk.ac.ncl.burton.twy.crypto.CyclicGroup;

public final class PKProver {

	/** The id for the proof of knowledge */
	private final UUID PK_id = UUID.randomUUID();
	
	/** The cyclic group for the proof */
	private final CyclicGroup G;
	
	public PKProver(final CyclicGroup G){
		this.G = G;
	}
	
	/** The list of components for the proof */
	private final List<PKComponentProver> components = new ArrayList<PKComponentProver>();
	
	/**
	 * Add a component to the poof
	 * @param comp a component
	 */
	public void addComponent( final PKComponentProver comp ){
		components.add(comp);
	}
	
	/**
	 * Add multiple components to the proof
	 * @param comps A list of components
	 */
	public void addComponents( final List<PKComponentProver> comps ){
		components.addAll(comps);
	}
	
	
	// == JSON Text ==
	
	
	/**
	 * Get the initialisation JSON string. The is used to initialise a PKVerifier.
	 * @return the initialisation JSON string
	 */
	public String getJSONInitialise(){
		String json = "";
		
		json += "{\n";
			json += "\t\"PK_id\":\"" + PK_id + "\",\n";
			json += "\t\"protocol_version\":" + Arrays.toString(PKConfig.PROTOCOL_VERSION) + ",\n";
			json += "\t\"step\":\"initialise\",\n";
			
			json += "\t\"group\":{\n";
				json += "\t\t\"generator\":\"" + G.getG() + "\"\n";
				json += "\t\t\"modulus\":\"" + G.getQ() + "\"\n";
				json += "\t\t\"p\":\"" + G.getP() + "\"\n";
			json += "\t},\n";
				
			json += "\t\"components\":[\n";
				for( int i = 0 ; i < components.size(); i++ ){
					
					json += "\t\t{\n";
						
						json += "\t\t\t\"component_id\":\"" + components.get(i).getComponentID() + "\",\n";
						json += "\t\t\t\"nBases\":" +  components.get(i).getBases().size() + "\n";
					json += "\t\t}";
					
					if( i < components.size()-1 ) json += ",";
					json += "\n";
				}
			json += "\t],\n";
			json += "\t\"time\":" + (System.currentTimeMillis()/1000) + "\n";
		json += "}\n";
		
		log("Initialise string created");
		
		return json;
	}
	
	
	/**
	 * Get the commitment JSON string. This is used to make a commitment to a PKVerifier.
	 * @return the commitment JSON string.
	 */
	public String getJSONCommitment(){
		String json = "";
		
		json += "{\n";
			json += "\t\"PK_id\":\"" + PK_id + "\",\n";
			json += "\t\"protocol_version\":" + Arrays.toString(PKConfig.PROTOCOL_VERSION) + ",\n";
			json += "\t\"step\":\"commitment\",\n";
			json += "\t\"components\":[\n";
				for( int i = 0 ; i < components.size(); i++ ){
					
					json += "\t\t{\n";
						json += "\t\t\t\"t\":\"" +  components.get(i).getCommitment() + "\",\n";
						json += "\t\t\t\"component_id\":\"" + components.get(i).getComponentID() + "\"\n";
					json += "\t\t}";
					
					if( i < components.size()-1 ) json += ",";
					json += "\n";
				}
			json += "\t],\n";
			json += "\t\"time\":" + (System.currentTimeMillis()/1000) + "\n";			
		json += "}\n";
		
		log("Commitment string created");
		
		return json;
	}
	
	
	/**
	 * Get the response JSON string. This is used by the PKVerifier to verify the ZKP.
	 * @param JSONchallenge the challenge JSON string from the PKVerifier.
	 * @return the response JSON string.
	 */
	public String getJSONResponse( final String JSONchallenge ){
		
		
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
							json += "\t\t\t\"s\":[\n";
								//+  components.get(i).getResponse(challenge) +
								List<BigInteger> ss = components.get(i).getResponse(challenge);
								for( int j = 0 ; j < ss.size(); j++){
									json += "\t\t\t\t\"" + ss.get(j) + "\"";
									if( j < ss.size()-1 ) json += ",";
									json += "\n";
								}
								
							json += "\t\t\t],\n";
							json += "\t\t\t\"component_id\":\"" + components.get(i).getComponentID() + "\"\n";
						json += "\t\t}";
						
						if( i < components.size()-1 ) json += ",";
						json += "\n";
					}
				json += "\t],\n";
				json += "\t\"time\":" + (System.currentTimeMillis()/1000) + "\n";
			json += "}\n";
			
			log("Response string created");
			
			// Call garbage collector to destroy random exponents.
			System.gc();
			
			return json;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Get the passing variables JSON string. This is used to give the PKVerifier the value and bases.
	 * @return the passing variables JSON string.
	 */
	public String getJSONPassingVariables(){
		String json = "";
		
		json += "{\n";
			json += "\t\"PK_id\":\"" + PK_id + "\",\n";
			json += "\t\"protocol_version\":" + Arrays.toString(PKConfig.PROTOCOL_VERSION) + ",\n";
			json += "\t\"step\":\"passing\",\n";
			json += "\t\"components\":[\n";
				for( int i = 0 ; i < components.size(); i++ ){
					
					json += "\t\t{\n";
						json += "\t\t\t\"component_id\":\"" + components.get(i).getComponentID() + "\",\n";
						json += "\t\t\t\"value\":\"" + components.get(i).getValue() + "\",\n";
						json += "\t\t\t\"bases\":[\n";
							
							List<BigInteger> passingBases = components.get(i).getBases();
							for( int j = 0 ; j < passingBases.size(); j++ ){
								json += "\t\t\t\t";
								
								json += "\"" + passingBases.get(j) + "\"";
								
								if( j < passingBases.size()-1 ) json += ",";
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
		
		log("Passing string created");
		
		return json;
	}
	
	
	
	
	
	
	/*
	 * NETWORK PROTCOL: [SLIGHTLY DIFFERENT?!]
	 * 
	 * 		== COMMITMENT == P>V
	 * 
	 * 		{
	 * 			"PK_id":"f2b774f6-c712-475a-88b9-d252101f0cb1",
	 * 			"protocol_version":[0,0,1],
	 * 			"step":"commitment",
	 * 			"components":[
	 * 				{"t":"00000000000", "component_id":"534a8fe6-3319-42f7-901d-6c9f6f523c77"},
	 * 				{"t":"00000000000", "component_id":"ed98678e-b7e6-4faa-af6a-2f5345c47dfc"},
	 * 				{"t":"00000000000", "component_id":"20fa0d17-28c8-4523-b3ed-9ff2fefa98f6"}
	 * 			],
	 * 			"time":1489844473
	 * 
	 * 		}
	 * 
	 *		== CHALLENGE == V>P
	 * 
	 * 		{
	 * 			"PK_id":"f2b774f6-c712-475a-88b9-d252101f0cb1",
	 * 			"protocol_version":[0,0,1],
	 * 			"step":"challenge",
	 * 			"components":[
	 * 				{"c":"00000000000", "component_id":"534a8fe6-3319-42f7-901d-6c9f6f523c77"}
	 * 			],
	 * 			"time":1489844473
	 * 
	 * 		}
	 * 
	 * 		== RESPONSE == P>V
	 * 
	 * 		{
	 * 			"PK_id":"f2b774f6-c712-475a-88b9-d252101f0cb1",
	 * 			"protocol_version":[0,0,1],
	 * 			"step":"response",
	 * 			"components":[
	 * 				{"s":"00000000000", "component_id":"534a8fe6-3319-42f7-901d-6c9f6f523c77"},
	 * 				{"s":"00000000000", "component_id":"ed98678e-b7e6-4faa-af6a-2f5345c47dfc"},
	 * 				{"s":"00000000000", "component_id":"20fa0d17-28c8-4523-b3ed-9ff2fefa98f6"}
	 * 			],
	 * 			"time":1489844473
	 * 
	 * 		}
	 * 
	 * 		== PASSING VARIABLES == P>V
	 * 
	 * 		{
	 * 			"PK_id":"f2b774f6-c712-475a-88b9-d252101f0cb1",
	 * 			"protocol_version":[0,0,1],
	 * 			"step":"passing",
	 * 			"components":[
	 * 				{	
	 * 					"component_id":"534a8fe6-3319-42f7-901d-6c9f6f523c77",
	 * 					"value":"000000",
	 * 					"bases":[
	 * 								"000000","00000","00000"
	 * 							]
	 * 				},
	 * 				{
	 * 					"component_id":"ed98678e-b7e6-4faa-af6a-2f5345c47dfc",
	 * 					"value":"000000",
	 *					"bases":[
	 * 								"000000","00000","00000"
	 * 							]
	 * 				},
	 * 				{
	 * 					"component_id":"20fa0d17-28c8-4523-b3ed-9ff2fefa98f6",
	 * 					"value":"000000",
	 * 					"bases":[
	 * 								"000000","00000","00000"
	 * 							]
	 * 				}
	 * 			],
	 * 			"time":1489844473
	 * 
	 * 		}
	 * 
	 * 
	 * 		== OUTCOME == P>V
	 * 
	 * 		{
	 * 			"PK_id":"f2b774f6-c712-475a-88b9-d252101f0cb1",
	 * 			"protocol_version":[0,0,1],
	 * 			"step":"outcome",
	 * 			"outcome":1,
	 * 			"time":1489844473
	 * 		}
	 * 
	 * 
	 * 
	 */
	
	/**
	 * Log a message to console if logging flag is true
	 * @param msg the message
	 */
	private void log(String msg ){
		if( PKConfig.PRINT_PK_LOG ) System.out.println("[" + PK_id + "][Prover] " + msg);
	}
}
