package uk.ac.ncl.burton.twy.equalityTestingCorrectness;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import uk.ac.ncl.burton.twy.ZKPoK.PKConfig;
import uk.ac.ncl.burton.twy.ZKPoK.PKProver;
import uk.ac.ncl.burton.twy.ZKPoK.PKVerifier;
import uk.ac.ncl.burton.twy.ZKPoK.components.PKComponentProver;
import uk.ac.ncl.burton.twy.ZKPoK.components.PKRandomnessManager;
import uk.ac.ncl.burton.twy.ZKPoK.utils.BigIntegerUtils;
import uk.ac.ncl.burton.twy.crypto.CyclicGroup;
import uk.ac.ncl.burton.twy.crypto.EEA;
import uk.ac.ncl.burton.twy.crypto.EEAResult;
import uk.ac.ncl.burton.twy.network.NetworkConnectionClient;
import uk.ac.ncl.burton.twy.userInterface.Style;
import uk.ac.ncl.burton.twy.userInterface.TConsole;

public class CorrectnessClient {

	@Test
	public void test() {
		
		TConsole.print(Style.CLEAR);
		
		BigInteger password = BigInteger.ZERO;
		
		NetworkConnectionClient client = new NetworkConnectionClient("localhost", PKConfig.PROTOCOL_PORT);
		Thread clientThread = new Thread(client);
		clientThread.start();
		
		client.setBlockingMode(true);
		
		// ====== PKS ======
		System.out.println("Generating Group...");
		CyclicGroup G = CyclicGroup.generateGroup(256);
		System.out.println("Group Generated");
		
		while( !password.equals(BigInteger.valueOf(99999)) ){
			
			password = password.add(BigInteger.ONE);
			
			BigInteger g = G.getG();
			BigInteger mod = G.getQ();
			
			
			BigInteger a;
			double randomPassword = Math.random();
			if( randomPassword > 0.5 ){
				a = password; // Secret value
			} else {
				a = password.add(BigInteger.ONE);
			}
			
			// === STEP 1 ===
			
			// -- Calculations --
			BigInteger h =  G.generateGenerator();
			BigInteger x1 = RandomZQ(G.getQ());
			BigInteger x2 = RandomZQ(G.getQ());
			BigInteger r =  RandomZQ(G.getQ());
			
			
			long t0 = System.nanoTime();
			BigInteger c =  g.modPow( x1, mod ).multiply( h.modPow( x2, mod) );
			BigInteger u1 = g.modPow(r, mod );
			BigInteger u2 = h.modPow(r, mod );
			BigInteger e =  g.modPow( a, mod ).multiply( c.modPow( r, mod) );
			long t1 = System.nanoTime();
			// -- PoK --
			PKProver peggy = new PKProver(G);
			
			// (1)
			List<BigInteger> bases = new ArrayList<BigInteger>();
			List<BigInteger> exponents = new ArrayList<BigInteger>();
			bases.add(g);
			exponents.add(r);
			PKComponentProver c1X1 = PKComponentProver.generateProver(G, bases, exponents);
			
			// (2)
			bases = new ArrayList<BigInteger>();
			exponents = new ArrayList<BigInteger>();
			bases.add(h);
			exponents.add(r);
			PKComponentProver c1X2 = PKComponentProver.generateProver(G, bases, exponents);
			
			// (3)
			bases = new ArrayList<BigInteger>();
			exponents = new ArrayList<BigInteger>();
			bases.add(g);
			exponents.add(a);
			bases.add(c);
			exponents.add(r);
			PKComponentProver c1X3 = PKComponentProver.generateProver(G, bases, exponents);
			
			// -- Random Exponents --
//			BigInteger c1r1 = c1X1.getRandomExponent(0);
//			c1X2.setRandomExponent(0, c1r1);
//			c1X3.setRandomExponent(1, c1r1);
			
			PKRandomnessManager.transferRandomness(c1X2, 0, c1X1, 0);
			PKRandomnessManager.transferRandomness(c1X3, 1, c1X1, 0);
			
			// -- ADD --
			peggy.addComponent(c1X1);
			peggy.addComponent(c1X2);
			peggy.addComponent(c1X3);
			
			// -- Proof --
			String init = peggy.getJSONInitialise();
			client.sendMessage(init);
			
			String commitment = peggy.getJSONCommitment();
			client.sendMessage(commitment);
			
			String challenge = client.receiveMessage();
			
			String response = peggy.getJSONResponse(challenge);
			client.sendMessage(response);
			
			r = null;
			System.gc();
			
			String passing = peggy.getJSONPassingVariables();
			client.sendMessage(passing);
			
			String outcome = client.receiveMessage();
			System.out.println(outcome);
			
			
			
			
			// === STEP 2 ===
			init = client.receiveMessage();
			PKVerifier victor = PKVerifier.getInstance(init);

			commitment = client.receiveMessage();
			
			challenge = victor.getJSONChallenge(commitment);
			client.sendMessage(challenge);
			
			response = client.receiveMessage();
			passing = client.receiveMessage();
			
			outcome = victor.getJSONOutcome(response, passing);
			client.sendMessage(outcome);
			
			boolean success = victor.isProofSuccessful();
			//System.out.println(success);
			
			
			BigInteger u1d = victor.getValue(0);
			BigInteger u2d = victor.getValue(1);
			BigInteger ed = victor.getValue(2);
			
			// === STEP 3 ===
			
			// -- Calculations --
			BigInteger z = RandomZQ(G.getQ());
			
			
			BigInteger edz = ed.modPow(z, mod);
				BigInteger zx1 = z.multiply(x1);
			BigInteger u1dzx1 = u1d.modPow( zx1 , mod).modInverse(mod);
				BigInteger zx2 = z.multiply(x2);
			BigInteger u2dzx2 = u2d.modPow( zx2 , mod).modInverse(mod);
		
			BigInteger d = edz.multiply(u1dzx1).multiply(u2dzx2).mod(mod);
			
			// GCD PROOF:   PK{(z,q,r1,r2,a,b) : C = g^z h^r1 AND D = g^q h^r2 AND g = C^a D^b
			BigInteger r1 = RandomZQ(G.getQ());
			BigInteger r2 = RandomZQ(G.getQ());
			
			EEAResult eea = EEA.eea(z, G.getQ());
			
			List<BigInteger> baseseea = new ArrayList<BigInteger>();
			List<BigInteger> exponentseea = new ArrayList<BigInteger>();
			baseseea.add(g);
			baseseea.add(h);
			exponentseea.add(z);
			exponentseea.add(r1);
			BigInteger C = BigIntegerUtils.multiplyBaseExponents(mod, baseseea, exponentseea);
			
			baseseea = new ArrayList<BigInteger>();
			exponentseea = new ArrayList<BigInteger>();
			baseseea.add(g);
			baseseea.add(h);
			exponentseea.add(mod);
			exponentseea.add(r2);
			BigInteger D = BigIntegerUtils.multiplyBaseExponents(mod, baseseea, exponentseea);
		
			// -- PoK --
			peggy = new PKProver(G);
			
			// (1)
			bases = new ArrayList<BigInteger>();
			exponents = new ArrayList<BigInteger>();
			bases.add(g);
			exponents.add(x1);
			bases.add(h);
			exponents.add(x2);
			PKComponentProver c3X1 = PKComponentProver .generateProver(G, bases, exponents);
			
			// (2)
			bases = new ArrayList<BigInteger>();
			exponents = new ArrayList<BigInteger>();
			bases.add(ed);
			exponents.add(z);
			bases.add(u1d);
				List<BigInteger> nzx1List = new ArrayList<BigInteger>(); nzx1List.add(BigInteger.valueOf(-1)); nzx1List.add(z); nzx1List.add(x1);
			exponents.add(BigIntegerUtils.multiplyList(nzx1List));
			bases.add(u2d);
				List<BigInteger> nzx2List = new ArrayList<BigInteger>(); nzx2List.add(BigInteger.valueOf(-1)); nzx2List.add(z); nzx2List.add(x2);
			exponents.add(BigIntegerUtils.multiplyList(nzx2List));
			PKComponentProver c3X2 = PKComponentProver .generateProver(G, bases, exponents);
			
			// (3)
			bases = new ArrayList<BigInteger>();
			exponents = new ArrayList<BigInteger>();
			bases.add(g);
			exponents.add(z);
			bases.add(h);
			exponents.add(r1);
			PKComponentProver c3X3 = PKComponentProver.generateProver(G, bases, exponents);
			
			// (4)
			bases = new ArrayList<BigInteger>();
			exponents = new ArrayList<BigInteger>();
			bases.add(g);
			exponents.add(mod);
			bases.add(h);
			exponents.add(r2);
			PKComponentProver c3X4 = PKComponentProver.generateProver(G, bases, exponents);
			
			// (5)
			bases = new ArrayList<BigInteger>();
			exponents = new ArrayList<BigInteger>();
			bases.add(C);
			exponents.add(eea.getS());
			bases.add(D);
			exponents.add(eea.getT());
			PKComponentProver c3X5 = PKComponentProver.generateProver(G, bases, exponents);
			
			// -- Random Exponents --
			PKRandomnessManager.transferRandomness2Multiplyx(c3X2, 1, c3X2, 0 , c3X1, 0 , BigInteger.valueOf(-1));
			PKRandomnessManager.transferRandomness2Multiplyx(c3X2, 2, c3X2, 0 , c3X1, 1 , BigInteger.valueOf(-1));
			
			// -- ADD --
			peggy.addComponent(c3X1);
			peggy.addComponent(c3X2);
			peggy.addComponent(c3X3);
			peggy.addComponent(c3X4);
			peggy.addComponent(c3X5);
			
			// -- Proof --
			init = peggy.getJSONInitialise();
			client.sendMessage(init);
			
			commitment = peggy.getJSONCommitment();
			client.sendMessage(commitment);
			
			challenge = client.receiveMessage();
			
			response = peggy.getJSONResponse(challenge);
			client.sendMessage(response);
			
			x1 = null;
			x2 = null;
			z = null;
			System.gc();
			
			passing = peggy.getJSONPassingVariables();
			client.sendMessage(passing);
			
			outcome = client.receiveMessage();
			System.out.println(outcome);
			
			System.out.println("d: " + d );
			System.out.println( randomPassword );
			System.out.println( a );
			
			if( randomPassword > 0.5 ){
				assertEquals( BigInteger.ONE, d);
			} else {
				assertNotEquals( BigInteger.ONE, d);
			}
			
			
			if( d.equals(BigInteger.ONE)){
				TConsole.println("Passwords Equal", Style.GREEN);
			} else {
				TConsole.println("Passwords Not Equal", Style.RED);
			}
			
		}
		
		client.stop();
	}
	
	private static BigInteger RandomZQ(BigInteger q){
		return BigIntegerUtils.randomBetween(BigInteger.ONE, q);
	}

}
