package benchmarking;

import java.util.Random;
import java.util.Arrays;

public class PfitzmannScheduling {
	private Random r;
	// The number of clients
	private int c;
	// The number of slots
	private int s;
	// The size of one message
	private int msgSize;
	// The slot that each client chose
	private long[] choices;
	private long[] schedule;

	// Statistical data
	public long[] sentBytes;

	public boolean[] hasSent;
	private StatisticsTracker tracker;



	public PfitzmannScheduling(int numClients, int numSlots, StatisticsTracker tracker) {
		this.c = numClients;
		this.s = numSlots;
		this.tracker = tracker;
		sentBytes = new long[c];
		// Calculate size of each message
		msgSize = 2;
		int alphabet = c * s;
		while(alphabet > 0) {
			alphabet >>=1;
			msgSize++;
		}
		int countSize = c;
		while(countSize > 0) {
			countSize >>=1;
			msgSize++;
		}
		// Round up to full bytes
		msgSize += (8-(msgSize%8));
		msgSize >>= 3;

		hasSent = new boolean[c];
		Arrays.fill(hasSent, false);

		r = new Random();
	}

	public void schedule() {
		choices = new long[c];
		schedule = new long[s];
		
		for(int i = 0; i < c; i++) {
			// Add 1 to avoid that stations schedule slot 0
			choices[i] = (long) r.nextInt(s) + 1;
		}
		
		long[] outcome = send(s);
		deduce(outcome[0], outcome[1]);

		for(int i = 0; i < c; i++) {
			if(schedule[(int) choices[i]-1]==1) {
				tracker.reportReservation(sentBytes[i]);
				sentBytes[i] = 0;
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

	private void deduce(long sum, long count) {
		if(count == 1) {
			schedule[(int) sum-1]++;
			return;
		} else {
			long avg = sum / count;
			long[] outcome = send(avg);
			long newSum = outcome[0];
			long newCount = outcome[1];
			if(newSum == sum && newCount == count) {
				assert(sum % count == 0);
				long slot = sum/count;
				schedule[(int) slot-1] += count;
			} else {
				deduce(newSum, newCount);
				sum -= newSum;
				count -= newCount;
				deduce(sum, count);
			}
		}
	}

	private long[] send(long avgThreshold) {
		long sum = 0;
		long count = 0;
		for(int i = 0; i < c; i++) {
			sentBytes[i]+=msgSize;
			if(schedule[(int) choices[i] - 1 ] == 0 && choices[i] <= avgThreshold) {
				sum += choices[i];
				count++;
			}
		}
		return new long[]{sum, count};
	}
}