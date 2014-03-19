package dc.cli;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cli.ArgSet;
import cli.CLC;
import cli.Debugger;
import dc.Connection;
import dc.DCStation;
import dc.KeyHandler;
import dc.testing.DummyConnection;


public class MultiStationInterface extends CLC {
	private Map<String, StationInterface> ciMap;
	private Action listAction, forwardAction, connect, create;

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
					createStation(s);
				}
				ciMap.get(s).handle(args);
			}
		};

		connect = new Action() {
			@Override
			public void execute(ArgSet args) {
				DCStation s1, s2;
				while(args.hasStringArg()) {
					s1 = getStation(args);
					s2 = getStation(args);
					if(s1 == null || s2 == null || s1 == s2) {
						System.err.println("[MultiStationInterface] Please provide two different stations");
					} else {
						byte[] key;
						if(args.hasAbbArg() && args.fetchAbbr().equals('k') || args.hasOptionArg() && args.fetchOption().equals("key")) {
							if(!args.hasStringArg()) {
								System.err.println("[StationInterface] No key has been provided although option \"key\" was set.");
								return;
							} else {
								key = args.fetchString().getBytes();
							}
						} else {
							key = new byte[KeyHandler.KEY_SIZE];
							for(int i = 0; i < KeyHandler.KEY_SIZE; i++) {
								key[i] = (byte) (Math.random()*Byte.MAX_VALUE);
							}
						}
						DummyConnection dc = new DummyConnection();
						Connection c1 = new Connection(dc.chA.getInputStream(), dc.chB.getOutputStream());
						Connection c2 = new Connection(dc.chB.getInputStream(), dc.chA.getOutputStream());
						s1.getCB().addConnection(c1, key);
						s2.getCB().addConnection(c2, key);
					}
				}
			}
		};
		
		create = new Action() {
			@Override
			public void execute(ArgSet args) {
				while(args.hasStringArg()) {
					createStation(args.fetchString());
				}
			}
		};

		setRootAction(listAction);
		setDefaultAction(forwardAction);
		mapAbbreviation('l', listAction);
		mapOption("list", listAction);
		mapOption("connect", connect);
		mapAbbreviation('c', connect);
		mapCommand("make", create);
		mapAbbreviation('m', create);
	}

	private DCStation getStation(ArgSet args) {
		if(!args.hasStringArg()) return null; 

		String alias = args.fetchString();
		if(!ciMap.containsKey(alias)) {
			return null;
		} else {
			return ciMap.get(alias).s;
		}
	}
	
	private void createStation(String s) {
		ciMap.put(s, new StationInterface());
	}
	
	private class StationInterface extends CLC {
		private DCStation s;
				
		private Action send, close, create, read;
		
		public StationInterface() {		
			s = new DCStation();
			
			
			
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
			mapCommand("send", send);
			mapCommand("read", read);
		}
		
		protected void onEntering() {
			
		}

	}

}