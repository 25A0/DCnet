package benchmarking;

import java.util.Random;
import java.util.Arrays;

public class PfitzmanScheduling {
	private Random r;
	private int c;
	private int s;
	private int b;

	// Statistical data
	public boolean succeeded;
	public int requiredRounds;
	public int emptySlots;



	public PfitzmanScheduling(int numClients) {
		this.c = numClients;
		this.s = numSlots;
		this.b = numBits;
		r = new Random();
	}

	public void schedule() {
		if(b > 8) {
			System.out.println("More than 8 bits per slot are not supported");
			return;
		}

		// A convenient schedule array
		byte[] schedule = new byte[s];

		// An array for the clients to store their fingerprints in
		byte[] fingerprints = new byte[c];

		// Clients choose the slot in which they want to send
		// and initial fingerprints are chosen
		int[] choices = new int[c];
		for(int i = 0; i < c; i++) {
			choices[i] = r.nextInt(s);
			// choices[i] = -1;
			fingerprints[i] = getFingerprint(b);
			schedule[choices[i]] ^= fingerprints[i];
		}
		
		for (int round = 0; round < s; round++) {	
			// System.out.println("Schedule is: " + Arrays.toString(schedule));
			// System.out.println("Choices are: " + Arrays.toString(choices));
			byte[] nextSchedule = new byte[s];
			for (int cl = 0; cl < c; cl++) {
				int choice = choose(schedule, choices[cl], fingerprints[cl], round == s - 1);
				fingerprints[cl] = getFingerprint(b);
				if(choice != -1) {
					nextSchedule[choice] ^= fingerprints[cl];
				}
				choices[cl] = choice;
			}
			schedule = nextSchedule;
			if(!hasCollision(choices)) {
				if(!succeeded) {
					// update # required rounds:
					requiredRounds = round + 1;
				}
				succeeded = true;
			} else {
				requiredRounds = round + 1;
				succeeded = false;
			}
		}

		emptySlots = 0;
		for(int i = 0; i < s; i++) {
			if(schedule[i] == 0) {
				emptySlots++;
			}
		}
	}

	private int choose(byte[] schedule, int lastChoice, byte fingerprint, boolean lastRound) {
		// A client that is already in withdrawed state will not
		// attempt to re-enter the scheduling
		if(lastChoice == -1) {
			return -1;
		} else if(schedule[lastChoice] == fingerprint) {
			// If there's no collision, we just stay in this slot
			return lastChoice;
		} else if(lastRound) {
			// There was a collision and there's no time left to try more things
			return -1;
		} else {
			// There was a collision
			// 
			// Determine the free slots
			int numFree = 0;
			int[] freeSlots = new int[schedule.length];
			for(int i =0; i < schedule.length; i++) {
				if(schedule[i] == 0) {
					// Keep that slot in mind
					freeSlots[numFree] = i;
					numFree++;
				}
			}
			if(r.nextDouble() < 0.5d) {
				// 50% chance that we stay, in the hope that others move away from
				// our slot, otherwise we withdraw.
				if(numFree == 0) {
					return -1;
				} else {
					// We choose a random free slot
					return freeSlots[r.nextInt(numFree)];
				}
			} else {
				return lastChoice;
			}
		}
	}

	private byte getFingerprint(int b) {
		byte f = (byte) (r.nextInt((1 << b) - 1) + 1);
		return f;
	}

	private boolean hasCollision(int[] choice) {
		for(int i = 0; i < choice.length; i++) {
			if(choice[i] == -1) continue;
			for(int j = i + 1; j < choice.length; j++) {
				if(choice[i] == choice[j]) return true;
			}
		}
		return false;
	}
}