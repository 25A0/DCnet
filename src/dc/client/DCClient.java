package dc.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import cli.Debugger;
import dc.DCMessage;
import dc.testing.DummyConnection;

public class DCClient {
	private Connection c;
	
	public DCClient(String host, int port) throws UnknownHostException, IOException {
		Debugger.println(1, "[DCClient] Client started");
		Debugger.println(2, "[DCClient] Attempting to connect to host " + host + ":" + port);
		c = new Connection(host, port);
	}
	
	public DCClient(DummyConnection dc) {
		Debugger.println(2, "[DCClient] Client started");
		Debugger.println(2, "[DCClient] Using dummy connection");
		c = new Connection(dc);
	}
	
	public void send(String s) throws IOException {
		DCMessage m = new DCMessage(s);
		c.send(m);
		m = c.receive();
		Debugger.println(2, "[DCClient] Received message " + m.toString());
	}
	
	public DataInputStream getInputStream() {
		return null;
	}
	
	public DataOutputStream getOutputStream() {
		return null;
	}
}
