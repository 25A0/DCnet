package cli;

import dc.DC;

public class MainInterface extends CLC {
	private ServerInterface si;
	private MultiClientInterface mci;
	private Action forwardAction;
	public MainInterface() {
		System.out.println("DCnet command line interface, v" + DC.VERSION);
		si = new ServerInterface();
		mci = new MultiClientInterface();

		mapCommand("client", new CommandAction(mci));
		mapCommand("server", new CommandAction(si));
	}

}