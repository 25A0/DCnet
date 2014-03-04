package cli;

import java.io.IOException;
import java.net.UnknownHostException;

import dcp.client.DCpClient;
import dcp.testing.DummyConnection;

public class ClientInterface extends CLC {
	private DCpClient c;
	private Action connect, send;
	
	public ClientInterface() {		
		//Define actions
		connect = new Action() {
			@Override
			public void execute(String... args) {
				String host = "192.168.2.74";
				int port = Integer.valueOf(args[0]);
				try {
					c = new DCpClient(host, port);
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
						c.send(s);
					} catch (IOException e) {
						Debugger.println(1, e.toString());
					}
				}
			}
		};
		
		
		teachCommands();
	}
	
	private void teachCommands() {
		mapCommand("connect", connect);
		mapCommand("send", send);
	}

}
