package uk.ac.ncl.burton.twy.PSPAKEDH;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import uk.ac.ncl.burton.twy.ZKPoK.utils.BigIntegerUtils;
import uk.ac.ncl.burton.twy.crypto.Crypto;
import uk.ac.ncl.burton.twy.crypto.CyclicGroup;

public class PDHSender extends PDHParty {

	
	private CyclicGroup G;
	private BigInteger password;
	private BigInteger sender_id;
	private BigInteger receiver_id;
	
	private BigInteger r = BigInteger.valueOf(2); // Random-ish
	
	public PDHSender( CyclicGroup G, BigInteger password, BigInteger sender_id, BigInteger receiver_id ){
		this.G = G;
		this.password = password;
		this.sender_id = sender_id;
		this.receiver_id = receiver_id;
	}
	
	
	private BigInteger x; // Secret value for DH 
	private BigInteger idP; // Hash of ids and password
	
	private BigInteger m; // Step 1 message
	
	@Override
	public void generateValues() {
		hasGeneratedValues = true;
		
		x = BigIntegerUtils.randomBetween( BigInteger.ONE, G.getQ() );
		
		idP = Crypto.hash( sender_id.add(receiver_id).add(password) ).mod(G.getQ());
		m = G.getG().modPow(x, G.getQ()).multiply( idP.modPow(r, G.getQ()) ).mod(G.getQ());
	}
	
	public BigInteger getStep1(){
		checkValues();
		return m;
	}

	
	public BigInteger getStep3( BigInteger B, BigInteger k) throws TerminateProtocolException{
		
		DHKey = B.modPow(x, G.getQ());
		
		List<BigInteger> hashCheckList = new ArrayList<BigInteger>();
		hashCheckList.add(sender_id);
		hashCheckList.add(receiver_id);
		hashCheckList.add(m);
		hashCheckList.add(B);
		hashCheckList.add(DHKey);
		hashCheckList.add(password);
		BigInteger k_expected = BigIntegerUtils.multiplyList(hashCheckList).modPow(PDHConfig.AUTHENTICATION_SALT_A, G.getQ());
	
		
		if( !k.equals( Crypto.hash(k_expected ) ) ) throw new TerminateProtocolException("k invalid");
		
		hashCheckList = new ArrayList<BigInteger>();
		hashCheckList.add(sender_id);
		hashCheckList.add(receiver_id);
		hashCheckList.add(m);
		hashCheckList.add(B);
		hashCheckList.add(DHKey);
		hashCheckList.add(password);
		BigInteger k2_pre = BigIntegerUtils.multiplyList(hashCheckList).modPow(PDHConfig.AUTHENTICATION_SALT_B, G.getQ());
		
		hashCheckList = new ArrayList<BigInteger>();
		hashCheckList.add(sender_id);
		hashCheckList.add(receiver_id);
		hashCheckList.add(m);
		hashCheckList.add(B);
		hashCheckList.add(DHKey);
		hashCheckList.add(password);
		sessionKey = Crypto.hash(BigIntegerUtils.multiplyList(hashCheckList).modPow(PDHConfig.EXCHANGE_SALT, G.getQ()));
		
		return Crypto.hash(k2_pre);
	}
}
