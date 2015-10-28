package component;

import dc.DCConfig;
import cli.CLC;
import dc.cli.MultiStationInterface;

public class MainInterface extends CLC {
	private MultiStationInterface msi;
	
	public MainInterface() {
		System.out.println("DCnet command line interface, v" + DCConfig.VERSION);
		System.out.println("2014, Moritz Neikes, m.neikes@student.ru.nl");
		msi = new MultiStationInterface();
		Action forwardAction = new CommandAction(msi);

		setDefaultAction(forwardAction);
		mapCommand("dc", forwardAction);
	}

}