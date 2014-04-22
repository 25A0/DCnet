package dc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import cli.Debugger;
import dc.DCPackage;

import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.LinkedList;

public class ConnectionBundle {
	private final ArrayList<ConnectionHandler> chl;
	private int connections;

	private boolean isClosed = false;
	private final Semaphore inputAvailable;
	private final LinkedList<DCPackage> inputBuffer;
	private final DCPackage[] pendingPackages;
	private final int[] remaining;
	
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
		
		remaining = new int[DCConfig.NUM_ROUNDS_AT_A_TIME];
		pendingPackages = new DCPackage[DCConfig.NUM_ROUNDS_AT_A_TIME];
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

	public void close() {
		// TODO: close individual connections
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
	 * Adds bytes to the input of the current round.
	 * @param input The input to be added
	 */
	private void addInput(DCPackage input) {
		if(input.getPayload().length != DCPackage.PAYLOAD_SIZE) {
			throw new InputMismatchException("The provided input has length " + input.getPayload().length + " but should be " + DCPackage.PAYLOAD_SIZE);
		} else {
			int round = input.getNumber();
			if(pendingPackages[round] == null) {
				pendingPackages[round] = input;
			} else {
				pendingPackages[round].combine(input);
			}
			remaining[round]--;
			Debugger.println(2, "[ConnectionBundle] Remaining messages: " + remaining);
			if(remaining[round] == 0) {
				Debugger.println(2, "[ConnectionBundle] Composed input: " + pendingPackages[round].toString());
				inputBuffer.add(pendingPackages[round]);
				inputAvailable.release();
				
				pendingPackages[round] = null;
				remaining[round] = connections;
			}
		}
	}

	private class ConnectionHandler implements Runnable {
		private boolean isClosed = false;
		public final Connection c;

		public ConnectionHandler(Connection c) {
			this.c = c;
		}

		public void close() {
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
