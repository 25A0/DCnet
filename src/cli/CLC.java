package cli;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>Command line controller</h2>
 * @author moritz
 *
 */
public abstract class CLC {
	private Map<String, Action> commandMap;
	private Action defaultAction;
	
	/**
	 * The action that will be executed in case that no
	 * specific action has been registered for a given command
	 */
	protected final Action unknownCommandAction = new Action() {
		@Override
		public void execute(String... args) {
			System.out.println("The command " + args[0] + " is unknown.");
		}
	};
	
	/**
	 * Initializes local fields
	 */
	public CLC() {
		commandMap = new HashMap<String, Action>();
		defaultAction = unknownCommandAction;
	}
	
	/**
	 * This function is used to define the behaviour of the command line control.
	 * 
	 * @param command The command that triggers the action
	 * @param a The action that is executed when the given command occurs 
	 */
	protected final void mapCommand(String command, Action a) {
		if(commandMap.containsKey(command)) 
			System.err.println("[CLC] Command " + command + " has already been registered.");
		else if(a == null) {
			System.err.println("[CLC] Command " + command + " can not be linked to a null Action.");
		}
		else {
			commandMap.put(command, a);
		}
	}
	
	/**
	 * Changes the action that is executed when no command was triggered.
	 * This can be used to wrap an interface with additional commands without 
	 * disturbing the functionality of the enclosed interface.
	 * 
	 * @param a The command that is meant to be executed. 
	 */
	protected final void setDefaultAction(Action a) {
		if(a == null) 
			defaultAction = unknownCommandAction;
		else
			defaultAction = a;
			
	}
	
	public final void handle(String... args) {
		if(args.length == 0) return;
		else if(!commandMap.containsKey(args[0])) {
			// In this step ALL commands are forwarded to the default action
			defaultAction.execute(args);
		} else {
			// In this case the first command is consumed and only 
			// the remaining commands are forwarded
			commandMap.get(args[0]).execute(tail(args));
		}
	}
	
	private final String[] tail(String... args) {
		if(args.length <= 1) return new String[]{};
		else {
			String[] sa = new String[args.length-1];
			for(int i = 0; i < args.length-1; i++) {
				sa[i] = args[i+1];
			}
			return sa;
		}
	}
	
	protected abstract class Action {
		public abstract void execute(String... args);
	}
	
	protected final class CommandAction extends Action {
		private CLC clc;
		public CommandAction(CLC clc) {
			this.clc = clc;
		}
		public final void execute(String... args) {
			clc.handle(args);
		}
	}
}
