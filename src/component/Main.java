package component;

import cli.CLI;
import cli.MainInterface;

public class Main {

	public static void main(String... args) {
		new CLI(new MainInterface());
	}
}