package cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

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
	
	public CLI(CLC controller) {
		br = new BufferedReader(new InputStreamReader(System.in));
		this.innerController = controller;
		this.controller = new CLIController();
		try {
			readLoop();
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
	
	private class CLIController extends CLC {
		public CLIController() {
			Action exitAction = new Action() {
				@Override
				public void execute(String... args) {
					stopped = true;
				}
			};
			Action innerAction = new CommandAction(innerController);
			
			this.mapCommand("exit", exitAction);
			this.setDefaultAction(innerAction);
		}
	}
}
