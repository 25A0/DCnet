package cli;

import java.util.HashMap;
import java.util.Map;


public class MultiClientInterface extends CLC {
	private Map<String, ClientInterface> ciMap;
	private Action listAction, forwardAction;

	public MultiClientInterface() {
		ciMap = new HashMap<String, ClientInterface>();

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
						ciMap.put(s, new ClientInterface());
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