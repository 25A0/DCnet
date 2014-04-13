package dc;

public class DcServer extends DCStation {
	private ConnectionBundle cb;

	public DcServer(String alias) {
		super(alias);
		cb = new ConnectionBundle();
		(new Thread(new InputReader())).start();
	}

	public ConnectionBundle getCB() {
		return cb;
	}

	@Override
	protected void addInput(byte[] message) {
		cb.broadcast(message);
	}
	
	/**
	 * This Runnable will constantly read output from the ConnectionBundle
	 * and if this server is at the top of the hierachy, it will
	 * reflect the outcome, otherwise it will forward it to the next
	 * layer.
	 */
	private class InputReader implements Runnable {

		@Override
		public void run() {
			while(!isClosed) {
				byte[] input = cb.receive();
				if(c != null) {
					broadcast(input);
				} else {
					cb.broadcast(input);
				}
			}
		}
	}
}