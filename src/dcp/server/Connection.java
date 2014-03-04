package dcp.server;

import java.net.ServerSocket;

public class Connection {
	private ServerSocket s;
	
	public Connection() {
		s = new ServerSocket(1822)
	}

}
