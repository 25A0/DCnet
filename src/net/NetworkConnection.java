package net;

import java.io.IOException;
import java.net.Socket;

public class NetworkConnection extends Connection {
	private Socket s;

	public NetworkConnection(Socket s) throws IOException {
		super(s.getInputStream(), s.getOutputStream());
		this.s = s;
	}

	@Override
	public void close() throws IOException {
		s.close();
	}
}