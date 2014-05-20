package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class HashUtil {
	public static String SHA_256 = "SHA-256";
	
	private MessageDigest md;

	public HashUtil(String algorithm) {

		try {
			md = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			System.err.println("[HashUtil] Severe: The algorithm "+algorithm+" is not available. Make sure that your JRE provides an implementation of " + algorithm);
			e.printStackTrace();
			System.exit(1);
		}
	}

	public synchronized byte[] digest(byte[] input) {
		md.update(input);
		byte[] hash = md.digest();
		return hash;
	}
}