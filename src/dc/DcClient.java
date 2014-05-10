package dc;

import java.util.InputMismatchException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Semaphore;

import dc.DCPackage;
import dc.scheduling.Scheduler;
import dc.scheduling.PrimitiveScheduler;

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
	// The index of the next round to be sent
	private int nextRound;
	// The index of the next round that is scheduled for us to send something
	private int nextScheduledRound;
	
	// The time that this client waits between sending messages
	// This is a minimal wait time, the client will wait longer if the current round
	// takes longer to finish
	public static final long WAIT_TIME = 1000;

	// The number of times clients will wait before they assume that they
	// are alone in the network or everyone is waiting for each other to start
	public static final long AWKWARD_SILENCE_LIMIT = 3;
	
	
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

		scheduler = DCConfig.schedulingMethod.getScheduler();

		mb = new MessageBuffer(DCPackage.PAYLOAD_SIZE - scheduler.getScheduleSize());
		// We initially don't know the current round of the network
		// therefore this is initialized to a sentinel value
		nextRound = -1;
		// -1 is the sentinel value for 'no round is scheduled for us'
		nextScheduledRound = -1;
		
		// Start up the round initializer
		(new Thread(new RoundInitializer())).start();
		
	}

	/**
	 * Changes the scheduler to be used and resets the round numbers
	 * @param s The scheduler to be used
	 */
	public void setScheduler(Scheduler s) {
		this.scheduler = s;
		nextScheduledRound = -1;
	}

	public void send(String s) throws IOException {
		mb.write(s.getBytes());
	}
	
	public void send(byte b) throws IOException {
		mb.write(b);
	}

	/**
	 * Checks whether this client has received any messages since the last call to {@code receive}
	 * @return true if there are messages to be read, false otherwise
	 */
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
	protected void addInput(DCPackage message) {
		byte[] inputPayload = null;
		try {
			inputPayload = Padding10.revert10padding(message.getMessage(scheduler.getScheduleSize()));
		} catch(InputMismatchException e) {
			System.out.println("Failed to revert 1- padding");
		} 
		int number = message.getNumber();
		if(inputPayload != null) {
			// We compare 'message' rather than 'inputPayload' since
			// the pending message includes padding.
			if(mb.hasPendingMessage() && number == nextScheduledRound) {
				if(!mb.compareMessage(message.getMessage(scheduler.getScheduleSize()))) {
					// TODO: report collision to statistics tracker.
					System.out.println("[DcClient "+alias+"] Collision detected");
				} else {
					mb.confirmMessage();
				}
			}
			inputBuffer.add(inputPayload);
			inputAvailable.release();
		}
		if(scheduler.addPackage(message)) {
			nextScheduledRound = scheduler.getNextRound();
		}
		nextRound = ++number % message.getNumberRange();
		// Announce the end of the round no matter what.
		roundCompletionSemaphore.release();	
	}

	

	private class RoundInitializer implements Runnable {
		private int passedSilentRounds = 0;
		@Override
		public void run() {
			while(!isClosed) {
				connectionSemaphore.acquireUninterruptibly();
				try{
					Thread.sleep(WAIT_TIME);
					if(nextRound == -1) {
						// In this case we haven't received a message from our network yet
						if(passedSilentRounds > AWKWARD_SILENCE_LIMIT) {
							// We start sending and assume that the current round number is 0
							Debugger.println(1, "[DcClient "+alias+"] starts sending after awkward silence");
							nextScheduledRound = 0;
							nextRound = 0;
						} else {
							// We keep waiting
							passedSilentRounds++;
							connectionSemaphore.release();
							continue;
						}
					}
				} catch (InterruptedException e) {
					// ignore
				}
				byte[] output;
				synchronized(kh) {
					byte[] message;
					/**
					 *  Make sure that there are enough connections and test if we planned to send an actual message in the upcoming round. 
					 *  This prevents stations from broadcasting messages once the number of connections drops below
					 *  the minimum. If too little connections are available, then the station
					 *  will only send empty messages
					 */
					if(kh.approved() && nextScheduledRound == nextRound) {
						message = mb.getMessage();
					} else {
						message = new byte[DCPackage.PAYLOAD_SIZE - scheduler.getScheduleSize()];
					}
					Debugger.println(2, "[DcClient "+alias+"] Sending message " + Arrays.toString(message));
					output = kh.getOutput(scheduler.getSchedule(), message);
					Debugger.println(2, "[DcClient "+alias+"] Sending output " + Arrays.toString(output));
					
				}
				DCPackage pckg = new DCPackage(nextRound, output);
				broadcast(pckg);
				
				connectionSemaphore.release();
				// Wait until a new round can be started
				roundCompletionSemaphore.acquireUninterruptibly();
			}
		}
	}

	

}