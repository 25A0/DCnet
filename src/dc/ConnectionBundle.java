package dc;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class ConnectionBundle {
	private KeyHandler kh;
	private ArrayList<Connection> cc;
	
	
	/**
	 *  This Semaphore is used to block until 
	 */
	private final Semaphore connectionSemaphore;
	
	/**
	 * This Semaphore is used to protect fields that are sensible in terms of concurrency
	 */
	private final Semaphore accessSemaphore;
	
	public ConnectionBundle() {
		kh = new KeyHandler();
		cc = new ArrayList<Connection>();
		
		/**
		 * This Semaphore is initalized in such a way that at least DCConfig.MIN_NUM_STATIONS 
		 * other stations have to be connected to this station before any communication happens.
		 */
		connectionSemaphore = new Semaphore(1 - DCConfig.MIN_NUM_STATIONS);
		
		/**
		 * Initially one party has access to the fields.
		 */
		accessSemaphore = new Semaphore(1);
	}
	
	public void addConnection(Connection c, byte[] key) {
		accessSemaphore.acquireUninterruptibly();
		cc.add(c);
		kh.addKey(c, key);
		connectionSemaphore.release();
		accessSemaphore.release();
	}
	
	public void removeConnection(Connection c) {
		accessSemaphore.acquireUninterruptibly();
		connectionSemaphore.acquireUninterruptibly();
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
		
		
		
	}
	
	public byte[] receive(int payload) {
		
	}
}
