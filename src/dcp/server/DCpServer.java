package dcp.server;

import java.io.IOException;
import java.net.UnknownHostException;

import cli.Debugger;
import dcp.DCpMessage;

public class DCpServer {

	public DCpServer() {
		Debugger.println(1, "[DCpServer] Server started");
		
		new Thread(new ConCollector()).start();
	}
	
	private class ConCollector implements Runnable {
		int port = 1822;
		public void run() {
			while(true) {
				try {
					Connection c = new Connection(port);
					Debugger.print(1, "[DCpServer] Connection established on port " + port);
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
				DCpMessage m;
				try {
					m = c.receive();
					Debugger.println(2, "[DCpServer] Received message " + m.toString());
					c.send(m);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
