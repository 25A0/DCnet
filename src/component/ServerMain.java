package component;

import cli.CLI;
import cli.ServerInterface;

public class ServerMain {
	public static void main(String... args) {
		new CLI(new ServerInterface(), args);
	}
}
