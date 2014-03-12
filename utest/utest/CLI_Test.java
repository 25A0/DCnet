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
		System.out.println(a1.peek());
		assertTrue(a1.pop().equals("set"));
		System.out.println(a1.peek());
		assertFalse(a1.peek().isEmpty());
		assertFalse(a1.pop().isEmpty());
		System.out.println(a1.peek());
		assertTrue(a1.pop().equals("args"));
		assertTrue(a1.peek().isEmpty());
		assertTrue(a1.peek().isEmpty());
		assertTrue(a1.pop().isEmpty());
		
		
	}
}
