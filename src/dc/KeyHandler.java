package dc;

import java.util.Arrays;
import java.util.HashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.neilalexander.jnacl.crypto.salsa20;

import com.neilalexander.jnacl.crypto.salsa20;

import dc.DCPackage;
import cli.Debugger;

public class KeyHandler {
	private static MessageDigest md;
	private static final String HASH_ALG = "SHA-256";
	public static final int KEY_SIZE = 32;
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
	
	/**
	 * Turns plain messages into encrypted output
	 * @param  scheduling The byte array of scheduling information
	 * @param  message    The byte array containing the message payload
	 * @return            A byte array that contains the encrypted concatenation of both inputs
	 */
	public byte[] getOutput(byte[] scheduling, byte[] message) {
		int length = scheduling.length + message.length;
		byte[] output = new byte[length];
		byte[] currentKeyMix = nextKeyMix(length);
		for(int i = 0; i < scheduling.length; i++) {
			output[i] = (byte) (scheduling[i] ^ currentKeyMix[i]);
		}
		for(int i = 0; i < message.length; i++) {
			output[i + scheduling.length] = (byte) (message[i] ^ currentKeyMix[i % currentKeyMix.length]);
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
		Debugger.println(2, "[KeyHandler] Station "+ alias+ " has keyMix: " + Arrays.toString(keyMix));
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
			int kl = key.length, il = input.length;
			int iterations = il / kl;
			if(il % kl > 0) iterations++;
			for(int i = 0; i < iterations; i++) {
				byte[] keystream = new byte[kl];
				salsa20.crypto_stream(keystream, kl, nonce, 0, key);
				int lim = (i+1) * kl;
				// In the last iteration, the input length is our limit
				if(lim > il) lim = il;
				for(int n = i * kl; n < lim; n++) {
					input[n] ^= keystream[n % kl];
				}
				nextNonce();				
			}
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
