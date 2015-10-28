package dc.scheduling;

import java.util.Arrays;
import java.util.Random;
import java.util.InputMismatchException;

import cli.Debugger;
import dc.DCPackage;

public class FingerprintScheduler implements Scheduler {
	// The current fingerprint that is used to identify the slot that we reserved
	private int fingerprint;
	// The number of bytes that are used in the schedule for each slot
	private final int bitsPerSlot = 2;
	// The slot that we desire to reserve
	private int desiredSlot;
	// The slot that we <b> successfully </b> reserved
	private int chosenSlot;
	// The number of slots in the schedule. This equals the size of the scheduling phase.
	private final int numSlots = 32;
	
	// The likelihood that we withdraw our reservation attempt if we encounter a collision
	// default is 0.5
	private static final double WITHDRAW_CHANCE = 0.5;

	// An instance of random for choice dependent operations
	private final Random random;

	/**
	 * 
	 * @param  numSlots   The number of slots in a schedule. This influences the size of the scheduling block as well as the duration of a scheduling phase.
	 */
	public FingerprintScheduler() {
		desiredSlot = (int) (Math.random() * (double) numSlots);
		Debugger.println("scheduling", "[FingerprintScheduler] Attempting to reserve slot \t" + desiredSlot);
		chosenSlot = -1;
		random = new Random();
		refreshFingerprint();
	}

	
	@Override
	public boolean addPackage(DCPackage p, boolean waiting) {
		if(p.getNumberRange() != numSlots) {
			throw new InputMismatchException("[FingerprintScheduler] The numberRange is " + p.getNumberRange() + " but the number of slots is " + numSlots+".");
		}
		
		// If we do not want to reserve a slot, then the desired 
		// slot is set to the sentinel value -1. This will cancel 
		// scheduling for the ongoing cycle, since clients can not
		// re-enter scheduling once they've left.
		if(!waiting) desiredSlot = -1;

		byte[] schedule = p.getSchedule(getScheduleSize());
		boolean hasCollision = hasCollision(schedule);
		refreshFingerprint();
		if(p.getNumber() == numSlots -1) {
			// This is the last round of this schedule. If there is no collision then we have found a round for the upcoming phase
			if(!hasCollision) {
				chosenSlot = desiredSlot;
				Debugger.println("scheduling", "[FingerprintScheduler] Succesfully reserved \t" + desiredSlot);
			} else {
				chosenSlot = -1;
				Debugger.println("scheduling", "[FingerprintScheduler] Failed at reserving \t" + desiredSlot);
			}
			// Pick a different slot for the next round
			desiredSlot = (int) (Math.random() * (double) numSlots);
			Debugger.println("scheduling", "[FingerprintScheduler] Attempting to reserve slot \t" + desiredSlot);
			// No matter if we succeeded or not, there is a new round number
			return true;
		} else {
			if(hasCollision) {
				
				if(withdraw()) {
					// We try to move to a different slot.
					// If there are free slots left, then we'll move to one of them.
					// Otherwise pickFree will return -1, indicating that we don't attempt to reserve a slot any longer.
					desiredSlot = pickFree(schedule);
					Debugger.println("scheduling", "[FingerprintScheduler] Moved to free slot \t" + desiredSlot);
				} else {
					// We simply stick to our current slot.
					Debugger.println("scheduling", "[FingerprintScheduler] Sticking to slot \t" + desiredSlot + " although collision");
				}
			} else {
				// If there's no collision then we simply stick to our current desired slot
				Debugger.println("scheduling", "[FingerprintScheduler] Sticking to slot \t" + desiredSlot);
			}
			// Return false since this was not the last round of the current phase.
			return false;
		}
	}

	@Override
	public int getScheduleSize() {
		return (numSlots * bitsPerSlot) / 8;
	}

	@Override
	public byte[] getSchedule() {
		byte[] schedule = new byte[getScheduleSize()];
		if(desiredSlot != -1) {
			setSlot(schedule, desiredSlot, fingerprint);
		}
		return schedule;
	}

	@Override
	public int getNextRound() {
		return chosenSlot;
	}

	/**
	 * Choses a new, randomly generated fingerprint
	 */
	private void refreshFingerprint() {
		fingerprint = random.nextInt(1 << (bitsPerSlot));
	}

	/**
	 * Checks if the given schedule contains scheduling collisions.
	 * In order to check that, we compare the desired slot with the current
	 * fingerprint.
	 * @param  schedule The schedule that is checked for collisions
	 * @return          False if we do not want to reserve a slot, or if the desired slot only contains our current fingerprint.
	 */
	private boolean hasCollision(byte[] schedule) {
		if(desiredSlot == -1) return false;
		int content = extractSlot(schedule, desiredSlot);
		return content != fingerprint;
	}

	/**
	 * Extracts the value of a specific slot of a given schedule.
	 * The value is stored in little-endian manner.
	 * @param  schedule The schedule to extract a value from.
	 * @param  slot     The slot that to be extracted.
	 * @return          The value in the given slot.
	 */
	private int extractSlot(byte[] schedule, int slot) {
		// since 8 is divisible by bitsPerSlot, we know that
		// one slot will never span more than one byte.

		// relevant byte:
		byte b = schedule[(slot * bitsPerSlot) / 8];
		int startBit = (slot * bitsPerSlot) % 8;
		b >>= startBit;
		b &= (1 << bitsPerSlot) - 1;
		return (int) b;
	}

	/**
	 * Stores a specific value in a schedule
	 * @param schedule The schedule to store the value in.
	 * @param slot     The index of the slot in which the value will be stored.
	 * @param value    The value to be stored.
	 */
	private void setSlot(byte[] schedule, int slot, int value) {
		int startBit = (slot * bitsPerSlot) % 8;
		schedule[slot] |= (byte) value << startBit;
	}

	/**
	 * Picks a random free slot in a given schedule
	 * @param  schedule The schedule to be scanned
	 * @return          The index of a random free slot, or -1 if there are no free slots.
	 */
	private int pickFree(byte[] schedule) {
		int[] freeSlots = new int[numSlots];
		int numFrees = 0;
		for(int i = 0; i < numSlots; i++) {
			if(extractSlot(schedule, i) == 0) {
				freeSlots[numFrees] = i;
				numFrees++;
			}
		}
		if(numFrees == 0) {
			return -1;
		} else {
			int i = random.nextInt(numFrees);
			return freeSlots[i];
		}
	}

	/**
	 * Returns whether or not to withdraw the reservation
	 * attempt. The outcome of this function depends on the value
	 * WITHDRAW_CHANCE. 
	 */
	private boolean withdraw() {
		double value = random.nextDouble();
		return value < WITHDRAW_CHANCE;
	}

}