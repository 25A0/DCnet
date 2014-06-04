package component;

import dc.DCConfig;
import cli.CLC;
import dc.cli.MultiStationInterface;
import benchmarking.SchedulingController;

public class MainInterface extends CLC {
	private MultiStationInterface msi;
	private SchedulingController si;
	
	public MainInterface() {
		System.out.println("DCnet command line interface, v" + DCConfig.VERSION);
		msi = new MultiStationInterface();
		Action forwardAction = new CommandAction(msi);

		si = new SchedulingController();
		Action schedulingAction = new CommandAction(si);
		
		setDefaultAction(forwardAction);
		mapCommand("station", forwardAction);
		mapCommand("scheduling", schedulingAction);		
	}

}