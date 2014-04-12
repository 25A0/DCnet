package dc.cli;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cli.ArgSet;
import cli.CLC;
import cli.Debugger;
import dc.Connection;
import dc.DCStation;
import dc.KeyHandler;
import dc.testing.DummyConnection;

import java.util.Arrays;


public class MultiStationInterface extends CLC {
<<<<<<< Updated upstream
	private ArrayList<String> stations;
	private Action listAction, noSuchStationAction, connect, create;

	public MultiStationInterface() {
		stations = new ArrayList<String>();
=======
	private Map<String, StationInterface> clientMap, serverMap;
	private Action listAction, forwardAction, connect, create;

	public MultiStationInterface() {
		clientMap = new HashMap<String, StationInterface>();
		serverMap = new HashMap<String, StationInterface>();
>>>>>>> Stashed changes

		listAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				System.out.println(stations);
			}
		};

		noSuchStationAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				String s = args.pop();
				System.out.println("[MultiStationInterface] No command is associated with \"" + s + "\". If you want to create a station with that alias, use \"station make "+s+"\"");
			}
		};

		connect = new Action() {
			@Override
			public void execute(ArgSet args) {
				DCStation s1, s2;
				while(args.hasArg()) {
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
				if(!args.hasArg()) {
					System.out.println("[MultiStationInterface] Please provide a valid command of the form \"(client|c|server|s) <alias>... \"");
				} else {
					String type = args.pop();
					if(type.equals("client") || type.equals("c")) {
						
					} else if(type.equals("server") || type.equals("s")) {
						
					}
				}
				while(args.hasArg()) {
<<<<<<< Updated upstream
					String alias = args.pop();
					if(stations.contains(alias)) {
						System.out.println("[MultiStationInterface] A station with alias " + alias + " already exists.");
					} else {
						stations.add(alias);
						setContext(alias, new StationInterface());
					}
=======

					createStation(args.pop());
>>>>>>> Stashed changes
				}
			}
		};

		setRootAction(listAction);
		setDefaultAction(noSuchStationAction);
		mapAbbreviation('l', listAction);
		mapOption("list", listAction);
		mapOption("connect", connect);
		mapAbbreviation('c', connect);
		mapCommand("make", create);
		mapAbbreviation('m', create);
	}

	private DCStation getStation(ArgSet args) {
		if(!args.hasArg()) return null; 

		String alias = args.pop();
		if(!stations.contains(alias)) {
			return null;
		} else {
			return ((StationInterface) getContext(alias)).s;
		}
	}
		
	private class StationInterface extends CLC {
		private DCStation s;
		private int roundCounter;
				
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
					boolean block = false;
					if(s != null) {
						if(args.hasAbbArg() && args.fetchAbbr() == 'b' || args.hasOptionArg() && args.fetchOption().equals("block")) {
							block = true;
						}
						if(args.hasAbbArg() && args.fetchAbbr() == 'f' || args.hasOptionArg() && args.fetchOption().equals("file")) {
							String path = args.pop();
							File f = new File(path);
							try {
								InputStream is= new FileInputStream(f);
								long byteCount = 0;
								while( is.available() > 0) {
									s.send((byte) is.read());									
									byteCount++;
								}
								is.close();
								if(block) s.getCB().block();
								System.out.println("[MultiStationInterface] " + byteCount + " byte(s) have been read from file " + path);
							} catch (FileNotFoundException e) {
								System.out.println("[MultiStationInterface] The file at " + path + " was not found. Please provide a valid path to an existing file.");
							} catch (IOException e) {
								System.out.println("[MultiStationInterface] An error occured while reading from file " + path);
							}							
						} else if(!args.hasStringArg()) {
							System.out.println("[MultiStationInterface] Please provide a message, enclosed by \" characters");
						} else {
							String m = args.fetchString();
							Debugger.println(2, "Trying to send message " + m);
							try {	
								s.send(m);
								if(block) s.getCB().block();
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
						if(args.hasAbbArg() && args.fetchAbbr() == 'f' || args.hasOptionArg() && args.fetchOption().equals("file")) {
							if(!args.hasArg()) {
								System.out.println("[MultiStationInterface] Please provide a relative path to a file that you want to write to");
							} else {
								String path = args.pop();
								File f = new File(path);
								try {
									OutputStream os= new FileOutputStream(f, true);
									long byteCount = 0;
									while(s.getCB().canReceive()) {
										byte[] output = s.getCB().receive();
										byteCount += output.length;
										os.write(output);											
										roundCounter++;
									}
									os.close();
									System.out.println("[MultiStationInterface] " + byteCount + " byte(s) have been appended to file " + path);
								} catch (FileNotFoundException e) {
									System.out.println("[MultiStationInterface] The file at " + path + " was not found. Please provide a valid path to an existing file.");
								} catch (IOException e) {
									System.out.println("[MultiStationInterface] An error occured while reading from file " + path);
								}					
							}
						} else {
							while(s.getCB().canReceive()) {
								byte[] output = s.getCB().receive();
								StringBuilder sb = new StringBuilder();
								for(int k = 0; k < output.length; k++) {
									sb.append((char) output[k]);
								}
								System.out.println(roundCounter + ": " + sb.toString());
								roundCounter++;
							}
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