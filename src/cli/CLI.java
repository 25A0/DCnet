package cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

/**
 * <h2>Command line interface</h2>
 * @author Moritz Neikes
 *
 */
public class CLI {
	/**
	 * A reader that helps reading input from System.in
	 */
	private BufferedReader br;
	/**
	 * A local controller that handles some special commands like "exit"
	 */
	private CLC controller;
	/**
	 * The inner controller that handles all other commands.
	 */
	private CLC innerController;
	/**
	 * A boolean that determines whether the CLI should stop waiting for user input
	 */
	private boolean stopped = false;
	
	/**
	 * Initialises a new Command Line Interface. This interface holds a controller
	 * to handle commands.
	 * @param controller The controller that handles user commands
	 * @param args The list of arguments which are still to be evaluated
	 */
	public CLI(CLC controller, String... args) {
		br = new BufferedReader(new InputStreamReader(System.in));
		this.innerController = controller;
		this.controller = new CLIController();
		try {
			this.controller.handle(new ArgSet(args));
			readLoop();
			System.exit(0);
		} catch (IOException e) {
			System.err.println("[CLI] An error occurred while reading input from the command line.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Read a line of input from the console and process it.
	 * Checks if stopped is true. In this case the loop stops.
	 * @throws IOException
	 */
	private void readLoop() throws IOException {
		while(!stopped) {
			System.out.print("[DCnet] ");
			String s = br.readLine();
			// TODO split on complete whitespaces, not just spaces
			controller.handle(new ArgSet(s));
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
				public void execute(ArgSet args) {
					stopped = true;
				}
			};
			
			Action debugAction = new Action() {
				@Override
				public void execute(ArgSet args) {
					int i = args.fetchInteger();
					Debugger.setLevel(i);
				}
			};

			Action scriptAction = new Action() {
				@Override
				public void execute(ArgSet args) {
					if(!args.hasArg()) {
						System.out.println("[CommandLineInterface] Please provide a relative path to the script that you want to run");
					} else {
						String path = args.pop();
						try {
							File f = new File(path);
							BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
							while(br.ready()) {
								String s = br.readLine();
								controller.handle(new ArgSet(s));
							}
							// f.close();
						} catch(FileNotFoundException e) {
							System.out.println("[CommandLineInterface] The file " + path + " does not exist.");
						} catch(IOException e) {
							System.out.println("[CommandLineInterface] An error occurred while reading from file " + path);
							e.printStackTrace();
						}
					}

				}
			};

			Action echoAction = new Action() {
				@Override
				public void execute(ArgSet args) {
					if(args.hasStringArg()) {
						System.out.println(args.fetchString());
					} else if(args.hasArg()) {
						System.out.println(args.pop());
					} else {
						System.out.println("[CommandLineInterface] the command \"echo\" requires an argument enclosed by quotation marks");
					}
				}
			};
			
			Action innerAction = new CommandAction(innerController);
			
			mapCommand("exit", exitAction);
			mapAbbreviation('d', debugAction);
			mapOption("debug", debugAction);
			mapAbbreviation('r', scriptAction);
			mapCommand("run", scriptAction);
			mapCommand("echo", echoAction);
			setDefaultAction(innerAction);
		}
	}
}
