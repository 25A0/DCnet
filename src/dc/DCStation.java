package dc;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import cli.Debugger;

public class DCStation {
	private final LinkedList<String> pendingData;
	private Semaphore pendingDataSemaphore;
	private ConnectionBundle cb;
	
	private BufferedOutputStream bos;

	private boolean isClosed = false;
	
	public DCStation() {
		Debugger.println(2, "[DCStation] Station started");
		cb = new ConnectionBundle();
		pendingData = new LinkedList<String>();
		pendingDataSemaphore = new Semaphore(0);
		
		bos = new BufferedOutputStream(cb.getOutputStream());
		// (new Thread(new ProtocolCore())).start();
	}
	
	public void close() {
		isClosed = true;
	}
	
	public ConnectionBundle getCB() {
		return cb;
	}
	
	public void send(String s) throws IOException {
		cb.getOutputStream().write(s.getBytes());
	}
	
	private class ProtocolCore implements Runnable {
		
		@Override
		public void run() {
			while(!isClosed) {
				try{
					Thread.sleep(1000);
					pendingDataSemaphore.acquireUninterruptibly();
					
				} catch(InterruptedException e) {
					System.err.println("[DCStation] Unable to complete sleep. Proceeding...");
				}

				
			}
		}
	}

}
