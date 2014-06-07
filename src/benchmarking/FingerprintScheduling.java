package benchmarking;

import java.util.Random;
import java.util.Arrays;

public class FingerprintScheduling {
	private Random r;
	private int c;
	private int s;
	private int b;
	private double a;

	// Statistical data
	public long[] sentBits;
	public boolean[] hasSent;

	private StatisticsTracker tracker;

	// The chance that a station moves to a different slot in
	// case that the chosen slot appears to be occupied
	private double chance = 0.5;

	public FingerprintScheduling(int numClients, int numSlots, int numBits, double activity, StatisticsTracker tracker) {
		this.c = numClients;
		this.s = numSlots;
		this.b = numBits;
		this.a = activity;
		this.tracker = tracker;
		sentBits = new long[c];

		hasSent = new boolean[c];
		Arrays.fill(hasSent, false);

		r = new Random();
	}

	public void schedule() {
		if(b > 8) {
			System.out.println("More than 8 bits per slot are not supported");
			return;
		}
		boolean succeeded = false;
		int requiredRounds = s;

		// A convenient schedule array
		byte[] schedule = new byte[s];

		// An array for the clients to store their fingerprints in
		byte[] fingerprints = new byte[c];

		// Clients choose the slot in which they want to send
		// and initial fingerprints are chosen
		int[] choices = new int[c];
		for(int i = 0; i < c; i++) {
			if(r.nextDouble() < a) {
				choices[i] = r.nextInt(s);
			} else {
				choices[i] = -2;
				continue;
			}
			fingerprints[i] = getFingerprint(b);
			schedule[choices[i]] ^= fingerprints[i];
		}
		
		for (int round = 0; round < s; round++) {	
			// System.out.println("Schedule is: " + Arrays.toString(schedule));
			// System.out.println("Choices are: " + Arrays.toString(choices));
			byte[] nextSchedule = new byte[s];
			for (int cl = 0; cl < c; cl++) {
				if(choices[cl] == -2) continue;
				int choice = choose(schedule, choices[cl], fingerprints[cl], round == s - 1);
				fingerprints[cl] = getFingerprint(b);
				if(choice != -1) {
					nextSchedule[choice] ^= fingerprints[cl];
				}
				sentBits[cl] += s * b;
				choices[cl] = choice;
			}
			schedule = nextSchedule;
			int collisions = numCollisions(choices);
			if(collisions == 0) {
				if(!succeeded) {
					// update # required rounds:
					requiredRounds = round + 1;
				}
				succeeded = true;
			} else {
				requiredRounds = s;
				succeeded = false;
			}
		}

		int emptySlots = 0;
		for(int i = 0; i < s; i++) {
			if(schedule[i] == 0) {
				emptySlots++;
			}
		}
		tracker.reportFreeSlots(emptySlots);
		tracker.reportCollisions(numCollisions(choices));
		tracker.reportRequiredRounds(requiredRounds);
		if (succeeded) {
			for(int i = 0; i < c; i++) {
				if(choices[i] >= 0) {
					long bytes = (sentBits[i] >> 3) + ((sentBits[i]%8 == 0)?0:1);
					tracker.reportReservation(bytes);
					sentBits[i] = 0;
					hasSent[i] = true;
				}
			}
		}
		int coverage = 0;
		for(int i = 0; i < c; i++) {
			if(hasSent[i]) {
				coverage++;
			}
		}
		tracker.reportCoverage(coverage, c);
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
			if(withdraw()) {
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

	private boolean withdraw() {
		return r.nextDouble() < chance;
	}

	private byte getFingerprint(int b) {
		byte f = (byte) (r.nextInt((1 << b) - 1) + 1);
		return f;
	}

	private int numCollisions(int[] choices) {
		int collisions = 0;
		int[] slots = new int[s];
		Arrays.fill(slots, -1);
		for(int i = 0; i < c; i++) {
			int choice = choices[i];
			if(choice < 0) continue;
			if(slots[choice] != -1) {
				collisions++;
			} else {
				slots[choice] = i;
			}
		}
		return collisions;
	}
}