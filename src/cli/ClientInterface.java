package cli;

import java.io.IOException;
import java.net.UnknownHostException;

import dc.client.DCClient;
import dc.testing.DummyConnection;

public class ClientInterface extends CLC {
	private DCClient c;
	private Action connect, send, dummy;
	
	public ClientInterface() {		
		//Define actions
		connect = new Action() {
			@Override
			public void execute(String... args) {
				String host = "192.168.2.74";
				int port = Integer.valueOf(args[0]);
				try {
					c = new DCClient(host, port);
				} catch (UnknownHostException e) {
					Debugger.println(1, e.toString());
//					e.printStackTrace();
				} catch (IOException e) {
					Debugger.println(1, e.toString());
//					e.printStackTrace();
				}
			}
		};
		
		send = new Action() {
			@Override
			public void execute(String... args) {
				if(c != null) {
					try {
						String s = args.length > 0? args[0]: "";
						Debugger.println(2, "Trying to send message " + s);
						c.send(s);
					} catch (IOException e) {
						Debugger.println(1, e.toString());
					}
				}
			}
		};
		
		dummy = new Action() {
			@Override
			public void execute(String... args) {
				c = new DCClient(DummyConnection.C1);
			};
		};
		
		
		teachCommands();
	}
	
	private void teachCommands() {
		mapCommand("connect", connect);
		mapCommand("send", send);
		mapCommand("dummy", dummy);
	}

}
