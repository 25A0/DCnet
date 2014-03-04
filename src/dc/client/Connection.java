package dc.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import cli.Debugger;
import dc.DCMessage;
import dc.testing.DummyConnection;

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
	
	public Connection(DummyConnection dc) {
		is = dc.getInputStream();
		os = DummyConnection.SERVER.getOutputStream();
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
