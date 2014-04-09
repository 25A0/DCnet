package dc;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import cli.Debugger;

public class DCStation {
	private ConnectionBundle cb;
	
	private BufferedOutputStream bos;

	private boolean isClosed = false;
	
	public DCStation() {
		Debugger.println(2, "[DCStation] Station started");
		cb = new ConnectionBundle();
		
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
	
	public void send(byte b) throws IOException {
		cb.getOutputStream().write(b);
	}
	
	

}
