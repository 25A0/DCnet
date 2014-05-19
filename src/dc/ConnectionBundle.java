package dc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import cli.Debugger;
import dc.DCPackage;
import net.Connection;

import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.LinkedList;

public class ConnectionBundle {
	private final ArrayList<ConnectionHandler> chl;
	private int connections;
	
	private DCPackage pendingPackage;
	private int remaining;	

	private boolean isClosed = false;
	private final Semaphore inputAvailable;

	private final LinkedList<DCPackage> inputBuffer;
		

	/**
	 * This Semaphore is used to protect fields that are sensible in terms of concurrency
	 */
	private final Semaphore accessSemaphore;

	
	public ConnectionBundle() {
		chl = new ArrayList<ConnectionHandler>();
		
		/**
		 * Initially one party has access to the fields.
		 */
		accessSemaphore = new Semaphore(1);

		inputBuffer = new LinkedList<DCPackage>();
		inputAvailable = new Semaphore(0);
		
		connections = 0;
	}
	
	public void addConnection(Connection c) {
		Debugger.println(2, "[ConnectionBundle] New connection to " + c.toString());
		
		accessSemaphore.acquireUninterruptibly();
			ConnectionHandler ch = new ConnectionHandler(c);
			chl.add(ch);
			connections++;
			remaining++;
			(new Thread(ch)).start();
		accessSemaphore.release();
	}
	
	public void removeConnection(Connection c) {
		accessSemaphore.acquireUninterruptibly();
			connections--;
			remaining--;
			chl.remove(c);
		accessSemaphore.release();
	}
	
	public void broadcast(DCPackage message) {
		for(ConnectionHandler ch: chl) {
			try{
				ch.c.send(message);
				
			} catch (IOException e) {
				Debugger.println(1, e.getMessage());
			}
		}
	}

	public void close() throws IOException {
		for(ConnectionHandler ch: chl) {
			ch.close();
		}
		isClosed = true;
	}

	public boolean canReceive() {
		boolean isAvailable = inputAvailable.tryAcquire();
		if(isAvailable) {
			inputAvailable.release();
			return true;
		} else {
			return false;
		}
	}
	
	public DCPackage receive() {
		inputAvailable.acquireUninterruptibly();
		return inputBuffer.pop();
	}

	/**
	 * Adds a package to the input of the current round.
	 * @param input The input to be added
	 */
	private void addInput(DCPackage input) {
		if(input.getPayload().length != DCPackage.PAYLOAD_SIZE) {
			throw new InputMismatchException("The provided input has length " + input.getPayload().length + " but should be " + DCPackage.PAYLOAD_SIZE);
		} else {
			int round = input.getNumber();
			if(pendingPackage == null) {
				pendingPackage = input;
			} else {
				pendingPackage.combine(input);
			}
			remaining--;
			Debugger.println(2, "[ConnectionBundle] Remaining messages: " + remaining);
			if(remaining == 0) {
				Debugger.println(2, "[ConnectionBundle] Composed input: " + pendingPackage.toString());
				inputBuffer.add(pendingPackage);
				inputAvailable.release();
				
				pendingPackage = null;
				remaining = connections;
			}
		}
	}

	/**
	 * Checks whether a given number is in between two included bounds, in modulo {@code n}
	 * @param  a The lower bound
	 * @param  b The upper bound
	 * @param  x The number to be tested
	 * @param  n modulo
	 * @return   if {@code x} is in between {@code [a, b]} in modulo {@code n}
	 */
	private boolean inBetweenMod(int a, int b, int x, int n) {
		if(b < a) b += n;
		if(x < a) x += n;
		return a <= x && x <= b;
	}


	private class ConnectionHandler implements Runnable {
		private boolean isClosed = false;
		public final Connection c;

		public ConnectionHandler(Connection c) {
			this.c = c;
		}

		public void close() throws IOException {
			c.close();
			isClosed = true;
		}

		@Override
		public void run() {
			while(!isClosed) {
				DCPackage input;
				try {
					input = c.receiveMessage();
					accessSemaphore.acquireUninterruptibly();
					addInput(input);
					accessSemaphore.release();
				} catch (IOException e) {
					Debugger.println(1, e.getMessage());
				}
			}
		}
	}

}
