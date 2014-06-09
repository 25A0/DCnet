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

import net.NetStatPackage;
import net.NetStatPackage.Joining;
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

	
	
	protected final MessageBuffer mb;


	public DcClient(String alias){
		super(alias);
		
		inputAvailable = new Semaphore(0);
		inputBuffer = new LinkedList<byte[]>();

		scheduler = DCConfig.schedulingMethod.getScheduler();

		mb = new MessageBuffer(DCPackage.PAYLOAD_SIZE - scheduler.getScheduleSize());
		// We initially don't know the current round of the network
		// therefore this is initialized to a sentinel value
		nextRound = -1;
		// -1 is the sentinel value for 'no round is scheduled for us'
		nextScheduledRound = -1;
		
		
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

	public void stop() {
		mb.stop();
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
	public void addInput(DCPackage message) {
		byte[] inputPayload = null;
		try {
			inputPayload = Padding10.revert10padding(message.getMessage(scheduler.getScheduleSize()));
		} catch(InputMismatchException e) {
			System.out.println("Failed to revert 1-0 padding: " + e.getMessage());
		} 
		int number = message.getNumber();
		if(inputPayload != null) {
			// We compare 'message' rather than 'inputPayload' since
			// the pending message includes padding.
			if(mb.hasPendingMessage() && number == nextScheduledRound) {
				if(!mb.compareMessage(message.getMessage(scheduler.getScheduleSize()))) {
					// TODO: report collision to statistics tracker.
					Debugger.println("collision", "[DcClient "+alias+"] Collision detected");
				} else {
					mb.confirmMessage();
				}
			}
			inputBuffer.add(inputPayload);
			inputAvailable.release();
		}
		boolean waiting = mb.hasPendingMessage() || !mb.isEmpty();
		if(isActive && scheduler.addPackage(message, waiting)) {
			nextScheduledRound = scheduler.getNextRound();
			Debugger.println("scheduling", "[DcClient "+alias+"] successfully scheduled slot: " + nextScheduledRound);	
		}
		nextRound = ++number % message.getNumberRange();
		if(isActive) {
			sendOutput();
		}
	}

	@Override
	public void addInput(NetStatPackage nsp) {
		if(nsp instanceof NetStatPackage.Snapshot) {
			synchronized(net) {
				Debugger.println("network", "[DcClient " + alias + "] received Snapshot package");
				nsp.apply(net);
			}
		} else if (nsp instanceof NetStatPackage.Joining) {
			synchronized(net) {
				nsp.apply(net);
				String foreignAlias = ((NetStatPackage.Joining) nsp).getStation();
				Debugger.println("network", "[DcClient " + alias + "] Station " + foreignAlias + " joined the network");
				if(foreignAlias.equals(alias)) {
					Debugger.println("network", "[DcClient " + alias + "] State changed to active");
					isActive = true;
				} 
			}
			//won't resend
		} else {
			synchronized(net) {
				nsp.apply(net);
				String foreignAlias = ((NetStatPackage.Leaving) nsp).getStation();
				Debugger.println("network", "[DcClient " + alias + "] Station " + foreignAlias + " left the network");
				if(foreignAlias.equals(alias)) {
					Debugger.println("network", "[DcClient " + alias + "] State changed to inactive");
					isActive = false;
				} else {
					sendOutput();
				}
			}
		}
	}

	

	private void sendOutput() {
		// try{
		// 	Thread.sleep(WAIT_TIME);
		// } catch (InterruptedException e) {
		// 	// duh
		// }
		if(isClosed()) return;
		byte[] output;
		synchronized(kh) { 
			synchronized(net) {
				byte[] message;
				/**
				 *  Make sure that there are enough connections. This prevents stations
				 *  from broadcasting messages once the number of connections drops below
				 *  the minimum. If too little connections are available then the station
				 *  will only send empty messages
				 */
				// System.out.println(alias + ": Are we allowed to send? " + (kh.approved(net.getStations())? " Yes":"No"));
				if(kh.approved(net.getStations()) && nextScheduledRound == nextRound) {
					Debugger.println("messages", "[DcClient " + alias + "] Sending...");
					message = mb.getMessage();
				} else {
					message = new byte[DCPackage.PAYLOAD_SIZE - scheduler.getScheduleSize()];
				}
				Debugger.println(2, "[DcClient "+alias+"] Sending message " + Arrays.toString(message));
				output = kh.getOutput(scheduler.getSchedule(), message, net.getStations());
				Debugger.println(2, "[DcClient "+alias+"] Sending output " + Arrays.toString(output));
				
			} 
		}
		DCPackage pckg = new DCPackage(nextRound, output);
		broadcast(pckg);
	}
	
}