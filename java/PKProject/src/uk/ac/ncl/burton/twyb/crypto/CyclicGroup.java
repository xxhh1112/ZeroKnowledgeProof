package uk.ac.ncl.burton.twyb.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

import uk.ac.ncl.burton.twyb.ZKPoK.utils.BigIntegerUtils;

public class CyclicGroup {

	// https://en.wikipedia.org/wiki/Schnorr_group
	
	// http://csrc.nist.gov/publications/fips/fips186-3/fips_186-3.pdf ???
	// http://crypto.stanford.edu/~dabo/papers/DDH.pdf
	
	/** Prime order */
	private final BigInteger q;
	/** Large prime */
	private final BigInteger p;
	/** Generator */
	private final BigInteger g;
	
	public CyclicGroup( final BigInteger p , final BigInteger q, final BigInteger g){
		this.p = p;
		this.q = q;
		this.g = g;
		
		if( !this.validate() ) throw new IllegalArgumentException("The group values passed are not valid.");
	}
	
	/**
	 * Get the value of the groups large prime.
	 * @return large prime
	 */
	public BigInteger getP(){
		return p;
	}
	
	/**
	 * Get the value of the groups prime order.
	 * @return prime order
	 */
	public BigInteger getQ(){
		return q;
	}
	
	/**
	 * Get the groups generator
	 * @return generator
	 */
	public BigInteger getG(){
		return g;
	}
	
	/**
	 * Creates another random group generator
	 * @return group generator
	 */
	public BigInteger generateGenerator(){
		
		SecureRandom ran = new SecureRandom();
		BigInteger g = null;
		
		// Generate g
		boolean notDone = true;
		while ( notDone ){
			g = BigIntegerUtils.randomBetween(BigInteger.valueOf(2), p.subtract(BigInteger.ONE), ran);
			
			notDone = false;
			
			if( g.modPow(BigInteger.valueOf(2), p).equals(BigInteger.ONE ) ){
				notDone = true;
			}
			
			if( !g.modPow(q, p).equals( BigInteger.ONE )  ){
				notDone = true;
			}
			
			if( g.equals(this.g ) ){
				notDone = true;
			}

		}
		
		return g;
	}
	
	/**
	 * Check that the group is valid
	 * @return true if the group is valid else false.
	 */
	public boolean validate(){		
		return p.equals( BigInteger.valueOf(2).multiply(q).add(BigInteger.ONE) );
	}
	
	/**
	 * Generate a new CyclicGroup with q bit length of bitLength
	 * @param bitLength the length of q
	 * @return The cyclic group
	 */
	public static CyclicGroup generateGroup( int bitLength ){
		
		SecureRandom ran = new SecureRandom();
		
		
		BigInteger q = null;
		BigInteger p = null;

		// Generate p and q
		//System.out.println("GENERATING PRIME NUMBERS...");
		while (true){
			
			// Generate Prime Order
			q = BigInteger.probablePrime(bitLength, ran);
			// Generate Large Prime
			p = BigInteger.valueOf(2).multiply(q).add(BigInteger.ONE);
			
			if( p.isProbablePrime(100) ){
				break;
			}
			
		}
			
		BigInteger g = null;
		
		// Generate g
		boolean notDone = true;
		while ( notDone ){
			g = BigIntegerUtils.randomBetween(BigInteger.valueOf(2), p.subtract(BigInteger.ONE), ran);
			
			notDone = false;
			
			if( g.modPow(BigInteger.valueOf(2), p).equals(BigInteger.ONE ) ){
				notDone = true;
			}
			
			if( !g.modPow(q, p).equals( BigInteger.ONE )  ){
				notDone = true;
			}

		}
		
		return new CyclicGroup( p , q , g );
		
	}
}
