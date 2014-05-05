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
	protected void addInput(DCPackage message) {
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
				DCPackage input = cb.receive();
				if(c != null) {
					input.combine(kh.getOutput(DCPackage.PAYLOAD_SIZE));
					System.out.println("[DcServer " + alias + "] Spreading package " + input.toString());
					broadcast(input);
				} else {
					cb.broadcast(input);
				}
			}
		}
	}
}