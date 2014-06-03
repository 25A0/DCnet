package net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.LinkedList;

import cli.Debugger;
import dc.DCConfig;
import dc.DCPackage;
import dc.testing.DummyConnection;

public abstract class Connection {
	private final InputStream is;
	private final OutputStream os;

	private boolean isClosed = false;

	private PackageListener listener;
	
	private LinkedList<NetStatPackage> statusBuffer;
	private LinkedList<DCPackage> messageBuffer;
	
	public Connection(InputStream is, OutputStream os) {
		this(is, os, null);
	}
	
	public Connection(InputStream is, OutputStream os, PackageListener listener) {
		this.is = is;
		this.os = os;
		this.listener = listener;
		statusBuffer = new LinkedList<NetStatPackage>();
		messageBuffer = new LinkedList<DCPackage>();

		(new Thread(new InputReader())).start();
	}
	
	public void setListener(PackageListener listener) {
		this.listener = listener;
		// This does not work well: the two separate
		// lists do not preserve the order in which 
		// messages arrive.
		while(!statusBuffer.isEmpty()) {
			listener.addInput(statusBuffer.pop());
		}
		while(!messageBuffer.isEmpty()) {
			listener.addInput(messageBuffer.pop());
		}
	}

	private class InputReader implements Runnable {
		@Override
		public void run() {
			try {
				while(!isClosed) {
					byte type = (byte) is.read();
					if(type == 1) {
						DCPackage dp = receiveDCPackage();
						if(listener == null) {
							messageBuffer.add(dp);
						} else {
							listener.addInput(dp);							
						}
					} else {
						NetStatPackage nsp = receiveStatusPackage();
						if(listener == null) {
							statusBuffer.add(nsp);
						} else {
							listener.addInput(nsp);							
						}
					}
				}
			} catch (IOException e) {
				listener.connectionLost(e.getMessage());
			}
		}
		
	}


	public void send(DCPackage p) throws IOException {
		synchronized(os) {
			os.write((byte) 1);
			os.write(p.toByteArray());
		}
	}

	public void send(NetStatPackage p) throws IOException {
		synchronized(os) {
			os.write((byte) 0);
			os.write(p.toByteArray());
		}
	}
	
	private DCPackage receiveDCPackage() throws IOException {
		byte[] buffer = new byte[DCPackage.PACKAGE_SIZE];
		for(int i = 0; i < DCPackage.PACKAGE_SIZE; i++) {
			buffer[i] = (byte) is.read();
		}
		return DCPackage.getPackage(buffer);
		// String s = Arrays.toString(buffer);
		// Debugger.println(2, "[Connection] reading " + s);
		// return DCPackage.getMessages(s)[0];
	}

	
	private NetStatPackage receiveStatusPackage() throws IOException {
		byte header = (byte) is.read();
		if((header & (1 << 7)) != 0) {
			return parseSnapshot(header, is);
		} else {
			boolean joining = (header & 1) == 0;
			byte[] alias = new byte[DCConfig.ALIAS_LENGTH];
			is.read(alias);
			int start = 0;
			while(alias[start] == (byte)0) start++;
			
			if(joining) {
				return new NetStatPackage.Joining(new String(alias, start, DCConfig.ALIAS_LENGTH - start));
			} else {
				return new NetStatPackage.Leaving(new String(alias, start, DCConfig.ALIAS_LENGTH - start));
			}
		}
	}

	private NetStatPackage parseSnapshot(byte header, InputStream is) throws InputMismatchException, IOException{
		int length = 0;
		int shifts = 0;
		boolean cont;
		do {
			int count = (header & 0x3F) << (6*shifts++);
			length |= count;
			if(shifts > 4) {
				throw new InputMismatchException("A network of this size might cause Integer overflows.");
			}
			cont = (header & (1 << 6)) != 0;
			if(cont) {
				header = (byte) is.read();
			}
		} while(cont);
		ArrayList<String> aliases = new ArrayList<String>();
		for(int i = 0; i < length; i++) {
			byte[] alias = new byte[DCConfig.ALIAS_LENGTH];
			is.read(alias);
			aliases.add(new String(alias));
		}
		return new NetStatPackage.Snapshot(aliases);
	}

	public void close() throws IOException {
		isClosed = true;
	}
}
