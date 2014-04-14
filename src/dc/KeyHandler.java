package dc;

import java.util.Arrays;
import java.util.HashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dc.DCPackage;
import cli.Debugger;

public class KeyHandler {
	private static MessageDigest md;
	private static final String HASH_ALG = "SHA-256";
	public static final int KEY_SIZE = 32;
	
	private HashMap<String, byte[]> keys;
	private byte[] currentKeyMix;

	private final String alias;

	private int numKeys;

	static {
		try {
			md = MessageDigest.getInstance(HASH_ALG);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			System.err.println("[KeyHandler] Severe: The algorithm "+HASH_ALG+" is not available. Make sure that your JRE provides an implementation of " + HASH_ALG);
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public KeyHandler(String alias) {
		this.alias = alias;
		keys = new HashMap<String, byte[]>();
		currentKeyMix = new byte[KEY_SIZE];
		Arrays.fill(currentKeyMix, (byte) 0);
	}

	public void addKey(String foreignAlias)  {
		String baseString = symmetricConcat(alias, foreignAlias);
		md.update(baseString.getBytes());
		byte[] hash = md.digest();
		addKey(foreignAlias, hash);
	}
	
	public void addKey(String c, byte[] key) {
		if(key.length != KEY_SIZE) {
			System.err.println("[KeyHandler] Severe: The provided key has length " + key.length + " but it has to be of length " + KEY_SIZE + ".");
		} else {
			Debugger.println(2, "[KeyHandler] Adding key " + Arrays.toString(key) + " for station " + c + " to keychain.");
			synchronized(keys) {
				keys.put(c, key);
				numKeys++;
				
				synchronized(currentKeyMix) {
					for(int i = 0; i < KEY_SIZE; i++) {
						currentKeyMix[i] ^= key[i];
					}
				}
			}			
		}
	}
	
	public void removeKey(String c) {
		synchronized(keys) {
			byte[] oldKey = keys.remove(c);
			numKeys--;
			synchronized(currentKeyMix) {
				for(int i = 0; i < KEY_SIZE; i++) {
					currentKeyMix[i] ^= oldKey[i];
				}
			}
		}
	}
	
	public byte[] getOutput() {
		return currentKeyMix;		
	}
	
	public byte[] getOutput(byte[] message) {
		byte[] output = new byte[message.length];
		synchronized(currentKeyMix) {
			for(int i = 0; i < message.length; i++) {
				output[i] = (byte) (message[i] ^ currentKeyMix[i % currentKeyMix.length]);
			}
		}
		return output;
	}

	public boolean approved() {
		synchronized(keys) {
			return numKeys >= DCConfig.MIN_NUM_KEYS;
		}
	}

	private String symmetricConcat(String a1, String a2) {
		String s1, s2;
		if(a1.length() <= a2.length()) {
			s1 = a1;
			s2 = a2;
		} else {
			s1 = a2;
			s2 = a1;
		}

		for(int i = 0; i < s1.length(); i++) {
			if(s1.charAt(i) < s2.charAt(i)) {
				return s1+s2;
			} else if(s1.charAt(i) > s2.charAt(i)) {
				return s2 + s1;
			}
		}
		return s1 + s2;
	}
}
