package dcp.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import dcp.DCpMessage;
import dcp.testing.DummyConnection;

public class Connection {
	private Socket s;
	private final InputStream is;
	private final OutputStream os;
	
	/**
	 * Attempts to initiate a connection to the provided host server.
	 * @param host
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public Connection(String host, int port) throws UnknownHostException, IOException {
		s = new Socket(host, port);
		is = s.getInputStream();
		os = s.getOutputStream();
	}
	
	/**
	 * Debugging constructor
	 * @param dc
	 */
	public Connection(DummyConnection dc) {
		is = DummyConnection.SERVER.getInputStream();
		os = DummyConnection.C1.getOutputStream();
	}
	
	public void send(DCpMessage m) throws IOException {
		os.write(m.toByteArray());
	}
	
	public DCpMessage receive() throws IOException {
		byte[] buffer = new byte[16];
		is.read(buffer);
		return DCpMessage.getMessage(buffer);
	}

}
