package cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * <h2>Command line interface</h2>
 * @author Moritz Neikes
 *
 */
public class CLI {
	private BufferedReader br;
	private CLC controller;
	
	private CLC innerController;
	
	private boolean stopped = false;
	
	public CLI(CLC controller, String... args) {
		br = new BufferedReader(new InputStreamReader(System.in));
		this.innerController = controller;
		this.controller = new CLIController();
		try {
			this.controller.handle(args);
			readLoop();
			System.exit(0);
		} catch (IOException e) {
			System.err.println("[CLI] An error occurred while reading input from the command line.");
			e.printStackTrace();
		}
	}
	
	private void readLoop() throws IOException {
		while(!stopped) {
			String s = br.readLine();
			// TODO split on complete whitespaces, not just spaces
			controller.handle(s.split(" +"));
		}
	}
	
	/**
	 * This controller catches all commands that are meant to interact with this wrapper 
	 * rather than with the contained controller. 
	 * @author moritz
	 *
	 */
	private class CLIController extends CLC {
		public CLIController() {
			Action exitAction = new Action() {
				@Override
				public void execute(String... args) {
					stopped = true;
				}
			};
			Action debugAction = new Action() {
				@Override
				public void execute(String... args) {
					if(args.length < 1) return;
					else {
						int i;
						try{
							i = Integer.valueOf(args[0]);
							Debugger.setLevel(Integer.valueOf(i));
						} catch(Exception e) {return;}
					}
				}
			};
			
			Action innerAction = new CommandAction(innerController);
			
			this.mapCommand("exit", exitAction);
			this.mapCommand("-d", debugAction);
			this.setDefaultAction(innerAction);
		}
	}
}
