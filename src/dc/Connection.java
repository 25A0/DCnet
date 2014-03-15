package dc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

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

	protected void send(DCMessage m) throws IOException {
		os.write(m.toByteArray());
	}
	
	protected DCMessage receive() throws IOException {
//		byte[] buffer = new byte[16];
//		is.read(buffer);
		char c = (char) is.read();
		Debugger.println(2, "[Connection] reading " + c);
		return DCMessage.getMessage(String.valueOf(c).getBytes());
	}

}
