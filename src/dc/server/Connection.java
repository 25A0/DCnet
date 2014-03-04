package dc.server;

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
	
	public Connection(DummyConnection dc) {
		os = dc.getOutputStream();
		is = DummyConnection.SERVER.getInputStream();
	}
	
	public void send(DCMessage m) throws IOException {
		os.write(m.toByteArray());
	}
	
	public DCMessage receive() throws IOException {
		byte[] buffer = new byte[16];
		is.read(buffer);
		Debugger.println(2, "[Connection] reading " + Arrays.toString(buffer));
		return DCMessage.getMessage(buffer);
	}

}
