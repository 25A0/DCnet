package util;

import java.util.InputMismatchException;
import cli.Debugger;

public class Padding10 {

	private Padding10() {

	}

	/**
	 * Applies 10 padding to a byte array
	 * Similar to {@code apply10padding(byte[] input, int paddingStart, int goalSize)},
	 * but assumes that the complete input shall be contained in the padded output.
	 */
	public static byte[] apply10padding(byte[] input, int goalSize) {
		return apply10padding(input, input.length, goalSize);
	}

	/**
	 * Applies 10 padding to a byte array.
	 *
	 * @param  input The byte array that will be contained in the resulting array. Must be smaller than {@code goalSize}
	 * @param  paddingStart The index of the first byte of the padding part.
	 * @param  goalSize The size of the array that the input will be padded to.
	 *
	 * @return  A byte array of size {@code goalSize} that contains {@code input}, followed by a {@code 1} at index {@code paddingStart}, followed by zero or more {@code 0}'s.
	 */
	public static byte[] apply10padding(byte[] input, int paddingStart, int goalSize) {
		if(input != null && input.length +1 > goalSize) {
			throw new InputMismatchException("The provided input is too big to apply 10 padding");
		} 
		if(paddingStart > input.length || paddingStart > goalSize) {
			Debugger.println(0, "[ConnectionBundle] Warning: Cannot start padding at " + paddingStart);
			paddingStart = input.length;
		}

		byte[] output = new byte[goalSize];
		if(input != null) {
			// Note that we do not check for the bounds of input, since we checked earlier 
			// that paddingStart does not point beyond input.length
			for(int i = 0; i < paddingStart; i++) {
				output[i] = input[i];
			}
		}
		// Put the 1 down to mark the end of the payload
		output[paddingStart] = (byte) 1;
		// The remaining bytes have been initialized to 0
		return output;
		
	}

	/**
	 * Reverts 10 padding on a byte array.
	 * That means: Starting at the end, we expect to find 0's until we find a 1.
	 *
	 * @return The content of the input in front of the padding, or null if the input only contains {@code 0}'s.
	 */
	public static byte[] revert10padding(byte[] input) {
		// An empty input array doesn't contain any information.
		if(input.length == 0) {
			return null;
		}

		int paddingStart = input.length - 1;
		for(; input[paddingStart] == 0; paddingStart--) {
			if(paddingStart == 0) {
				// In this case we went through the whole array without encountering anything
				// but 0's. This does not fulfil the requirements
				return null;
			}
		}
		// Check if we found a 1 now.
		if(input[paddingStart] == 1) {
			// That fulfills the padding pattern.
			// Copy the remaining input
			byte[] output = new byte[paddingStart];
			for(int i = 0; i < paddingStart; i++) {
				output[i] = input[i];
			}
			return output;
		} else {
			throw new InputMismatchException("The provided input is malformed.");
		}
	}

}