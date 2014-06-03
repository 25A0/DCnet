package dc;

import cli.Debugger;

import net.Connection;
import net.NetworkConnection;
import net.NetStatPackage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class DcServer extends DCStation {
	private ConnectionBundle cb;

	private boolean needsPulse;

	/**
	 * Starts a DcServer that is able to handle network traffic
	 * @param  alias The alias that identifies this server
	 * @param  port  The port that will be used to accept incoming connections
	 */
	public DcServer(String alias, int port) {
		this(alias);
		(new Thread(new NetworkListener(port))).start();
	}

	/**
	 * Starts a local DcServer. If you want to handle network traffic, 
	 * use the constructor {@code DcServer(String alias, int port)} instead.
	 * @param  alias The alias that identifies this server
	 */
	public DcServer(String alias) {
		super(alias);
		cb = new ConnectionBundle();
		needsPulse = true;
		(new Thread(new InputReader())).start();
		(new Thread(new NetStatInputListener())).start();
		(new Thread(new Pulser())).start();
	}

	public ConnectionBundle getCB() {
		return cb;
	}

	@Override
	public void addInput(DCPackage message) {
		cb.broadcast(message);
	}

	@Override
	public void addInput(NetStatPackage message) {
		message.apply(net);
		cb.handle(message);
		cb.broadcast(message);
	}

	@Override
	public void close() throws IOException{
		cb.close();
		super.close();
	}
	
	/**
	 * This Runnable will constantly read output from the ConnectionBundle
	 * and if this server is at the top of the hierarchy, it will
	 * reflect the outcome, otherwise it will forward it to the next
	 * layer.
	 */
	private class InputReader implements Runnable {

		@Override
		public void run() {
			while(!isClosed) {
				DCPackage input = cb.receiveDCPackage();
				input.combine(kh.getOutput(DCPackage.PAYLOAD_SIZE, net.getStations()));
				needsPulse = false;
				if(c != null) {
					broadcast(input);
				} else {
					cb.broadcast(input);
				}
			}
		}
	}

	private class NetStatInputListener implements Runnable {

		@Override
		public void run() {
			while(!isClosed) {
				NetStatPackage nsp = cb.receiveStatusPackage();
				if(c != null) {
					broadcast(nsp);
				} else {
					nsp.apply(net);
					cb.handle(nsp);
					cb.broadcast(nsp);
				}
			}
		}
	}

	/**
	 * This class sends an empty round to all connected stations 
	 * to start the conversation. This only needs to happen once
	 * to get the network going.
	 */
	private class Pulser implements Runnable {
		private static final long INTERVAL = 3000;
		@Override
		public void run() {
			while(!isClosed) {
				try {
					Thread.sleep(INTERVAL);
					// In case this is not the highest server in the hierarchy, 
					// this server is not responsible for initializing a conversation.
					if(c != null) return;
					// else we let the connectionBundle send out a pulse to all 
					// connected stations.
					cb.pulse();
					Debugger.println("server", "[Info] Server " + alias + " sent pulse message.");
				} catch(InterruptedException e) {
					continue;
				}
			}
		}
	}

	/**
	 * This Runnable will accept incoming network connections on the specified
	 * path. It will be started automatically on a separate thread if the
	 * constructor {@code DcServer(String alias, int port)} was used.
	 */
	private class NetworkListener implements Runnable {
		private ServerSocket servSock;
		private int port;

		public NetworkListener(int port) {
			this.port = port;
		}

		@Override
		public void run() {
			try {
				servSock = new ServerSocket(port);
				while(!isClosed) {
					Socket s = servSock.accept();
					NetworkConnection ncon = new NetworkConnection(s);
					cb.addConnection(ncon);
					ncon.send(new NetStatPackage.Snapshot(net.getStations()));
				}
				servSock.close();
			} catch (IOException e) {
				System.err.println("NetworkListener crashed");
				e.printStackTrace();
			}
		}
	}
}