package cli;

import dcp.server.DCpServer;

public class ServerInterface extends CLC {
	private DCpServer s;
	
	public ServerInterface() {
		s = new DCpServer();
	}

}
