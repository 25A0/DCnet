package dc;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import dc.MessageBuffer;
import net.Connection;
import net.Network;
import net.NetStatPackage;
import net.PackageListener;
import cli.Debugger;


public abstract class DCStation implements PackageListener{
	protected Connection c;
	protected final String alias;
	protected final KeyHandler kh;
	protected final Network net;
	
	protected boolean isClosed = false;
	
	// Keeps track of whether this station is actively participating in the
	// network or just listening.
	public boolean isActive = false;

	
	protected final Semaphore connectionSemaphore;

	public DCStation(String alias) {
		this.alias = alias;
		net = new Network();
		kh = new KeyHandler(alias);
		connectionSemaphore = new Semaphore(0);
		Debugger.println(2, "[DCStation] Station " + alias + " started");
	}

	public String getAlias() {
		return alias;
	}

	protected void broadcast(DCPackage output) {	
		try{
			c.send(output);
		} catch (IOException e) {
			connectionLost();
		}	
	}

	protected void broadcast(NetStatPackage output) {
		try {
			c.send(output);
		} catch(IOException e) {
			connectionLost();
		}
	}
	
	public void close() throws IOException {
		if(c!= null) {
			c.close();
		}
		isClosed = true;
	}
	
	public boolean isClosed() {
		return isClosed;
	}
	
	public void setState(boolean active) {
		if(c == null) {
			System.out.println("[DcClient " + alias +"] Can not switch states without being connected to a server.");
			return;
		}
		if(isActive == active) {
			System.out.println("[DcClient " + alias +"] This client is already in " + (isActive?"active":"idle") + " state.");
		} else if(active) {
			NetStatPackage nsp = new NetStatPackage.Joining(alias);
			broadcast(nsp);
		} else {
			NetStatPackage nsp = new NetStatPackage.Leaving(alias);
			broadcast(nsp);
		}
	}
	
	
	public void setConnection(Connection c) {
		if(this.c != null) {
			connectionSemaphore.acquireUninterruptibly();
			net.clear();
		}
		this.c = c;
		connectionSemaphore.release();

	}

	public KeyHandler getKeyHandler() {
		return kh;
	}

	@Override
	public void connectionLost() {
		c = null;
		connectionSemaphore.acquireUninterruptibly();
		System.out.println("[DcStation " + alias + "] Connection lost.");
	}


}
