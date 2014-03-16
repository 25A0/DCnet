package dc;

import java.math.BigInteger;
import java.util.Arrays;

public class DCMessage {
	private byte[] payload;

	public static final int PAYLOAD_SIZE = 16;
	
	public DCMessage(byte[] payload) {
		if(payload.length > PAYLOAD_SIZE) {
			System.err.println("[DCMessage] Severe: Rejecting input " + 
				String.valueOf(payload) + " since it is too much for a single message.");
		} else {
			this.payload = payload;
		}
	}
	
	public String toString() {
		return String.valueOf(payload);
	}
	
	/**
	 * Transforms a String into a set of messages. The string is subdivided into as little 
	 * messages as possible, so that only the last message might not fill out the maximum
	 * payload.
	 *
	 * @return A list of messages, in such a way that concatenating the messages as they 
	 * appear in the array will give you the original string.
	 */
	public static DCMessage[] getMessages(String s) {
		int numMessages = s.length() / PAYLOAD_SIZE + 1;
		DCMessage[] messages = new DCMessage[numMessages];
		for(int i = 0; i < numMessages; i++) {
			int start = i*PAYLOAD_SIZE;
			int end = Math.min(s.length(), start + PAYLOAD_SIZE - 1);
			byte[] bytes = s.substring(start, end).getBytes();
			messages[i] = new DCMessage(bytes);
		}
		return messages;
	}
	
	public byte[] toByteArray() {
		return payload;
	}

}
