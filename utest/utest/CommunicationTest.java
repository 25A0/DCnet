package utest;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import dc.DCMessage;
import dc.testing.DummyConnection;


public class CommunicationTest {

	@Test
	public void testMessages() {
		String s = "testString";
		byte[] bb = s.getBytes();
		DCMessage m = DCMessage.getMessage(bb);
		assertArrayEquals(bb, m.toByteArray());
		
	}
	
	@Test
	public void testDummyCommunication() throws IOException {
		DummyConnection dc = new DummyConnection();
		InputStream is = dc.getInputStream();
		OutputStream os = dc.getOutputStream();
		int i = 42;
		os.write(i);
		os.write(i+1);
		int o = is.read();
		assertTrue(i == o);
		o = is.read();
		assertTrue(i + 1 == o);
	}

}
