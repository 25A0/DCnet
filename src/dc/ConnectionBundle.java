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
	private int remaining, connections;
	private byte[] currentInput;

	private boolean isClosed = false;
	private final Semaphore inputAvailable;
	private final LinkedList<byte[]> inputBuffer;
	
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

		inputBuffer = new LinkedList<byte[]>();
		inputAvailable = new Semaphore(0);
		
		currentInput = new byte[DCPackage.PAYLOAD_SIZE];

		connections = 0;
		remaining = 0;
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
	
	public void broadcast(byte[] message) {
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
	
	public byte[] receive() {
		inputAvailable.acquireUninterruptibly();
		return inputBuffer.pop();
	}

	private void reset() {
		currentInput = new byte[DCPackage.PAYLOAD_SIZE];

		// We need to read connections outputs before this round is complete
		// i.e.: we have to receive a message from each station that we're connected
		// to
		remaining = connections;
	}

	/**
	 * Adds bytes to the input of the current round.
	 * @param input The input to be added
	 */
	private void addInput(byte[] input) {
		if(input.length != currentInput.length) {
			throw new InputMismatchException("[ConnectionBundle] The current input has length " + currentInput.length + " but the provided input has length " + input.length + ".");
		} else {
			for(int i = 0; i < currentInput.length; i++) {
				currentInput[i] ^= input[i];
			}
			remaining--;
			Debugger.println(2, "[ConnectionBundle] Remaining messages: " + remaining);
			if(remaining == 0) {
				Debugger.println(2, "[ConnectionBundle] Composed input: " + Arrays.toString(currentInput));
				inputBuffer.add(currentInput);
				inputAvailable.release();
				
				reset();
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
				byte[] input = new byte[DCPackage.PAYLOAD_SIZE];
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
