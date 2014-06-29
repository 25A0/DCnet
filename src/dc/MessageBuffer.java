package dc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

import dc.DCPackage;
import util.Padding10;

import cli.Debugger;

/**
 * A buffer that is used by a DcClient to store messages that have yet to be sent.
 */
public class MessageBuffer extends OutputStream {
		// The size of the payload in a package
		private final int payloadSize;
		// The size of a message.
		private final int messageSize;
		// The message that was sent most recently but was not successfully delivered yet
		private byte[] pendingMessage;
		// A buffer that holds all unsent messages
		private final LinkedList<byte[]> messageBuffer;
		
		// A pointer that points to the first free byte in the current message
		private int writePointer;
		// The message that is currently build up from user input
		private byte[] currentMessage;

		private long startTime;
		private int deliveredPackages;

		/**
		 * Initializes the messageBuffer for a given message size.
		 * The message size does not need to care about padding 
		 * (i.e. if packages have a payload of 1024 bytes, set payloadSize to 1024
		 * and the MessageBuffer will reserve one byte for padding, leaving room 
		 * for 1023 byte messages)
		 * @param  payloadSize The size of the payload in bytes
		 */
		public MessageBuffer(int payloadSize) {
			messageBuffer = new LinkedList<byte[]>();
			// Subtract 1 for 10 padding
			this.payloadSize = payloadSize;
			this.messageSize = payloadSize - 1;
			currentMessage = new byte[messageSize];
			writePointer = 0;
		}

		@Override
		public void write(int i) {
			write((byte) i);
		}
		
		public synchronized void write(byte b){
			startTime = System.currentTimeMillis();
			currentMessage[writePointer] = b;
			writePointer++;
			if(writePointer >= messageSize) {
				stop();
			}
		}

		/**
		 * Concludes an ongoing input stream and pushes the collected data to the
		 * message buffer. 
		 * Conceptually similar to a full stop, hence the name.
		 */
		public void stop() {
			// Note: writePointer will be one ahead of the last message byte, so
			// we can read it as size in this case
			byte[] message = new byte[writePointer];
			for(int i = 0; i < writePointer; i++) {
				message[i] = currentMessage[i];
			}
			synchronized(messageBuffer) {
				messageBuffer.add(message);
			}
			// No need to replace currentMessage, 
			// new payload will simply replace it.
			writePointer = 0;
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
				pendingMessage = Padding10.apply10padding(messageBuffer.poll(), payloadSize);	
				return pendingMessage;
			} else if(writePointer > 0) {
				// Only in this case we have to apply padding
				synchronized(this) {
					byte[] message = Padding10.apply10padding(currentMessage, writePointer, payloadSize);
					currentMessage = new byte[messageSize];
					writePointer = 0;
					pendingMessage = message;
					return message;
				}
			}
			// This catches everything that was not caught by previous conditions
			// We do not apply padding since the message is meant to be _empty_.
			// We do not send a pending message, either.
			return new byte[payloadSize];
				
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
		 * Checks whether the underlying buffer still contains messages to be sent.
		 * Should be combined with {@code hasPendingMessage} to find out if there
		 * are still messages to be sent in general.
		 */
		public boolean isEmpty() {
			return messageBuffer.isEmpty() && writePointer == 0;
		}

		/**
		 * Confirms that the currently pending message has been succesfully delivered.
		 */
		public void confirmMessage() {
			if(pendingMessage == null) {
				throw new IllegalStateException("Message was confirmed although messageBuffer was not waiting for a confirmation.");
			} else {
				pendingMessage = null;
				deliveredPackages++;
				if(messageBuffer.isEmpty()) {
					long interval = System.currentTimeMillis() - startTime;
					Debugger.println("throughput", "Delivered " + deliveredPackages +" packages in " + interval/1000 + "."+ interval%1000 +" seconds.");
					deliveredPackages = 0;
				}
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