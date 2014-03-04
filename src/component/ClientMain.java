package component;

import cli.CLI;
import cli.ClientInterface;
import dc.server.DCServer;

public class ClientMain {
	public static void main(String... args) {
		new DCServer();
		new CLI(new ClientInterface(), args);
	}
}
