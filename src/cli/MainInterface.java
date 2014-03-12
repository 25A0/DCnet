package cli;

import java.util.HashMap;
import java.util.Map;
import dc.DC;

public class MainInterface extends CLC {
	private Map<String, ClientInterface> ciMap;
	private ServerInterface si;
	private Action list, forwardAction;
	public MainInterface() {
		System.out.println("Welcome to the DCnet command line interface.\nVersion " + DC.VERSION);

		ciMap = new HashMap<String, ClientInterface>();

		forwardAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				String s = args.fetchString();
				if(!ciMap.containsKey(s)) {
					ciMap.put(s, new ClientInterface());
				}
				System.out.println("Forwarding to client " + s);
				ciMap.get(s).handle(args);
			}
		};

		si = new ServerInterface();

		list = new Action() {
			@Override
			public void execute(ArgSet args) {
				System.out.println(ciMap.keySet());
			}
		};

		mapCommand("client", forwardAction);
		mapCommand("list", list);
		mapCommand("server", new CommandAction(si));
	}

}