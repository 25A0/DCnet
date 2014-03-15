package dc.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import cli.Debugger;
import dc.Connection;
import dc.DCMessage;
import dc.testing.DummyConnection;

public class DCServer {

	private static DCServer SERVER;

	
	public DCServer() {
		Debugger.println(1, "[DCServer] Server started");
		DCServer.SERVER = this;
		// new Thread(new ConCollector()).start();
		// new ConThread(new Connection(DummyConnection.C1));
	}

	public static DCServer getServer() {
		return SERVER;
	}

	public void connect(DummyConnection dc) throws IOException {
		ServerConnection c = new ServerConnection(dc);
		new ConThread(c);
	}
	
	private class ConCollector implements Runnable {
		int port = 2302;
		public void run() {
			while(true) {
				try {
					ServerSocket ss = new ServerSocket(port);
					Socket s = ss.accept();
					Connection c = new ServerConnection(s);
					Debugger.print(1, "[DCServer] Connection established on port " + port);
					new ConThread(c);
					port++;
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private class ConThread implements Runnable {
		private Connection c;
		
		public ConThread(Connection c) {
			this.c = c;
			(new Thread(this)).start();
		}
		
		public void run() {
			while(true) {
				DCMessage m;
				try {
					m = c.receive();
					Debugger.println(1, "[DCServer] Received message " + m.toString());
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
