package dcp;

import java.math.BigInteger;
import java.util.Arrays;

public class DCpMessage {
	private String s;
	
	public DCpMessage(String s) {
		this.s = s;
	}
	
	public String toString() {
		return s;
	}
	
	public static DCpMessage getMessage(byte[] bb) {
		return new DCpMessage(new String(bb));
	}
	
	public byte[] toByteArray() {
		// TODO enforce to use same charset on all machines?
		return this.toString().getBytes();
	}

}
