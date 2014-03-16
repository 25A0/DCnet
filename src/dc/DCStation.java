package dc.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

import cli.Debugger;
import dc.Connection;
import dc.DCMessage;
import dc.testing.DummyConnection;

public class DCStation {
	private final Connection c;
	private final KeyHandler kh;
	private final LinkedList<String> pendingData;
	private Semaphore pendingDataSemaphore;

	private boolean isClosed = false;
	
	public DCStation(ClientConnection c) {
		Debugger.println(2, "[DCStation] Client started");
		this.c = c;
		kh = new KeyHandler();
		pendingData = new LinkedList<String>();
		pendingDataSemaphore = new Semaphore(0);
	}
	
	public DCStation(String host, int port) throws UnknownHostException, IOException {
		this(new ClientConnection(new Socket(host, port)));
	}
	
	public DCStation(DummyConnection dc) throws IOException {
		this(new ClientConnection(dc));
	}

	public void close() {
		isClosed = true;
	}
	
	public KeyHandler getKeyHandler() {
		return kh;
	}
	
	public void send(String s) throws IOException {
		pendingData.add(s);
		pendingDataSemaphore.release();
	}
	
	private class ProtocolCore implements Runnable {
		
		@Override
		public void run() {
			while(!isClosed) {
				try{
					sleep(1000);
				} catch(InterruptException e) {
					System.err.println("[DCStation] Unable to complete sleep. Proceeding...");
				}

				
			}
		}
	}

}
