package dc;

import java.util.Arrays;
import java.util.HashMap;

import dc.DCPackage;
import cli.Debugger;

public class KeyHandler {
	public static final int KEY_SIZE = DCPackage.PAYLOAD_SIZE;
	
	private HashMap<String, byte[]> keys;
	private byte[] currentKeyMix;

	private int numKeys;
	
	public KeyHandler() {
		keys = new HashMap<String, byte[]>();
		currentKeyMix = new byte[KEY_SIZE];
		Arrays.fill(currentKeyMix, (byte) 0);
	}
	
	public void addKey(String c, byte[] key) {
		if(key.length != KEY_SIZE) {
			System.err.println("[KeyHandler] Severe: The provided key has length " + key.length + " but it has to be of length " + KEY_SIZE + ".");
		} else {
			Debugger.println(2, "[KeyHandler] Adding key " + String.valueOf(key) + " for station " + c + " to keychain.");
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
}
