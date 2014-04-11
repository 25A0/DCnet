package dc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

import dc.DCPackage;
import util.Padding10;

public class MessageBuffer extends OutputStream {
		private final int payloadSize;
		private byte[] currentMessage;
		private final LinkedList<byte[]> messageBuffer;
		private int writePointer;

		public MessageBuffer() {
			messageBuffer = new LinkedList<byte[]>();
			// Subtract 1 for 10 padding
			payloadSize = DCPackage.PAYLOAD_SIZE - 1;
			currentMessage = new byte[payloadSize];
			writePointer = 0;
		}

		@Override
		public void write(int i) {
			write((byte) i);
		}
		
		public synchronized void write(byte b){
			// Debugger.println(2, "[DummyConnection] writing " + (char)b);
			currentMessage[writePointer] = b;
			writePointer++;
			if(writePointer >= payloadSize) {
				synchronized(messageBuffer) {
					messageBuffer.add(currentMessage);
				}
				currentMessage = new byte[payloadSize];
				writePointer = 0;
			}
		}

		public synchronized boolean hasMessage() {
			return messageBuffer.isEmpty();
		}
		
		public synchronized byte[] getMessage() {
			if(!messageBuffer.isEmpty()) {
				// Fetch the last message from the buffer and apply padding
				return Padding10.apply10padding(messageBuffer.poll(), DCPackage.PAYLOAD_SIZE);	
			} else if(writePointer > 0) {
				// Only in this case we have to apply padding
				synchronized(this) {
					byte[] message = Padding10.apply10padding(currentMessage, writePointer, DCPackage.PAYLOAD_SIZE);
					currentMessage = new byte[payloadSize];
					writePointer = 0;
					return message;
				}
			}
			// This catches everything that was not caught by previous conditions
			// We do not apply padding since the message is meant to be _empty_.
			return new byte[DCPackage.PAYLOAD_SIZE];
				
		}

	}