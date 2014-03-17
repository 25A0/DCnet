package dc;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import cli.Debugger;
import java.util.Arrays;
import java.util.LinkedList;

public class ConnectionBundle {
	private KeyHandler kh;
	private final ArrayList<ConnectionHandler> chl;
	private final LinkedList<byte[]> outputBuffer;
	private int remaining, connections;

	private byte[] currentOutput;
		
	/**
	 *  This Semaphore is used to block until 
	 */
	private final Semaphore connectionSemaphore;
	
	/**
	 * This Semaphore is used to protect fields that are sensible in terms of concurrency
	 */
	private final Semaphore accessSemaphore;
	/**
	 * This Semaphore is used to keep track of the amount of output that
	 * can be read.
	 */
	private final Semaphore outputAvailable;
	
	public ConnectionBundle() {
		kh = new KeyHandler();
		chl = new ArrayList<ConnectionHandler>();
		
		/**
		 * This Semaphore is initalized in such a way that at least DCConfig.MIN_NUM_STATIONS 
		 * other stations have to be connected to this station before any communication happens.
		 */
		connectionSemaphore = new Semaphore(1 - DCConfig.MIN_NUM_STATIONS);
		
		/**
		 * Initially one party has access to the fields.
		 */
		accessSemaphore = new Semaphore(1);

		/**
		 * Initially there is no output available
		 */
		outputAvailable = new Semaphore(0);

		outputBuffer = new LinkedList<byte[]>();

		connections = 0;
		remaining = 0;
	}
	
	public void addConnection(Connection c, byte[] key) {
		Debugger.println(2, "[ConnectionBundle] New connection to " + c.toString() + " with key " + Arrays.toString(key));
		accessSemaphore.acquireUninterruptibly();
		chl.add(new ConnectionHandler(c));
		kh.addKey(c, key);
		connections++;
		connectionSemaphore.release();
		accessSemaphore.release();
	}
	
	public void removeConnection(Connection c) {
		accessSemaphore.acquireUninterruptibly();
		connectionSemaphore.acquireUninterruptibly();
		connections--;
		cc.remove(c);
		kh.removeKey(c);
		accessSemaphore.release();
	}
	
	public void broadcast(DCPackage m) {
		accessSemaphore.acquireUninterruptibly();
		/**
		 *  Make sure that there are enough connections
		 */
		connectionSemaphore.acquireUninterruptibly();
		connectionSemaphore.release();
		
		byte[] output = kh.getOutput(m.toByteArray());
		DCPackage o = new DCPackage(output);

		for(ConnectionHandler ch: chl) {
			ch.c.send(output);
		}
		reset(output);
		
		accessSemaphore.release();
	}

	public boolean canReceive() {
		boolean isAvailable = outputAvailable.tryAcquire();
		if(isAvailable) {
			outputAvailable.release();
			return true;
		} else {
			return false;
		}
	}
	
	public byte[] receive(int payload) {
		outputAvailable.acquireUninterruptibly();
		return outputBuffer.pop();
	}

	private void reset(byte[] output) {
		synchronized(currentOutput) {
			currentOutput = output;
			remaining = connections;
		}
	}

	private void addOutput(byte[] output) {
		synchronized(currentOutput) {
			if(output.length != currentOutput.length) {
				throw new InputMismatchException("[ConnectionBundle] The current output has length " + currentOutput.length + " but the provided output has length " + output.length + ".");
			} else {
				for(int i = 0; i < currentOutput.length; i++) {
					currentOutput[i] ^= output[i];
				}
				remaining--;
				if(remaining == 0) {
					outputBuffer.add(currentOutput);
					currentOutput = new byte[DCPackage.PAYLOAD_LENGTH];
					outputAvailable.release();
					reset(currentOutput);
				}
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
				byte[] output = new byte[DCPackage.PAYLOAD_LENGTH];
				for(int i = 0; i < output.length; i++) {
					
				}
			}
		}
	}
}
