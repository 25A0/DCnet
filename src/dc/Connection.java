package dc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import cli.Debugger;
import dc.DCPackage;
import dc.testing.DummyConnection;

public class Connection {
	private final InputStream is;
	private final OutputStream os;
	
	public Connection(InputStream is, OutputStream os) {
		this.is = is;
		this.os = os;
	}

	public void send(byte[] bb) throws IOException {
		os.write(bb);
	}
	
	public byte[] receiveMessage() throws IOException {
		byte[] buffer = new byte[DCPackage.PAYLOAD_SIZE];
		for(int i = 0; i < DCPackage.PAYLOAD_SIZE; i++) {
			buffer[i] = (byte) is.read();
		}
		return buffer;
		// String s = Arrays.toString(buffer);
		// Debugger.println(2, "[Connection] reading " + s);
		// return DCPackage.getMessages(s)[0];
	}
}
