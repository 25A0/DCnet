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
				
		private Action send, close, create, connect, read;
		
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
							s.getCB().addConnection(c1, key);
							l.getCB().addConnection(c2, key);
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
						if(args.hasAbbArg() && args.fetchAbbr().equals('s') || args.hasOptionArg() && args.fetchOption().equals("silence")) {
							s.getCB().broadcast();
						} else {
							try {
								String m = args.fetchString();
								Debugger.println(2, "Trying to send message " + m);
								s.send(m);
							} catch (IOException e) {
								Debugger.println(1, e.toString());
							}
						}
					} else {
						System.out.println("[StationInterface] Use \"station <alias> create\" to create a station first.");
					}
				}
			};

			read = new Action() {
				@Override
				public void execute(ArgSet args) {
					if(!s.getCB().canReceive()) {
						System.out.println("[StationInterface] This station does currently not have any output.");
					} else {
						int i = 1;
						while(s.getCB().canReceive()) {
							byte[] output = s.getCB().receive();
							StringBuilder sb = new StringBuilder();
							for(int k = 0; k < output.length; k++) {
								sb.append((char) output[k]);
							}
							System.out.println(i + ": " + sb.toString());
							i++;
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
			mapCommand("read", read);
		}
		
		protected void onEntering() {
			
		}

	}

}