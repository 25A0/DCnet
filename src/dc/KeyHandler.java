package dc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;

import com.neilalexander.jnacl.crypto.salsa20;

import com.neilalexander.jnacl.crypto.salsa20;

import dc.DCPackage;
import cli.Debugger;
import util.HashUtil;

import net.Network;

public class KeyHandler {
	private static HashUtil hu = new HashUtil(HashUtil.SHA_256);
	public static final int KEY_SIZE = 32;
	// Nonce size must not be larger than key size.
	public static final int NONCE_SIZE = 8;
	
	private HashMap<String, KeyNoncePair> keychain;
	
	private final String alias;

	private int numKeys;

	public KeyHandler(String alias) {
		this.alias = alias;
		keychain = new HashMap<String, KeyNoncePair>();
	}

	public void addKey(String foreignAlias)  {
		String baseString = symmetricConcat(alias, foreignAlias);
		byte[] hash = hu.digest(baseString.getBytes());
		addKey(foreignAlias, hash);
	}
	
	public void addKey(String c, byte[] key) {
		if(key.length != KEY_SIZE) {
			System.err.println("[KeyHandler] Severe: The provided key has length " + key.length + " but it has to be of length " + KEY_SIZE + ".");
		} else {
			Debugger.println("keys", "[KeyHandler] Adding key " + Arrays.toString(key) + " for station " + c + " to keychain.");
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
	
	public byte[] getOutput(int length, Collection<String> members) {
		Collection<String> cutSet = getCutSet(members);
		return nextKeyMix(length, cutSet);		
	}
	
	public byte[] getOutput(byte[] scheduling, byte[] message, Collection<String> members) {
		int length = scheduling.length + message.length;
		Collection<String> cutSet = getCutSet(members);
		byte[] output = new byte[length];
		byte[] currentKeyMix = nextKeyMix(length, cutSet);
		for(int i = 0; i < scheduling.length; i++) {
			output[i] = (byte) (scheduling[i] ^ currentKeyMix[i]);
		}
		for(int i = 0; i < message.length; i++) {
			// TODO: recurring key. issue?
			output[i + scheduling.length] = (byte) (message[i] ^ currentKeyMix[i % currentKeyMix.length]);
		}
		return output;
	}

	/**
	 * Returns the next key mix, given the set of aliases that should be used.
	 * @param length The desired length of the key
	 * @param members The set of aliases that will determine which keys will be used.
	 * @return  A byte array that contains the combined keys of all aliases that were found in {@code members}.
	 */
	private byte[] nextKeyMix(int length, Collection<String> members) {
		byte[] keyMix = new byte[length];
		synchronized(keychain) {
			for(String alias: members) {
				KeyNoncePair knp = keychain.get(alias);
				knp.encrypt(keyMix);
			}	
		}
		Debugger.println("keys", "[KeyHandler] Station "+ alias+ " has keyMix: " + Arrays.toString(keyMix));
		return keyMix;
	}

	public boolean approved(Collection<String> networkMembers) {
		Collection<String> cutSet = getCutSet(networkMembers);
		return cutSet.size() >= DCConfig.MIN_NUM_KEYS;
	}

	/**
	 * Draws a subset from the passed set that contains all
	 * entries of the passed set that are known by this 
	 * KeyHandler.
	 * @param  networkMembers The collection of members on this network
	 * @return                The cut-set of {@code networkMembers} with {@code keychain.keySet()}.
	 */
	private Collection<String> getCutSet(Collection<String> networkMembers) {
		synchronized(keychain) {
			ArrayList<String> cutSet = new ArrayList<String>();
			// networkMembers might be bigger than keychain.keySet()
			// but lookups are faster in HashMaps than in Collections.
			for(String s: networkMembers) {
				if(keychain.containsKey(s)) {
					cutSet.add(s);
				}
			}
			return cutSet;
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
