package dc;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import dc.MessageBuffer;

import cli.Debugger;

public abstract class DCStation {
	protected Connection c;
	protected final String alias;
	protected final KeyHandler kh;
	
	protected boolean isClosed = false;

	protected final Semaphore connectionSemaphore;
	
	public DCStation(String alias) {
		this.alias = alias;
		kh = new KeyHandler(alias);
		connectionSemaphore = new Semaphore(0);
		(new Thread(new InputReader())).start();
		Debugger.println(2, "[DCStation] Station " + alias + " started");
	}

	public String getAlias() {
		return alias;
	}

	protected void broadcast(byte[] output) {	
		try{
			c.send(output);
			
		} catch (IOException e) {
			Debugger.println(1, e.getMessage());
		}	
	}
	
	public void close() {
		isClosed = true;
	}
	
	public void setConnection(Connection c) {
		if(this.c != null) {
			connectionSemaphore.acquireUninterruptibly();
			connectionSemaphore.acquireUninterruptibly();
		}
		this.c = c;
		connectionSemaphore.release();
		connectionSemaphore.release();

	}

	public KeyHandler getKeyHandler() {
		return kh;
	}

	protected abstract void addInput(byte[] input);

	/**
	 * This Runnable will constantly read incoming messages
	 * and forward them to {@code addInput}.
	 */
	private class InputReader implements Runnable {

		@Override
		public void run() {
			while(!isClosed) {
				connectionSemaphore.acquireUninterruptibly();
				byte[] input = new byte[DCPackage.PAYLOAD_SIZE];
				try {
					input = c.receiveMessage();
					addInput(input);
				} catch (IOException e) {
					Debugger.println(1, e.getMessage());
				}
				connectionSemaphore.release();
			}
		}

		
	}
}
