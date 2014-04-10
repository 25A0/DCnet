package dc;

import java.util.Arrays;
import java.util.HashMap;

import dc.DCPackage;

public class KeyHandler {
	public static final int KEY_SIZE = DCPackage.PAYLOAD_SIZE;
	
	private HashMap<Connection, byte[]> keys;
	private byte[] currentKeyMix;
	
	public KeyHandler() {
		keys = new HashMap<Connection, byte[]>();
		currentKeyMix = new byte[KEY_SIZE];
		Arrays.fill(currentKeyMix, (byte) 0);
	}
	
	public void addKey(Connection c, byte[] key) {
		if(key.length != KEY_SIZE) {
			System.err.println("[KeyHandler] Severe: The provided key has length " + key.length + " but it has to be of length " + KEY_SIZE + ".");
		} else {
			synchronized(keys) {
				keys.put(c, key);
				
				synchronized(currentKeyMix) {
					for(int i = 0; i < KEY_SIZE; i++) {
						currentKeyMix[i] ^= key[i];
					}
				}
			}			
		}
	}
	
	public void removeKey(Connection c) {
		synchronized(keys) {
			byte[] oldKey = keys.remove(c);
			
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
}
