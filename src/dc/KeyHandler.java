package dc;

import java.util.Arrays;
import java.util.HashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.neilalexander.jnacl.crypto.salsa20;

import dc.DCPackage;
import cli.Debugger;

public class KeyHandler {
	private static MessageDigest md;
	private static final String HASH_ALG = "SHA-256";
	public static final int KEY_SIZE = 1024;
	// Nonce size must not be larger than key size.
	public static final int NONCE_SIZE = 8;
	
	private HashMap<String, KeyNoncePair> keychain;
	
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
		keychain = new HashMap<String, KeyNoncePair>();
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
			synchronized(keychain) {
				KeyNoncePair knp = new KeyNoncePair(key);
				keychain.put(c, knp);
				numKeys++;
			}			
		}
	}
	
	public void removeKey(String c) {
		synchronized(keychain) {
			keychain.remove(c);
			numKeys--;
		}
	}
	
	public byte[] getOutput(int length) {
		return nextKeyMix(length);		
	}
	
	public byte[] getOutput(byte[] message) {
		byte[] output = new byte[message.length];
		byte[] currentKeyMix = nextKeyMix(message.length);
		for(int i = 0; i < message.length; i++) {
			output[i] = (byte) (message[i] ^ currentKeyMix[i % currentKeyMix.length]);
		}
		return output;
	}

	private byte[] nextKeyMix(int length) {
		byte[] keyMix = new byte[length];
		synchronized(keychain) {
			for(KeyNoncePair knp: keychain.values()) {
				knp.encrypt(keyMix);
			}	
		}
		// Debugger.println(1, Arrays.toString(keyMix));
		return keyMix;
	}

	public boolean approved() {
		synchronized(keychain) {
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

	private class KeyNoncePair {
		private final byte[] key;
		private final byte[] nonce;

		public KeyNoncePair(byte[] key) {
			byte[] nonce = new byte[NONCE_SIZE];
			for(int i = 0; i < NONCE_SIZE; i++) {
				nonce[i] = key[i];
			}
			this.key = key;
			this.nonce = nonce;
		}

		public byte[] encrypt(byte[] input) {
			System.out.println("Lenght of input: " + input.length);
			byte[] keystream = new byte[input.length];
			salsa20.crypto_stream(keystream, input.length, nonce, 0, key);
			for(int i = 0; i < input.length; i++) {
				input[i] ^= keystream[i];
			}
			nextNonce();
			return input;
		}

		private void nextNonce() {
			for(int i = 0; i < NONCE_SIZE; i++) {
				byte b = nonce[i];
				if(b == Byte.MAX_VALUE) {
					nonce[i] = Byte.MIN_VALUE;
				} else {
					nonce[i] = ++b;
					return;
				}
			}
		}
	}
}
