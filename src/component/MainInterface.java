package cli;

import dc.DC;
import cli.CLC;
import dc.cli.MultiStationInterface;

public class MainInterface extends CLC {
	private MultiStationInterface mci;
	private Action forwardAction;
	public MainInterface() {
		System.out.println("DCnet command line interface, v" + DC.VERSION);
		mci = new MultiStationInterface();

		mapCommand("client", new CommandAction(mci));
		mapCommand("server", new CommandAction(si));
	}

}