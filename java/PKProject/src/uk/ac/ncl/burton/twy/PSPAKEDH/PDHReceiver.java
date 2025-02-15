package uk.ac.ncl.burton.twy.PSPAKEDH;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import uk.ac.ncl.burton.twy.ZKPoK.utils.BigIntegerUtils;
import uk.ac.ncl.burton.twy.crypto.Crypto;
import uk.ac.ncl.burton.twy.crypto.CyclicGroup;


public class PDHReceiver extends PDHParty {

	private CyclicGroup G;
	private BigInteger password;
	private BigInteger sender_id;
	private BigInteger receiver_id;
	
	private BigInteger r = BigInteger.valueOf(2); // Random-ish
	
	public PDHReceiver( CyclicGroup G, BigInteger password, BigInteger sender_id, BigInteger receiver_id ){
		this.G = G;
		this.password = password;
		this.sender_id = sender_id;
		this.receiver_id = receiver_id;
	}


	private BigInteger x; // Secret value for DH 
	private BigInteger idP; // Hash of ids and password
	
	
	
	@Override
	public void generateValues() {
		hasGeneratedValues = true;
		
		x = BigIntegerUtils.randomBetween( BigInteger.ONE, G.getQ() );
		
		idP = Crypto.hash( sender_id.add(receiver_id).add(password) ).mod(G.getQ());

	}
	
	
	// Return g^x
	public BigInteger getStep2a(){
		checkValues();
		return G.getG().modPow(x, G.getQ());
	}
	
	private BigInteger m = null; 
	public BigInteger getStep2b( BigInteger m ) throws TerminateProtocolException{
		
		if( m.equals(BigInteger.ZERO)) throw new TerminateProtocolException("m is equal to 0");
		this.m = m;
		
		checkValues();
		DHKey = BigIntegerUtils.divide(m, idP.modPow(r, G.getQ()), G.getQ()).modPow(x, G.getQ());
		
		
		List<BigInteger> hashCheckList = new ArrayList<BigInteger>();
		hashCheckList.add(sender_id);
		hashCheckList.add(receiver_id);
		hashCheckList.add(m);
		hashCheckList.add(G.getG().modPow(x, G.getQ()));
		hashCheckList.add(DHKey);
		hashCheckList.add(password);
		BigInteger k = BigIntegerUtils.multiplyList(hashCheckList).modPow(PDHConfig.AUTHENTICATION_SALT_A, G.getQ());
		
		return Crypto.hash(k );
	}
	
	public void getStepFinal( BigInteger k2 ) throws TerminateProtocolException{
		
		if( m == null ) throw new TerminateProtocolException("m is null. Cannot do final step first.");
		
		List<BigInteger> hashCheckList = new ArrayList<BigInteger>();
		hashCheckList.add(sender_id);
		hashCheckList.add(receiver_id);
		hashCheckList.add(m);
		hashCheckList.add(G.getG().modPow(x, G.getQ()));
		hashCheckList.add(DHKey);
		hashCheckList.add(password);
		BigInteger k2_expected = BigIntegerUtils.multiplyList(hashCheckList).modPow(PDHConfig.AUTHENTICATION_SALT_B, G.getQ());
		
		if( !k2.equals(Crypto.hash(k2_expected) )) throw new TerminateProtocolException("K2 is not as expected");
		
		
		hashCheckList = new ArrayList<BigInteger>();
		hashCheckList.add(sender_id);
		hashCheckList.add(receiver_id);
		hashCheckList.add(m);
		hashCheckList.add(G.getG().modPow(x, G.getQ()));
		hashCheckList.add(DHKey);
		hashCheckList.add(password);
		sessionKey = Crypto.hash(BigIntegerUtils.multiplyList(hashCheckList).modPow(PDHConfig.EXCHANGE_SALT, G.getQ()));
	}

}
