package utest;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import dc.DCPackage;
import dc.testing.DummyConnection;


public class CommunicationTest {


	@Test
	public void testMessages() {
		
		byte[] payload = new byte[DCPackage.PAYLOAD_SIZE];
		for(int i = 0; i < DCPackage.PAYLOAD_SIZE; i++) {
			payload[i] = (byte) (Math.random() * 256d);
		}
		DCPackage m = new DCPackage((byte) 0, payload);
		assertArrayEquals(payload, m.getPayload());

		byte[] bb = m.toByteArray();
		DCPackage m2 = DCPackage.getPackage(bb);
		assertArrayEquals(payload, m2.getPayload());
		
	}
	
//	@Test
//	public void testDummyCommunication() throws IOException {
//		DummyConnection dc = new DummyConnection();
//		InputStream is = dc.getInputStream();
//		OutputStream os = dc.getOutputStream();
//		int i = 42;
//		os.write(i);
//		os.write(i+1);
//		int o = is.read();
//		assertTrue(i == o);
//		o = is.read();
//		assertTrue(i + 1 == o);
//	}

}
