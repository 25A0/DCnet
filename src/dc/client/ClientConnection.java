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
import dc.Connection;

public class ClientConnection extends Connection {
	private Socket s;
	
	/**
	 * Attempts to initiate a connection to the provided host server.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public ClientConnection(Socket s) throws IOException {
		super(s.getInputStream(), s.getOutputStream());
		this.s = s;
	}
	
	public ClientConnection(DummyConnection dc) throws IOException {
		super(dc.chA.getInputStream(), dc.chB.getOutputStream());
	}

}
