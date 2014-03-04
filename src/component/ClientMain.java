package component;

import java.util.Arrays;

import cli.CLI;
import cli.ClientInterface;

public class ClientMain {
	public static void main(String... args) {
		new CLI(new ClientInterface(), args);
	}
}
