package dc;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import dc.DCPackage;

import java.io.IOException;

import util.Padding10;
import cli.Debugger;

public class DcClient extends DCStation{
	/**
	 * This Semaphore is used to keep track of the amount of input that
	 * can be read.
	 */
	private final Semaphore inputAvailable;
	private final LinkedList<byte[]> inputBuffer;
	
	
	/**
	 * This semaphore keeps track of the number of uncompleted rounds.
	 */
	private final Semaphore roundCompletionSemaphore;

	protected final MessageBuffer mb;


	public DcClient(){
		super();
		
		/**
		 * The number of initial releases determine how many rounds can run in parallel.
		 * For now, more than one round at a time will break the protocol.
		 */
		roundCompletionSemaphore = new Semaphore(0);
		
		inputAvailable = new Semaphore(0);
		inputBuffer = new LinkedList<byte[]>();
		mb = new MessageBuffer();
		
		// Start up the round initializer
		(new Thread(new RoundInitializer())).start();
		
	}

	public void send(String s) throws IOException {
		mb.write(s.getBytes());
	}
	
	public void send(byte b) throws IOException {
		mb.write(b);
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

	public void block() {
		System.out.println("[DcClient] SEVERE: blocking is currently unavailable");
	}

	@Override
	protected void addInput(byte[] message) {
		byte[] inputPayload = Padding10.revert10padding(message);
		// Only add input if the input wasn't empty
		if(inputPayload != null) {
			inputBuffer.add(inputPayload);
			inputAvailable.release();
		}
		// But announce the end of the round anyways.
		roundCompletionSemaphore.release();	
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
				byte[] output;
				synchronized(kh) {
					byte[] message;
					/**
					 *  Make sure that there are enough connections. This prevents stations
					 *  from broadcasting messages once the number of connections drops below
					 *  the minimum. If too little connections are available then the station
					 *  will only send empty messages
					 */
					if(kh.approved()) {
						message = mb.getMessage();
					} else {
						message = new byte[DCPackage.PAYLOAD_SIZE];
					}
					output = kh.getOutput(message);
				}
				broadcast(output);
				// Wait until a new round can be started
				roundCompletionSemaphore.acquireUninterruptibly();
			}
		}
	}

	

}