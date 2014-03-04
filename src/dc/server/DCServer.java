package dc.server;

import java.io.IOException;
import java.net.UnknownHostException;

import cli.Debugger;
import dc.DCMessage;
import dc.testing.DummyConnection;

public class DCServer {

	public DCServer() {
		Debugger.println(1, "[DCServer] Server started");
		
//		new Thread(new ConCollector()).start();
		new ConThread(new Connection(DummyConnection.C1));
	}
	
	private class ConCollector implements Runnable {
		int port = 1822;
		public void run() {
			while(true) {
				try {
					Connection c = new Connection(port);
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
					c.send(m);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
