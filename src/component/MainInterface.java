package component;

import dc.DCConfig;
import cli.CLC;
import dc.cli.MultiStationInterface;

public class MainInterface extends CLC {
	private MultiStationInterface msi;
	
	public MainInterface() {
		System.out.println("DCnet command line interface, v" + DCConfig.VERSION);
		msi = new MultiStationInterface();

		mapCommand("station", new CommandAction(msi));
		
	}

}