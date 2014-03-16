package cli;

import java.util.HashMap;
import java.util.Map;


public class MultiStationInterface extends CLC {
	private Map<String, StationInterface> ciMap;
	private Action listAction, forwardAction;

	public MultiStationInterface() {
		ciMap = new HashMap<String, StationInterface>();

		listAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				System.out.println(ciMap.keySet());
			}
		};

		forwardAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				String s = args.fetchString();
				if(!ciMap.containsKey(s)) {
						ciMap.put(s, new StationInterface());
					}
				ciMap.get(s).handle(args);
			}
		};

		setRootAction(listAction);
		setDefaultAction(forwardAction);
		mapAbbreviation('l', listAction);
		mapOption("list", listAction);
	}
}