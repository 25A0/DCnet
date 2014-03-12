package cli;

import dc.server.DCServer;

public class ServerInterface extends CLC {
	private DCServer s;
	
	public ServerInterface() {
		
	}

	@Override
	protected void onEntering() {
		if(s == null) s = new DCServer();
	}

}
