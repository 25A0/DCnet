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
import dc.Connection;

public class ServerConnection extends Connection {
	private Socket s;
	
	/**
	 * Opens a server socket for an incoming connection and blocks until
	 * a connection has been established.
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public ServerConnection(Socket s) throws IOException {
		super(s.getInputStream(), s.getOutputStream());
		this.s = s;
	}
	
	public ServerConnection(DummyConnection dc) throws IOException {
		super(dc.chB.getInputStream(), dc.chA.getOutputStream());
	}
}
