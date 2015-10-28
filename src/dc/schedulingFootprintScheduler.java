package dc.scheduling;

import java.util.Arrays;
import java.util.Random;
import java.util.InputMismatchException;

import cli.Debugger;
import dc.DCPackage;

public class FingerprintScheduler implements Scheduler {
	// The number of bytes that are used in the schedule for each slot
	private final int bitsPerSlot = 2;
	// Holds for each slots whether we want to reserve it
	private boolean[] desiredSlots;
	// Holds for each slot whether we did successfully reserve it
	private boolean[] chosenSlots;
	// The number of slots in the schedule. This equals the size of the scheduling phase.
	private final int numSlots = 32;
	// The number of discussion rounds. This will change over time, depending on the network size.
	private final int numRounds;
	// The current fingerprint that is used to identify the slot that we reserved
	private final byte[] fingerprints = new byte[numSlots];
	
	// The likelihood that we withdraw our reservation attempt if we encounter a collision
	// default is 0.7
	private static final double WITHDRAW_CHANCE = 0.7;
	// The likelihood that we attempt to move to a different slot, rather than staying in the same slot
	// default is 0.5
	private static final double MOVE_CHANCE = 0.5;


	// An instance of random for choice dependent operations
	private final Random random;

	// An index that acts as an iterator over the array of chosen slots
	private int index = 0;
	// The current number of reserved slots that have not yet been used
	private int poolSize = 0;

	/**
	 * 
	 * @param  numSlots   The number of slots in a schedule. This influences the size of the scheduling block as well as the duration of a scheduling phase.
	 */
	public FingerprintScheduler(int numRounds) {
		this.numRounds = numRounds;
		desiredSlots = new boolean[numSlots];
		Arrays.fill(desiredSlots, true);
		chosenSlots = new boolean[numSlots];
		random = new Random();
		refreshFingerprints();
	}

	
	@Override
	public boolean addPackage(DCPackage p, boolean waiting) {
		if(p.getNumberRange() != numRounds) {
			throw new InputMismatchException("[FingerprintScheduler] The numberRange is " + p.getNumberRange() +
				" but the number of slots is " + numSlots+".");
		}
		
		// If we do not want to reserve a slot, then the desired 
		// slot is set to the sentinel value -1. This will cancel 
		// scheduling for the ongoing cycle, since clients can not
		// re-enter scheduling once they've left.
		if(!waiting) {
			Arrays.fill(desiredSlots, false);
		}

		// in the last round, we need to reset the array of desired slots
		// and scrap all reserved slots that have not been used.
		if(p.getNumber() == numRounds - 1) {
			Arrays.fill(chosenSlots, false);
			poolSize = 0;
			index = 0;
		}

		byte[] schedule = p.getSchedule(getScheduleSize());
		for (int i = 0; i < numSlots; i++) {
			if(!desiredSlots[i]) continue;
			boolean hasCollision = hasCollision(schedule, i);
			refreshFingerprint(i);
			if(p.getNumber() == numRounds -1) {
				// This is the last round of this schedule.
				// If there is no collision then we have found a round for the upcoming phase
				if(!hasCollision) {
					chosenSlots[i] = true;
					// Increase pool size since we found another slot that we can use
					poolSize++;
					// System.out.println("scheduling: [FingerprintScheduler] Succesfully reserved \t" + i);
				} else {
					// no need to change chosenSlots; values of a boolean array default to false.
					Debugger.println("scheduling", "[FingerprintScheduler] Failed at reserving \t" + i);
				}
			} else {
				if(hasCollision) {
					
					if(withdraw()) {
						desiredSlots[i] = false;
						Debugger.println("scheduling", "Backed off from slot \t" + i);
					} else {
						// We don't back off right away, so we either move or stay in the same slot.
						if(move()) {
							// We try to move to a different slot.
							// If there are free slots left, then we'll move to one of them.
							// Otherwise pickFree will return -1,
							// indicating that we don't attempt to reserve a slot any longer.
							int desiredSlot = pickFree(schedule);
							// Remember that we don't try to reserve i any longer.
							desiredSlots[i] = false;
							if(desiredSlot != -1) {
								desiredSlots[desiredSlot] = true;
								Debugger.println("scheduling", "[FingerprintScheduler] Moved to free slot \t" +
									desiredSlot);
							} else {
								// Otherwise there are no free slots, which comes down to simply backing off.
								Debugger.println("scheduling", "Backed off from slot \t" + i +
									" after attempting to move");
							}
						} else {
							// We simply stick to our current slot.
							Debugger.println("scheduling", "[FingerprintScheduler] Sticking to slot \t" + i +
								" despite collision");
						}
					}
				} else {
					// If there's no collision then we simply stick to our current desired slot
					Debugger.println("scheduling", "[FingerprintScheduler] Sticking to slot \t" + i);
				}
			}
		}

		// In the last round, we need to mark all slots as desired for the following cycle:
		if(p.getNumber() == numRounds -1) {
			Arrays.fill(desiredSlots, true);
		}
		return poolSize > 0;
	}

	@Override
	public int getScheduleSize() {
		return (numSlots * bitsPerSlot) / 8;
	}

	@Override
	public byte[] getSchedule() {
		byte[] schedule = new byte[getScheduleSize()];
		for (int i = 0; i < numSlots; i++) {
			if(desiredSlots[i]) {
				setSlot(schedule, i, fingerprints[i]);
			}
		}
		return schedule;
	}

	@Override
	public int getNextRound() {
		// skip to the next reserved slot
		// System.out.println("Starting search at index " + index);
		for(; index < numSlots && !chosenSlots[index]; index++);
		if(index < numSlots) {
			poolSize--;
			// System.out.println("Providing slot " + index);
			int slot = index;
			index++;
			return slot;
		} else {
			// System.out.println("no free slot");
			return -1;
		}
	}

	/**
	 * Choses a new, randomly generated fingerprint
	 */
	private void refreshFingerprints() {
		for (int i = 0; i < numSlots; i++) {
			refreshFingerprint(i);
		}
	}

	private void refreshFingerprint(int slot) {
		fingerprints[slot] = (byte) (random.nextInt(1 << (bitsPerSlot) - 1) + 1);	
	}

	/**
	 * Checks if the given schedule contains scheduling collisions.
	 * In order to check that, we compare the desired slot with the current
	 * fingerprint.
	 * @param  schedule The schedule that is checked for collisions
	 * @return          False if we do not want to reserve a slot, or if the desired slot only contains our current fingerprint.
	 */
	private boolean hasCollision(byte[] schedule, int slot) {
		int content = extractSlot(schedule, slot);
		return content != fingerprints[slot];
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
	private void setSlot(byte[] schedule, int slot, byte value) {
		int index = (slot * bitsPerSlot) / 8;
		int startBit = (slot * bitsPerSlot) % 8;
		schedule[index] |= value << startBit;
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
			if(extractSlot(schedule, i) == 0 && !desiredSlots[i]) {
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

	/**
	 * Returns whether or not we move to a different slot after a collision was detected.
	 * The outcome of this function depends on the value of MOVE_CHANCE. 
	 */
	private boolean move() {
		double value = random.nextDouble();
		return value < MOVE_CHANCE;
	}

}