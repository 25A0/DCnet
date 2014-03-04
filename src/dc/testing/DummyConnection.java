package dc.testing;

import java.util.LinkedList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import cli.Debugger;

/**
 * A connection mockup that uses InputStream and OutputStream to forward data
 * @author moritz
 *
 */
public class DummyConnection {
	private final Semaphore dataSemaphore;
	private final Queue<Integer> dataQueue;
	private static final int QUEUE_BUFFER_LIMIT = 1 << ((Integer.SIZE >> 1) - 2);
	
	private final InputStream is;
	private final OutputStream os;
	
	public static final DummyConnection 
		SERVER = new DummyConnection(), 
		C1 = new DummyConnection(),
		C2 = new DummyConnection(),
		C3 = new DummyConnection();
				
	
	public DummyConnection() {
		dataSemaphore = new Semaphore(0);
		dataQueue = new LinkedList<Integer>();
		is = new DummyInputStream();
		os = new DummyOutputStream();
	}
	
	public InputStream getInputStream() {
		return is;
	}
	
	public OutputStream getOutputStream() {
		return os;
	}
	
	private class DummyOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			Debugger.println(2, "[DummyConnection] writing " + String.valueOf(b));
			if(dataSemaphore.availablePermits() > QUEUE_BUFFER_LIMIT) {
				throw new IOException("Buffer size limit reached!");
			} else {
				synchronized(dataQueue) {
					dataQueue.add(b);
					dataSemaphore.release();
				}
			}
		}
		
	}
	
	private class DummyInputStream extends InputStream {

		@Override
		public int read() throws IOException {
			dataSemaphore.acquireUninterruptibly();
			synchronized(dataQueue) {
				Debugger.println(2, "[DummyConnection] reading " + String.valueOf(dataQueue.peek()));
				return dataQueue.poll();
			}
		}
		
	}

}
