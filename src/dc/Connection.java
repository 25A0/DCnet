package dc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.sun.xml.internal.ws.message.ByteArrayAttachment;

import cli.Debugger;
import dc.DCMessage;
import dc.testing.DummyConnection;

public class Connection {
	private final InputStream is;
	private final OutputStream os;
	
	public Connection(InputStream is, OutputStream os) throws IOException {
		this.is = is;
		this.os = os;
	}

	public void send(DCMessage m) throws IOException {
		os.write(m.toByteArray());
	}
	
	public DCMessage receiveMessage() throws IOException {
		byte[] buffer = new byte[DCMessage.PAYLOAD_SIZE];
		for(int i = 0; i < DCMessage.PAYLOAD_SIZE; i++) {
			buffer[i] = (byte) is.read();
		}
		
		String s = Arrays.toString(buffer);
		Debugger.println(2, "[Connection] reading " + s);
		return DCMessage.getMessages(s)[0];
	}

	public DCOutput receiveOutput(int payload) {
		byte[] buffer = new byte[DCMessage.PAYLOAD_SIZE];
		for(int i = 0; i < DCMessage.PAYLOAD_SIZE; i++) {
			buffer[i] = (byte) is.read();
		}
		
		String s = Arrays.toString(buffer);
		Debugger.println(2, "[Connection] reading " + s);
		return DCMessage.getMessages(s)[0];	
	}

}
