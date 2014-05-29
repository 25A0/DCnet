package utest;
import static org.junit.Assert.*;

import org.junit.Test;

import cli.ArgSet;



public class CLI_Test {
	
	@Test
	public void testArgSet() {
		ArgSet a1 = new ArgSet("some set of args");
		assertTrue(a1.peek().equals("some"));
		assertTrue(a1.peek().equals("some"));
		assertTrue(a1.pop().equals("some"));
		assertFalse(a1.peek().equals("some"));
		assertTrue(a1.pop().equals("set"));
		assertFalse(a1.peek().isEmpty());
		assertFalse(a1.pop().isEmpty());
		assertTrue(a1.pop().equals("args"));
		assertTrue(a1.peek().isEmpty());
		assertTrue(a1.peek().isEmpty());
		assertTrue(a1.pop().isEmpty());
		
		ArgSet a2 = new ArgSet("string another string 12 and 12");
		assertTrue(a2.hasArg());
		assertFalse(a2.hasIntArg());
		assertTrue(a2.pop().equals("string"));
		assertTrue(a2.peek().equals("another"));
		a2.pop();
		assertTrue(a2.hasArg());
		assertFalse(a2.hasIntArg());
		a2.pop();
		assertTrue(a2.hasIntArg());
		assertTrue(a2.hasArg());
		assertTrue(a2.peek().equals("12"));
		a2.pop();
		assertTrue(a2.peek().equals("and"));
		assertTrue(a2.hasArg());
		assertFalse(a2.hasIntArg());
	}
	
	@Test
	public void TestOptions() {
		ArgSet a1 = new ArgSet("nooption -o --option -op -- - -");
		assertTrue(a1.hasArg());
		assertFalse(a1.hasAbbArg());
		assertFalse(a1.hasOptionArg());
		a1.pop();
		assertFalse(a1.hasOptionArg());
		assertTrue(a1.hasAbbArg());
		assertTrue(a1.fetchAbbr().equals('o'));
		assertTrue(a1.hasOptionArg());
		assertFalse(a1.hasAbbArg());
		assertTrue(a1.fetchOption().equals("option"));
		assertFalse(a1.hasAbbArg());
		assertFalse(a1.hasOptionArg());
		assertTrue(a1.pop().equals("-op"));
		assertFalse(a1.hasAbbArg());
		assertFalse(a1.hasOptionArg());
		a1.pop();
		assertFalse(a1.hasAbbArg());
		assertFalse(a1.hasOptionArg());
		a1.pop();
		assertFalse(a1.hasAbbArg());
		assertFalse(a1.hasOptionArg());
		a1.pop();
	}

	@Test
	public void testStringArgs() {
		ArgSet a1 = new ArgSet("arguments \"with a string argument\" in between");
		assertTrue(a1.hasArg());
		assertFalse(a1.hasStringArg());
		assertTrue(a1.pop().equals("arguments"));
		assertTrue(a1.hasStringArg());
		assertTrue(a1.hasArg());
		assertTrue(a1.peek().equals("\"with a string argument\""));
		assertTrue(a1.fetchString().equals("with a string argument"));
		assertTrue(a1.hasArg());
		assertFalse(a1.hasStringArg());
		
	}
}
