package dc.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import cli.ArgSet;
import cli.CLC;
import cli.Debugger;
import net.Connection;
import net.NetworkConnection;
import dc.DCStation;
import dc.DcServer;
import dc.DcClient;
import dc.testing.DummyChannel;
import dc.testing.DummyConnection;


public class MultiStationInterface extends CLC {
	private HashMap<String, DcServer> servers;
	private HashMap<String, DcClient> clients;

	private Action listAction, noSuchStationAction, create, createServer, createClient, connectLocal;

	public MultiStationInterface() {
		servers = new HashMap<String, DcServer>();
		clients = new HashMap<String, DcClient>();

		listAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				System.out.println("Servers:");
				System.out.println(servers.keySet().toString());
				System.out.println("Clients:");
				System.out.println(clients.keySet().toString());
			}
		};

		noSuchStationAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				String s = args.pop();
				System.out.println("[ERROR] No command is associated with \"" + s + "\". If you want to create a station with that alias, use \"station make [server | client] "+s+"\"");
			}
		};
		
		connectLocal = new Action() {
			@Override
			public void execute(ArgSet args) {
				if(!args.hasArg()) {
					System.out.println("[ERROR] Please provide the alias of the server you want to connect to, followed by one or more stations.");
				} else {
					String serverAlias = args.pop();
					if(!servers.containsKey(serverAlias)) {
						System.out.println("[ERROR] There is no server called " + serverAlias);
					} else {
						DcServer server = servers.get(serverAlias);
						while(args.hasArg()) {
							String stationAlias  = args.pop();
							if(servers.containsKey(stationAlias)) {
								DCStation station = servers.get(stationAlias);
								connectLocal(server, station);
							} else if(clients.containsKey(stationAlias)) {
								DCStation station = clients.get(stationAlias);
								connectLocal(server, station);
							} else {
								System.out.println("[ERROR] There is no station called " + stationAlias);
								return;
							}
						}
					}
				}
			}
		};

		create = new Action() {
			@Override
			public void execute(ArgSet args) {
				System.out.println("[MultiStationInterface] Please provide a valid command of the form \"make (client|c|server|s) <alias>... \"");
			}
		};

		createServer = new Action() {
			@Override
			public void execute(ArgSet args) {
				boolean localFlag = args.hasAbbArg() && args.fetchAbbr() == 'l' || args.hasOptionArg() && args.fetchOption().equals("local");
				while(args.hasArg()) {
					String alias = args.pop();
					if(clients.containsKey(alias) || servers.containsKey(alias)) {
						System.out.println("[MultiStationInterface] A client or server with the name " + alias + " already exists.");
					} else {
						DcServer s;
						if(localFlag) {
							s = new DcServer(alias);
						} else if(!args.hasIntArg()) {
							System.out.println("[MultiStationInterface] Please provide a port number, or use \"-l\" or \"--local\" to start a local server.");
							return;
						} else {
							int port = args.fetchInteger();
							if(port < 0 || port >= 65536) {
								System.out.println("[MultiStationInterface] Please provide a port number in the range [0, 65536].");
								return;
							}
							s = new DcServer(alias, port);
						}
						servers.put(alias, s);
						mapCommand(alias, new CommandAction(new ServerInterface(s)));
					}
				}
			}
		};

		createClient = new Action() {
			@Override
			public void execute(ArgSet args) {
				while(args.hasArg()) {
					String alias = args.pop();
					if(clients.containsKey(alias) || servers.containsKey(alias)) {
						System.out.println("[MultiStationInterface] A client or server with the name " + alias + " already exists.");
					} else {
						DcClient c = new DcClient(alias);
						clients.put(alias, c);
						mapCommand(alias, new CommandAction(new ClientInterface(c)));
					}
				}
			}
		};

		setRootAction(listAction);
		setDefaultAction(noSuchStationAction);
		mapCommand("list", listAction);
		mapCommand("connect", connectLocal);
		mapAbbreviation('c', connectLocal);
		
		mapCommand("make", create);
		getContext("make").mapCommand("server", createServer);
		getContext("make").mapCommand("client", createClient);

	}

	private void connectLocal(DcServer server, DCStation station) {
		DummyChannel chA = new DummyChannel(), chB = new DummyChannel();
		Connection c1 = new DummyConnection(chA.getInputStream(), chB.getOutputStream(), station);
		Connection c2 = new DummyConnection(chB.getInputStream(), chA.getOutputStream(), server);
		station.setConnection(c1);
		server.getCB().addConnection(c2);
	}
	
	private void connect(String url, int port, DCStation station) {
		try {
			Socket s = new Socket(url, port);
			NetworkConnection nc = new NetworkConnection(s, station);
			station.setConnection(nc);
		} catch (UnknownHostException e) {
			System.out.println("Connection to host failed: The host with the address " + url + ":" + port + " was not found.");
		} catch (IOException e) {
			System.out.println("Connection to host failed");
			e.printStackTrace();
		}
	}

	private DCStation getStation(ArgSet args) {
		if(!args.hasArg()) return null; 

		String alias = args.pop();
		if(servers.containsKey(alias)) {
			return servers.get(alias);
		} else  if(clients.containsKey(alias)) {
			return clients.get(alias);
		} else {
			return null;
		}
	}

	private class ServerInterface extends StationInterface {
		private final DcServer server;

		public ServerInterface(DcServer server) {
			super(server);
			this.server = server;
		}
	}
	
	private class ClientInterface extends StationInterface {
		private final DcClient client;
		private Action send, read;

		private int roundCounter;
		
		public ClientInterface(DcClient c) {
			super(c);
			this.client = c;

			send = new Action() {
				@Override
				public void execute(ArgSet args) {
					boolean block = false;
					if(client != null) {
						// if(args.hasAbbArg() && args.fetchAbbr() == 'b' || args.hasOptionArg() && args.fetchOption().equals("block")) {
						// 	block = true;
						// }
						if(args.hasAbbArg() && args.fetchAbbr() == 'f' || args.hasOptionArg() && args.fetchOption().equals("file")) {
							String path = args.pop();
							File f = new File(path);
							try {
								InputStream is = new FileInputStream(f);
								long byteCount = 0;
								while(is.available() > 0) {
									client.send((byte) is.read());									
									byteCount++;
								}
								client.stop();
								is.close();
								if(block) client.block();
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
								client.send(m);
								if(block) client.block();
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
					if(!client.canReceive()) {
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
									while(client.canReceive()) {
										byte[] output = client.receive();
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
							while(client.canReceive()) {
								byte[] output = client.receive();
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

			mapCommand("send", send);
			mapCommand("read", read);
		}
		
	}
		
	private class StationInterface extends CLC {
		private final DCStation station;
				
		private Action close, connect, connectLocal, state;
		
		public StationInterface(DCStation s) {		
			this.station = s;
			
			connectLocal = new Action() {
				@Override
				public void execute(ArgSet args) {
					if(!args.hasArg()) {
						System.out.println("[ERROR] Please provide the alias of the server you want to connect to");
					} else {
						String serverAlias = args.pop();
						if(!servers.containsKey(serverAlias)) {
							System.out.println("[ERROR] There is no server called " + serverAlias);
						} else {
							DcServer server = servers.get(serverAlias);
							connectLocal(server, station);
						}
					}
				}
			};
			
			connect = new Action() {
				@Override
				public void execute(ArgSet args) {
					if(args.hasAbbArg() && args.fetchAbbr() == 'l' || args.hasOptionArg() && args.fetchOption().equals("local")) {
						connectLocal.execute(args);
						return;
					}
					if(!args.hasArg()) {
						System.out.println("[ERROR] Please provide the address of the server you want to connect to");
					} else {
						String url = args.pop();
						int port;
						String[] sa = url.split(":");
						if(sa.length == 2) {
							 url = sa[0];
							 port = Integer.parseInt(sa[1]);
						} else if(args.hasIntArg()) {
							port = args.fetchInteger();
						} else {
							port = 0;
						}
						connect(url, port, station);
					}
				}
			};

			state = new Action() {
				@Override
				public void execute(ArgSet args) {
					if(!station.isConnected()) {
						System.out.println("[ERROR] This station is not yet connected to a server.");
						return;
					} else if(!args.hasArg()) {
						System.out.println("Station " + station.getAlias() + " is " + (station.isActive()? "active":"inactive"));
					} else if(args.peek().equalsIgnoreCase("active")) {
						args.pop();
						station.setState(true);
					} else if(args.peek().equalsIgnoreCase("inactive")) {
						args.pop();
						station.setState(false);
					} else {
						System.out.println("[ERROR] The provided parameter did not match one of the options \'active\' or \'inactive\'.");
					}
				}
			};
			
			close = new Action() {
				@Override
				public void execute(ArgSet args) {
					try {
						station.close();
					} catch (IOException e) {
						System.out.println("[MultiStationInterface] An error occured while trying to close the station " + station.getAlias());
					}
				}
			};
			
			mapCommand("close", close);
			mapCommand("connect", connect);
			mapCommand("state", state);

			mapCommand("keys", new CommandAction(new KeyHandlerInterface(station)));
	
		}

		
		protected void onEntering() {
			
		}

	}

	private class KeyHandlerInterface extends CLC {
		private Action addAction;
		private DCStation station;
		public KeyHandlerInterface(DCStation s) {
			this.station = s;

			addAction = new Action() {
				@Override
				public void execute(ArgSet args) {
					String s2;
					while(args.hasArg()) {
						s2 = args.fetchString();
						if(s2 == null || station.getAlias().equals(s2)) {
							System.err.println("[ERROR] Please provide two different stations");
						} else {
							if(args.hasAbbArg() && args.fetchAbbr().equals('k') || args.hasOptionArg() && args.fetchOption().equals("key")) {
								if(!args.hasStringArg()) {
									System.err.println("[ERROR] No key has been provided although option \"key\" was set.");
								} else {
									byte[] key = args.fetchString().getBytes();
									station.getKeyHandler().addKey(s2, key);
									// s2.getKeyHandler().addKey(station.getAlias(), key);
								}
							} else {
								station.getKeyHandler().addKey(s2);
								// s2.getKeyHandler().addKey(station.getAlias());
							}

						}
					}
				}
			};

			mapCommand("add", addAction);
		}
	}

}