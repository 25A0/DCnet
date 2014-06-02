package dc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import cli.Debugger;
import dc.DCPackage;
import net.Connection;
import net.Network;
import net.PackageListener;
import net.NetStatPackage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.LinkedList;

public class ConnectionBundle {
	private final ArrayList<ConnectionHandler> chl;
	private int connections;
	private int activeConnections;

	private int currentRound;

	/**
	 * This HashMap contains all connections that are identified by an alias.
	 */
	private HashMap<String, ConnectionHandler> identifiedConnections;
	
	private DCPackage pendingPackage;
	private int remaining;	

	private boolean isClosed = false;

	private final LinkedList<DCPackage> inputBuffer;
	private final Semaphore inputAvailable;

	private final LinkedList<NetStatPackage> netStatBuffer;
	private final Semaphore statusAvailable;
		

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

		netStatBuffer = new LinkedList<NetStatPackage>();
		statusAvailable = new Semaphore(0);
		
		connections = 0;
		activeConnections = 0;

		currentRound = 0;
	}
	
	public void addConnection(Connection c) {
		Debugger.println(2, "[ConnectionBundle] New connection to " + c.toString());
		
		accessSemaphore.acquireUninterruptibly();
			ConnectionHandler ch = new ConnectionHandler(c);
			c.setListener(ch);
			chl.add(ch);
			connections ++;
		accessSemaphore.release();
	}
	
	public void removeConnection(ConnectionHandler ch) {
		accessSemaphore.acquireUninterruptibly();
			connections--;
			chl.remove(ch);
			if(ch.isActive) {
				// Communicate this to the server
				NetStatPackage nsp = new NetStatPackage.Leaving(ch.alias);
				netStatBuffer.add(nsp);
				statusAvailable.release();
			}
		accessSemaphore.release();
	}

	public void handle(NetStatPackage message) {
		if(message.getClass().equals(NetStatPackage.Joining.class)) {
			String alias = ((NetStatPackage.Joining) message).getStation();
			if(identifiedConnections.containsKey(alias)) {
				ConnectionHandler ch = identifiedConnections.get(alias);
				ch.setStatus(true);
				activeConnections++;
			}
		} else if(message.getClass().equals(NetStatPackage.Leaving.class)) {
			String alias = ((NetStatPackage.Leaving) message).getStation();
			if(identifiedConnections.containsKey(alias)) {
				ConnectionHandler ch = identifiedConnections.get(alias);
				ch.setStatus(false);
				activeConnections--;
				// everyone will resend their messages.
				resetRound();
			}
		} 
		// We don't handle snapshot messages. 
		
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

	public void broadcast(NetStatPackage message) {
		for(ConnectionHandler ch: chl) {
			try {
				ch.c.send(message);
			} catch(IOException e) {
				Debugger.println(1, e.getMessage());
			}
		}
	}

	public void pulse() {
		DCPackage pulsePackage = new DCPackage(currentRound, new byte[DCPackage.PAYLOAD_SIZE]);
		broadcast(pulsePackage);
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
	
	public DCPackage receiveDCPackage() {
		inputAvailable.acquireUninterruptibly();
		return inputBuffer.pop();
	}

	public NetStatPackage receiveStatusPackage() {
		statusAvailable.acquireUninterruptibly();
		return netStatBuffer.pop(); 
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
			if(round != currentRound) {
				// we refuse this package.
				return;
			}
			if(pendingPackage == null) {
				pendingPackage = input;
			} else {
				pendingPackage.combine(input);
			}
			remaining--;
			Debugger.println("message-cycle", "[ConnectionBundle] Remaining messages: " + remaining);
			if(remaining == 0) {
				Debugger.println(2, "[ConnectionBundle] Composed input: " + pendingPackage.toString());
				inputBuffer.add(pendingPackage);
				inputAvailable.release();
				currentRound = (pendingPackage.getNumber()+1) % pendingPackage.getNumberRange();

				resetRound();
			}
		}
	}

	private void resetRound() {
		pendingPackage = null;
		remaining = activeConnections;	
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


	private class ConnectionHandler implements PackageListener {
		public final Connection c;

		public String alias = null;
		public boolean isActive = false;

		public ConnectionHandler(Connection c) {
			this.c = c;
		}

		public void close() throws IOException {
			c.close();
			isClosed = true;
		}

		public void setStatus(boolean isActive) {
			Debugger.println("network", "[ConnectionBundle] Station " + alias + " is now " + (isActive?"active.":"inactive."));
			this.isActive = isActive;
		}

		@Override
		public void addInput(DCPackage message) {
			// Refuse message if this connection isn't active.
			if(!isActive) return;
			accessSemaphore.acquireUninterruptibly();
			addInput(message);
			accessSemaphore.release();
		}

		@Override
		public void addInput(NetStatPackage message) {
			if(!isActive && message.getClass().equals(NetStatPackage.Joining.class)) {
				// We now know which alias belongs to this connection.
				identifiedConnections.put(((NetStatPackage.Joining) message).getStation(), this);
			}
			netStatBuffer.add(message);
			statusAvailable.release();
		}

		@Override
		public void connectionLost() {
			removeConnection(this);
		}
	}

}
