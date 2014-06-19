package component;

import cli.CLI;

public class Main {

	public static void main(String... args) {
		new CLI(new MainInterface(), args);
	}
}