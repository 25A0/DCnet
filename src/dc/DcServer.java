package dc;

import net.Connection;
import net.NetworkConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class DcServer extends DCStation {
	private ConnectionBundle cb;

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
		(new Thread(new InputReader())).start();
	}

	public ConnectionBundle getCB() {
		return cb;
	}

	@Override
	protected void addInput(DCPackage message) {
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
				DCPackage input = cb.receive();
				input.combine(kh.getOutput(DCPackage.PAYLOAD_SIZE));
				if(c != null) {
					broadcast(input);
				} else {
					cb.broadcast(input);
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
					cb.addConnection(new NetworkConnection(s));
				}
				servSock.close();
			} catch (IOException e) {
				System.err.println("NetworkListener crashed");
				e.printStackTrace();
			}
		}
	}
}