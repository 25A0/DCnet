package dcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import cli.Debugger;
import dcp.DCpMessage;

public class DCpClient {
	private Connection c;
	
	public DCpClient(String host, int port) throws UnknownHostException, IOException {
		Debugger.println(1, "[DCpClient] Client started");
		Debugger.println(2, "[DCpClient] Attempting to connect to host " + host + ":" + port);
		c = new Connection(host, port);
	}
	
	public void send(String s) throws IOException {
		DCpMessage m = new DCpMessage(s);
		c.send(m);
		m = c.receive();
		Debugger.println(2, "[DCpClient] Received message " + m.toString());
	}
	
	public DataInputStream getInputStream() {
		return null;
	}
	
	public DataOutputStream getOutputStream() {
		return null;
	}
}
