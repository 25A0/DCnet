package dc;

import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import dc.DCPackage;

import java.io.IOException;
import java.util.Arrays;

import util.Padding10;
import cli.Debugger;

public class DcClient extends DCStation{
	/**
	 * This Semaphore is used to keep track of the amount of input that
	 * can be read.
	 */
	private final Semaphore inputAvailable;
	private final LinkedList<byte[]> inputBuffer;
	private Scheduler scheduler;

	private int currentRound, nextRound;
	
	public static final long WAIT_TIME = 5000;
	
	
	/**
	 * This semaphore keeps track of the number of uncompleted rounds.
	 */
	private final Semaphore roundCompletionSemaphore;

	protected final MessageBuffer mb;


	public DcClient(String alias){
		super(alias);
		
		/**
		 * The number of initial releases determine how many rounds can run in parallel.
		 * For now, more than one round at a time will break the protocol.
		 */
		roundCompletionSemaphore = new Semaphore(0);
		
		inputAvailable = new Semaphore(0);
		inputBuffer = new LinkedList<byte[]>();
		mb = new MessageBuffer();

		scheduler = new PrimitiveScheduler();
		// We initially don't know the current round of the network
		// therefore this is initialized to a sentinel value
		currentRound = -1;
		// -1 is the sentinel value for 'no round is scheduled for us'
		nextRound = -1;
		
		// Start up the round initializer
		(new Thread(new RoundInitializer())).start();
		
	}

	public void setScheduler(Scheduler s) {
		this.scheduler = s;
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
		try {
			byte[] inputPayload = Padding10.revert10padding(message);
			if(inputPayload != null) {
				// We compare 'message' rather than 'inputPayload' since
				// the pending message includes padding.
				if(mb.hasPendingMessage()) {
					if(!mb.compareMessage(message)) {
						// TODO: report collision to statistics tracker.
						System.out.println("[DcClient "+alias+"] Collision detected");
					} else {
						mb.confirmMessage();
					}
				}
				inputBuffer.add(inputPayload);
				inputAvailable.release();
			}
		} catch(InputMismatchException e) {
			// TODO: report malformed message to statistics tracker
		} 
		// Announce the end of the round no matter what.
		roundCompletionSemaphore.release();	
	}

	

	private class RoundInitializer implements Runnable {

		@Override
		public void run() {
			while(!isClosed) {
				Debugger.println(2, "[DcClient "+alias+"] Waiting for connectionSemaphore");
				connectionSemaphore.acquireUninterruptibly();
				Debugger.println(2, "[DcClient "+alias+"] ConnectionSemaphore acquired");
				try{
					Thread.sleep(WAIT_TIME);
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
					Debugger.println(2, "[DcClient "+alias+"] Sending message " + Arrays.toString(message));
					output = kh.getOutput(message);
					Debugger.println(2, "[DcClient "+alias+"] Sending output " + Arrays.toString(output));
					
				}
				broadcast(output);
				connectionSemaphore.release();
				// Wait until a new round can be started
				roundCompletionSemaphore.acquireUninterruptibly();
			}
		}
	}

	

}