package dc;

import java.math.BigInteger;
import java.util.Arrays;

public class DCMessage {
	private String s;
	
	public DCMessage(String s) {
		this.s = s;
	}
	
	public String toString() {
		return s;
	}
	
	public static DCMessage getMessage(byte[] bb) {
		return new DCMessage(new String(bb));
	}
	
	public byte[] toByteArray() {
		// TODO enforce to use same charset on all machines?
		return this.toString().getBytes();
	}

}
