package uk.ac.ncl.burton.twy.ZPK.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import uk.ac.ncl.burton.twy.ZPK.PKConfig;
import uk.ac.ncl.burton.twy.ZPK.PKProver;
import uk.ac.ncl.burton.twy.ZPK.components.PKComponentType;
import uk.ac.ncl.burton.twy.ZPK.network.NetworkedVerifier.Status;



/**
 * 
 * @author twyburton
 *
 */
public class NetworkedProver implements Runnable {
	
	/*
	 * NETWORK PROTCOL:
	 * 
	 * 		== COMMITMENT == P>V
	 * 
	 * 		{
	 * 			"PK_id":"f2b774f6-c712-475a-88b9-d252101f0cb1",
	 * 			"protocol_version":[0,0,1],
	 * 			"step":"commitment",
	 * 			"components":[
	 * 				{"type":"alpha", "t":"00000000000", "component_id":"534a8fe6-3319-42f7-901d-6c9f6f523c77"},
	 * 				{"type":"alpha", "t":"00000000000", "component_id":"ed98678e-b7e6-4faa-af6a-2f5345c47dfc"},
	 * 				{"type":"beta", "t":"00000000000", "component_id":"20fa0d17-28c8-4523-b3ed-9ff2fefa98f6"}
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
	 * 				{"type":"alpha", "c":"00000000000", "component_id":"534a8fe6-3319-42f7-901d-6c9f6f523c77"}
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
	 * 				{"type":"alpha", "s":"00000000000", "component_id":"534a8fe6-3319-42f7-901d-6c9f6f523c77"},
	 * 				{"type":"alpha", "s":"00000000000", "component_id":"ed98678e-b7e6-4faa-af6a-2f5345c47dfc"},
	 * 				{"type":"beta", "s":"00000000000", "component_id":"20fa0d17-28c8-4523-b3ed-9ff2fefa98f6"}
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
	 * 					"type":"alpha", 
	 * 					"component_id":"534a8fe6-3319-42f7-901d-6c9f6f523c77",
	 * 					"values":[
	 * 								"000000","00000","00000"
	 * 							]
	 * 				},
	 * 				{
	 * 					"type":"alpha", 
	 * 					"component_id":"ed98678e-b7e6-4faa-af6a-2f5345c47dfc",
	 *					"values":[
	 * 								"000000","00000","00000"
	 * 							]
	 * 				},
	 * 				{
	 * 					"type":"beta",
	 * 					"component_id":"20fa0d17-28c8-4523-b3ed-9ff2fefa98f6",
	 * 					"values":[
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
	

	private PKProver prover;
	
	public NetworkedProver( PKProver prover ){
		this.prover = prover;
	}
	
	public enum Status {
		INPROGRESS,
		FAILED,
		SUCCESS
	}
	private Status status = Status.INPROGRESS;
	public Status getStatus(){
		return status;
	}

	@Override
	public void run() {
		
		try {
			
			System.out.println("[Prover] Connecting to verifier...");
			Socket socket = new Socket("localhost",PKConfig.PROTOCOL_PORT);
			System.out.println("[Prover] Connected");
			
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			
			if( runNetworkedProof( in, out) )
				status = Status.SUCCESS;
			else
				status = Status.FAILED;
			
			
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	
	
	public boolean runNetworkedProof( InputStream in, OutputStream out) throws IOException{
		
		// == Check Protocol Version ==
		out.write(PKConfig.PROTOCOL_VERSION);
		// Get response
		int vAccept = in.read();
		if( vAccept != 1 ){
			System.out.println("[Prover] Protocol Version Incorrect");
			in.close();
			out.close();
			return false;
		}
		System.out.println("[Prover] Protocol Version Accepted");
		
		// == Perform component format check with verifier ==
		// Basically check that both parties have the expected format for PK
		List<PKComponentType> typeList = prover.getComponentTypeList();
		out.write( typeList.size() );
		
		for( int i = 0 ; i < typeList.size(); i++ ){
			out.write( typeList.get(i).ordinal() );
		}
		
		// Return 1 if accepted else return 0
		vAccept = in.read();
		if( vAccept != 1 ){
			System.out.println("[Prover] Incorrect Component Format");
			in.close();
			out.close();
			return false;
		}
		System.out.println("[Prover] Component Format Accepted");
		
		// == Commitment ==
		// == Challenge ==
		// == Response ==
		// == Passing Variables ==
		// == Verify ==
		
		return true;
	}
	
}
