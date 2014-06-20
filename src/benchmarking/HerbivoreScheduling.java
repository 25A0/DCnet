package benchmarking;

import java.util.Random;
import java.util.Arrays;

public class HerbivoreScheduling {
	private Random r;
	private int c;
	private int s;
	private double a;
	
	// Statistical data
	public long[] sentBits;
	public boolean[] hasSent;

	private StatisticsTracker tracker;

	public HerbivoreScheduling(int numClients, double activity, StatisticsTracker tracker) {
		this.c = numClients;
		this.a = activity;
		this.tracker = tracker;

		// The size of the scheduling vector follows the estimations
		// provided in the Herbivore paper.
		this.s = 1024* (int) ((double) c * a);

		sentBits = new long[c];

		hasSent = new boolean[c];
		Arrays.fill(hasSent, false);

		r = new Random();
	}

	public void schedule() {
		
		// Clients choose the slot in which they want to send
		int[] choices = new int[c];
		for(int i = 0; i < c; i++) {
			if(r.nextDouble() < a) {
				choices[i] = r.nextInt(s);
				sentBits[i] += s;
			} else {
				choices[i] = -1;
				continue;
			}
		}
		
		int collisions = numCollisions(choices);
		for(int i = 0; i < c; i++) {
			if(choices[i] != -1) {
				long bytes = sentBits[i];
				if(bytes%8 != 0) bytes += (8 - (bytes % 8));
				bytes >>= 3;
				tracker.reportReservation(bytes);
				hasSent[i] = true;
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

	public boolean finished() {
		for(boolean b: hasSent) {
			if(!b) return false;
		}
		return true;
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
				choices[i] = -1;
			} else {
				slots[choice] = i;
			}
		}
		return collisions;
	}
}