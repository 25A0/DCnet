package dc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

import dc.DCPackage;
import util.Padding10;

/**
 * A buffer that is used by a DcClient to store messages that have yet to be sent.
 */
public class MessageBuffer extends OutputStream {
		// The size of each message
		private final int payloadSize;
		// The message that was sent most recently but was not succesfully delivered yet
		private byte[] pendingMessage;
		// A buffer that holds all unsent messages
		private final LinkedList<byte[]> messageBuffer;
		
		// A pointer that points to the first free byte in the current message
		private int writePointer;
		// The message that is currently build up from user input
		private byte[] currentMessage;

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

		/**
		 * Returns the next message to be sent. This is either a pending message that was sent earlier but wasn't delivered succesfully (e.g. because of a collision), or the content of the current message, or the first message from the message buffer.
		 * @return A byte array that holds the next message to be sent.
		 */
		public synchronized byte[] getMessage() {
			if(pendingMessage != null) {
				// Padding has already been applied earlier, so we can just return the pending message.
				return pendingMessage;
			} else if(!messageBuffer.isEmpty()) {
				// Fetch the last message from the buffer and apply padding
				pendingMessage = Padding10.apply10padding(messageBuffer.poll(), DCPackage.PAYLOAD_SIZE);	
				return pendingMessage;
			} else if(writePointer > 0) {
				// Only in this case we have to apply padding
				synchronized(this) {
					byte[] message = Padding10.apply10padding(currentMessage, writePointer, DCPackage.PAYLOAD_SIZE);
					currentMessage = new byte[payloadSize];
					writePointer = 0;
					pendingMessage = message;
					return message;
				}
			}
			// This catches everything that was not caught by previous conditions
			// We do not apply padding since the message is meant to be _empty_.
			// We do not send a pending message, either.
			return new byte[DCPackage.PAYLOAD_SIZE];
				
		}

		/**
		 * Checks whether there is a pending message in the message buffer.
		 * This is the case if we sent a message earlier that didn't return properly as
		 * output of a round yet.
		 * @return true if there is a message
		 */
		public boolean hasPendingMessage() {
			return pendingMessage != null;
		}

		/**
		 * Confirms that the currently pending message has been succesfully delivered.
		 */
		public void confirmMessage() {
			if(pendingMessage == null) {
				throw new IllegalStateException("Message was confirmed although messageBuffer was not waiting for a confirmation.");
			} else {
				pendingMessage = null;
			}
		}

		/**
		 * Compares a given message with the currently pending message to check for collision artifacts.
		 * @param  message The outcome of the current round that is supposed to equal the pending message
		 * @return         true if the given message equals the pending message, false otherwise.
		 */
		public boolean compareMessage(byte[] message) {
			if(pendingMessage == null) {
				throw new IllegalStateException("There is no pending message to compare the given message with");
			}
			if(message.length != pendingMessage.length) {
				return false;
			} else {
				for(int i = 0; i < pendingMessage.length; i++) {
					if(pendingMessage[i] != message[i]) return false;
				}
				return true;
			}
		}

	}