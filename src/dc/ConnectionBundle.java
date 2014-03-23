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
	private KeyHandler kh;
	private final ArrayList<ConnectionHandler> chl;
	private final LinkedList<byte[]> inputBuffer;
	private int remaining, connections;

	private byte[] currentInput;
	private final LinkedList<byte[]> messageBuffer;

	private boolean isClosed = false;
	
	private final SimpleOutputStream os;
				
	/**
	 *  This Semaphore is used to block certain functions
	 *  until enough connections have been estabilshed
	 */
	private final Semaphore connectionSemaphore;
	
	/**
	 * This Semaphore is used to protect fields that are sensible in terms of concurrency
	 */
	private final Semaphore accessSemaphore;
	/**
	 * This Semaphore is used to keep track of the amount of input that
	 * can be read.
	 */
	private final Semaphore inputAvailable;
	
	/**
	 * This semaphore keeps track of the number of uncompleted rounds.
	 */
	private final Semaphore roundCompletionSemaphore;
	
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
		inputAvailable = new Semaphore(0);
		
		/**
		 * The number of initial releases determine how many rounds can run in parallel.
		 * For now, more than one round at a time will break the protocol.
		 */
		roundCompletionSemaphore = new Semaphore(0);

		inputBuffer = new LinkedList<byte[]>();
		messageBuffer = new LinkedList<byte[]>();

		currentInput = new byte[DCPackage.PAYLOAD_SIZE];

		connections = 0;
		remaining = 1;
		
		os = new SimpleOutputStream(DCPackage.PAYLOAD_SIZE);
		
		// Start up the round initializer
		(new Thread(new RoundInitializer())).start();
	}
	
	public void addConnection(Connection c, byte[] key) {
		Debugger.println(2, "[ConnectionBundle] New connection to " + c.toString() + " with key " + Arrays.toString(key));
		accessSemaphore.acquireUninterruptibly();
			ConnectionHandler ch = new ConnectionHandler(c);
			chl.add(ch);
			kh.addKey(c, key);
			connections++;
			remaining++;
			connectionSemaphore.release();
			(new Thread(ch)).start();
		accessSemaphore.release();
	}
	
	public void removeConnection(Connection c) {
		accessSemaphore.acquireUninterruptibly();
			connectionSemaphore.acquireUninterruptibly();
			connections--;
			remaining--;
			chl.remove(c);
			kh.removeKey(c);
		accessSemaphore.release();
	}
	
	public OutputStream getOutputStream() {
		return os;
	}

//	public void broadcast() {
//		byte[] bb = new byte[DCPackage.PAYLOAD_SIZE];
//		DCPackage emptyPackage = new DCPackage(bb);
//		broadcast(emptyPackage);
//	}
	
	private void broadcast() {
		accessSemaphore.acquireUninterruptibly();
			byte[] message = os.getMessage();
			byte[] output = kh.getOutput(message);
			
			for(ConnectionHandler ch: chl) {
				try{
					ch.c.send(output);
					
				} catch (IOException e) {
					Debugger.println(1, e.getMessage());
				}
			}
			addInput(output);
		accessSemaphore.release();
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

		// We need to read connections + 1 outputs before this round is complete
		// That is: we have to receive a message from each station that we're connected
		// to, but we also need our own output.
		remaining = connections + 1;
	}

	/**
	 * Adds bytes to the input of the current round.
	 * @param input The input to be added
	 */
	private void addInput(byte[] input) {
		if(input.length != currentInput.length) {
			throw new InputMismatchException("[ConnectionBundle] The current output has length " + currentInput.length + " but the provided output has length " + input.length + ".");
		} else {
			for(int i = 0; i < currentInput.length; i++) {
				currentInput[i] ^= input[i];
			}
			remaining--;
			Debugger.println(2, "[ConnectionBundle] Remaining messages: " + remaining);
			if(remaining == 0) {
				inputBuffer.add(currentInput);
				inputAvailable.release();
				roundCompletionSemaphore.release();
				reset();
			}
		}
	}

	private class SimpleOutputStream extends OutputStream {
		private final int payloadSize;
		private byte[] currentMessage;
		private int writePointer;

		public SimpleOutputStream(int payloadSize) {
			this.payloadSize = payloadSize;
			currentMessage = new byte[payloadSize];
			writePointer = 0;
		}

		@Override
		public void write(int b) throws IOException {
			synchronized(this) {
				// Debugger.println(2, "[DummyConnection] writing " + (char)b);
				currentMessage[writePointer] = (byte) b;
				writePointer++;
				if(writePointer >= payloadSize) {
					synchronized(messageBuffer) {
						messageBuffer.add(currentMessage);
					}
					currentMessage = new byte[payloadSize];
					writePointer = 0;
				}
			}
		}
		
		private byte[] getMessage() {
			synchronized(messageBuffer) {
				byte[] message;
				if(messageBuffer.isEmpty()) {
					synchronized(this) {
						for(; writePointer < payloadSize; writePointer++) {
							currentMessage[writePointer] = 0;
						}
						message = currentMessage;
						currentMessage = new byte[payloadSize];
						writePointer = 0;
					}
				} else {
					message = messageBuffer.poll();
				}
				return message;
			}
		}
	}

	private class RoundInitializer implements Runnable {

		@Override
		public void run() {
			while(!isClosed) {
				try{
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// ignore
				}
				/**
				 *  Make sure that there are enough connections. This prevents stations
				 *  from broadcasting messages once the number of connections drops below
				 *  the minimum.
				 */
				connectionSemaphore.acquireUninterruptibly();
				connectionSemaphore.release();
				
				broadcast();
				// Wait until a new round can be started
				roundCompletionSemaphore.acquireUninterruptibly();
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
