package dcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import dcp.testing.DummyConnection;

public class DCpClient {
	private Connection c;
	
	public DCpClient(String host, int port) throws UnknownHostException, IOException {
		c = new Connection(host, port);
		
	}
	
	public DCpClient(DummyConnection dc) {
		c = new Connection(dc);
	}
	
	public DataInputStream getInputStream() {
		
	}
	
	public DataOutputStream getOutputStream() {
		
	}
}
