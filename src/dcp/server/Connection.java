package dcp.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import dcp.DCpMessage;
import dcp.testing.DummyConnection;

public class Connection {
	private ServerSocket ss;
	private Socket s;
	private final InputStream is;
	private final OutputStream os;
	
	/**
	 * Opens a server socket for an incoming connection and blocks until
	 * a connection has been established.
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public Connection(int port) throws UnknownHostException, IOException {
		ss = new ServerSocket(port);
		s = ss.accept();
		is = s.getInputStream();
		os = s.getOutputStream();
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
