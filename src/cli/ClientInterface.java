package cli;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import dc.client.DCClient;
import dc.testing.DummyConnection;

public class ClientInterface extends CLC {
	private DCClient c;
	private Action create, send, dummy;
	
	public ClientInterface() {		
		

		//Define actions
		create = new Action() {
			@Override
			public void execute(ArgSet args) {
				String host = "192.168.2.74";
				try {
					int port = args.fetchInteger();
					if(port < 0 || port >= 65536) 
						throw new IllegalArgumentException("The provided port has to be in bounds [0..65536]");
					DCClient c = new DCClient(host, port);
				} catch (UnknownHostException e) {
					Debugger.println(1, e.toString());
//					e.printStackTrace();
				} catch (IOException e) {
					Debugger.println(1, e.toString());
//					e.printStackTrace();
				} catch (NumberFormatException e) {

				}
			}
		};
		
		send = new Action() {
			@Override
			public void execute(ArgSet args) {
				if(c != null) {
					try {
						String s = args.fetchString();
						Debugger.println(2, "Trying to send message " + s);
						c.send(s);
					} catch (IOException e) {
						Debugger.println(1, e.toString());
					}
				}
			}
		};
		
//		dummy = new Action() {
//			@Override
//			public void execute(ArgSet args) {
//				if(args.length > 0) {
//					try {
//						int i = Integer.valueOf(args[0]);
//						switch(i) {
//							case 1: 
//								c = new DCClient(DummyConnection.C1);
//								break;
//							case 2:
//								c = new DCClient(DummyConnection.C2);
//								break;
//							default:
//								c = new DCClient(DummyConnection.C3);
//								break;
//						}
//					} catch (NumberFormatException e) {
//						System.err.println("Enter a number in the range [1..3]");
//					}
//				}
//			};
//		};	
		
		teachCommands();
	}
	
	private void teachCommands() {
		mapCommand("create", create);
		mapCommand("send", send);
//		mapCommand("dummy", dummy);
	}
	
	protected void onEntering() {
		
	}

}
