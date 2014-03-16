package dc.cli;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cli.ArgSet;
import cli.CLC;
import cli.Debugger;
import dc.Connection;
import dc.DCStation;
import dc.testing.DummyConnection;


public class MultiStationInterface extends CLC {
	private Map<String, StationInterface> ciMap;
	private Action listAction, forwardAction;

	public MultiStationInterface() {
		ciMap = new HashMap<String, StationInterface>();

		listAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				System.out.println(ciMap.keySet());
			}
		};

		forwardAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				String s = args.fetchString();
				if(!ciMap.containsKey(s)) {
						ciMap.put(s, new StationInterface());
					}
				ciMap.get(s).handle(args);
			}
		};

		setRootAction(listAction);
		setDefaultAction(forwardAction);
		mapAbbreviation('l', listAction);
		mapOption("list", listAction);
	}
	
	private class StationInterface extends CLC {
		private DCStation s;
				
		private Action send, close, create, connect;
		
		public StationInterface() {		
			s = new DCStation();
			
			connect = new Action() {
				@Override
				public void execute(ArgSet args) {
					while(args.hasStringArg()) {
						String alias = args.fetchString();
						if(!ciMap.containsKey(alias)) {
							System.err.println("[StationInterface] The alias " + alias + " is unknown.");
							return;
						} else {
							if(!args.hasStringArg()) {
								System.err.println("[StationInterface] No key has been provided for alias " + alias + ".");
								return;
							}
							byte[] key = args.fetchString().getBytes();
							DCStation l = ciMap.get(alias).s;
							DummyConnection dc = new DummyConnection();
							Connection c1 = new Connection(dc.chA.getInputStream(), dc.chB.getOutputStream());
							Connection c2 = new Connection(dc.chB.getInputStream(), dc.chA.getOutputStream());
							s.getConnectionBundle().addConnection(c1, key);
							l.getConnectionBundle().addConnection(c2, key);
						}
					}
				}
			};
			
			close = new Action() {
				@Override
				public void execute(ArgSet args) {
					if(s != null) {
						s.close();
						s = null;						
					}
				}
			};
			
			create = new Action() {
				@Override
				public void execute(ArgSet args) {
					if(s != null) {
						System.err.println("[StationInterface] There is already a station associated with this alias. Use the \"close\" command to close the existing station.");
					} else {
						s = new DCStation();
					}
				}
			};

			send = new Action() {
				@Override
				public void execute(ArgSet args) {
					if(s != null) {
						try {
							String m = args.fetchString();
							Debugger.println(2, "Trying to send message " + m);
							s.send(m);
						} catch (IOException e) {
							Debugger.println(1, e.toString());
						}
					}
				}
			};	
			
			teachCommands();
		}
		
		private void teachCommands() {
			mapCommand("close", close);
			mapCommand("create", create);
			mapCommand("connect", connect);
			mapCommand("send", send);
		}
		
		protected void onEntering() {
			
		}

	}

}