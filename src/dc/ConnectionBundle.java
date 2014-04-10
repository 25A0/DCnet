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

	/**
	 * This semaphore counts the completed rounds until a certain message has been sent
	 */
	private Semaphore messageSendStatusSemaphore;
	
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
		messageSendStatusSemaphore = new Semaphore(0);

		inputBuffer = new LinkedList<byte[]>();
		messageBuffer = new LinkedList<byte[]>();

		currentInput = new byte[DCPackage.PAYLOAD_SIZE];

		connections = 0;
		remaining = 1;
		
		os = new SimpleOutputStream();
		
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
				byte[] inputPayload = revert10padding(currentInput);
				// Only add input if the input wasn't empty
				if(inputPayload != null) {
					inputBuffer.add(inputPayload);
					inputAvailable.release();
				}
				// But announce the end of the round anyways.
				roundCompletionSemaphore.release();
				messageSendStatusSemaphore.release();
				reset();
			}
		}
	}

	/**
	 * Applies 10 padding to a byte array
	 * Similar to {@code apply10padding(byte[] input, int paddingStart, int goalSize)},
	 * but assumes that the complete input shall be contained in the padded output.
	 */
	private byte[] apply10padding(byte[] input, int goalSize) {
		return apply10padding(input, input.length, goalSize);
	}

	/**
	 * Applies 10 padding to a byte array.
	 *
	 * @param  input The byte array that will be contained in the resulting array. Must be smaller than {@code goalSize}
	 * @param  paddingStart The index of the first byte of the padding part.
	 * @param  goalSize The size of the array that the input will be padded to.
	 *
	 * @return  A byte array of size {@code goalSize} that contains {@code input}, followed by a {@code 1} at index {@code paddingStart}, followed by zero or more {@code 0}'s.
	 */
	private byte[] apply10padding(byte[] input, int paddingStart, int goalSize) {
		if(input != null && input.length +1 > goalSize) {
			throw new InputMismatchException("The provided input is too big to apply 10 padding");
		} 
		if(paddingStart > input.length || paddingStart > goalSize) {
			Debugger.println(0, "[ConnectionBundle] Warning: Cannot start padding at " + paddingStart);
			paddingStart = input.length;
		}

		byte[] output = new byte[goalSize];
		if(input != null) {
			// Note that we do not check for the bounds of input, since we checked earlier 
			// that paddingStart does not point beyond input.length
			for(int i = 0; i < paddingStart; i++) {
				output[i] = input[i];
			}
		}
		// Put the 1 down to mark the end of the payload
		output[paddingStart] = (byte) 1;
		// The remaining bytes have been initialized to 0
		return output;
		
	}

	/**
	 * Reverts 10 padding on a byte array.
	 * That means: Starting at the end, we expect to find 0's until we find a 1.
	 *
	 * @return The content of the input in front of the padding, or null if the input only contains {@code 0}'s.
	 */
	private byte[] revert10padding(byte[] input) {
		// An empty input array doesn't contain any information.
		if(input.length == 0) {
			return null;
		}

		int paddingStart = input.length - 1;
		for(; input[paddingStart] == 0; paddingStart--) {
			if(paddingStart == 0) {
				// In this case we went through the whole array without encountering anything
				// but 0's. This does not fulfil the requirements
				return null;
			}
		}
		// Check if we found a 1 now.
		if(input[paddingStart] == 1) {
			// That fulfills the padding pattern.
			// Copy the remaining input
			byte[] output = new byte[paddingStart];
			for(int i = 0; i < paddingStart; i++) {
				output[i] = input[i];
			}
			return output;
		} else {
			throw new InputMismatchException("The provided input is malformed.");
		}
	}

	public void block() {
		accessSemaphore.acquireUninterruptibly();
			int waitCounter = messageBuffer.size();
			if(os.writePointer > 0) waitCounter++;
			messageSendStatusSemaphore = new Semaphore(1-waitCounter);
		accessSemaphore.release();
		messageSendStatusSemaphore.acquireUninterruptibly();
	}

	private class SimpleOutputStream extends OutputStream {
		private final int payloadSize;
		private byte[] currentMessage;
		private int writePointer;

		public SimpleOutputStream() {
			// Subtract 1 for 10 padding
			payloadSize = DCPackage.PAYLOAD_SIZE - 1;
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
				/**
				 *  Make sure that there are enough connections. This prevents stations
				 *  from broadcasting messages once the number of connections drops below
				 *  the minimum. If too little connections are available then the station
				 *  will only send empty messages
				 */
				if(connectionSemaphore.tryAcquire()) {
					connectionSemaphore.release();
					if(!messageBuffer.isEmpty()) {
						// Fetch the last message from the buffer and apply padding
						return apply10padding(messageBuffer.poll(), DCPackage.PAYLOAD_SIZE);	
					} else if(writePointer > 0) {
						// Only in this case we have to apply padding
						synchronized(this) {
							byte[] message = apply10padding(currentMessage, writePointer, DCPackage.PAYLOAD_SIZE);
							currentMessage = new byte[payloadSize];
							writePointer = 0;
							return message;
						}
					}
				}
				// This catches everything that was not caught by previous conditions
				// That is: Either not enough connections have been established or 
				// there is nothing to be send.
				// We do not apply padding since the message is meant to be _empty_.
				return new byte[DCPackage.PAYLOAD_SIZE];
				
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
