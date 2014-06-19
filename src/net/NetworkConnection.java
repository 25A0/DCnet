package net;

import java.io.IOException;
import java.net.Socket;

public class NetworkConnection extends Connection {
	private Socket s;
	
	public NetworkConnection(Socket s) throws IOException {
		this(s, null);
	}

	public NetworkConnection(Socket s, PackageListener listener) throws IOException {
		super(s.getInputStream(), s.getOutputStream(), listener);
		this.s = s;
		s.setTcpNoDelay(true);
		s.setPerformancePreferences(0, 2, 1);
	}

	@Override
	public void close() throws IOException {
		super.close();
		s.close();
	}
}