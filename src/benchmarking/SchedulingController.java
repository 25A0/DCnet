package benchmarking;

import cli.CLC;
import cli.ArgSet;

import java.util.Arrays;

public class SchedulingController extends CLC {

	public SchedulingController() {
		Action scheduleAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				int numClients = args.fetchInteger();
				int numSlots = args.fetchInteger();
				int numBits = args.fetchInteger();
				StatisticsTracker tracker = new StatisticsTracker();
				FingerprintScheduling s = new FingerprintScheduling(numClients, numSlots, numBits, tracker);
				s.schedule();
				System.out.println("Collisions: " + tracker.getAverageCollisions(1));
				System.out.println("Required rounds: " + tracker.getAverageRequiredRounds(1));
				System.out.println("Unused slots: " + tracker.getAverageFreeSlots(1));
				System.out.println("Sent bytes: " + tracker.getAverageBytesPerReservation());
			}
		};

		Action fingerprintAction = new Action() {
			private int numSlots, numBits;

			@Override
			public void execute(ArgSet args) {
				int avgs = args.fetchInteger();
				numSlots = args.fetchInteger();
				numBits = args.fetchInteger();
				
				int[] clients = new int[] {1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000};
				int cc = clients.length;
				double[] bytesPerReservation = new double[cc];
				double[] collisions = new double[cc];
				double[] requiredRounds = new double[cc];
				double[] emptySlots = new double[cc];
				double[] coverage = new double[cc];
				System.out.println("Executing benchmark for " +  numSlots + " slots and " + numBits + " bits per slot");
				for(int i = 0; i < cc; i++) {
					System.out.print("["+(i+1)+"/"+cc+"]\t" + clients[i] + " client(s)\t" + clients[i] + " slots\t");
					StatisticsTracker tracker = benchmark(clients[i], avgs);
					bytesPerReservation[i] = tracker.getAverageBytesPerReservation();
					collisions[i] = tracker.getAverageCollisions(avgs);
					requiredRounds[i] = tracker.getAverageRequiredRounds(avgs);
					emptySlots[i] = tracker.getAverageFreeSlots(avgs);
					coverage[i] = tracker.getCoverage();
					System.out.println("\t [DONE]");
				}

				System.out.println(" DONE");
				System.out.println("Clients: \n" + Arrays.toString(clients));
				System.out.println("Sent bytes per reservation:\n" + Arrays.toString(bytesPerReservation));
				System.out.println("Average collisions: \n" + Arrays.toString(collisions));
				System.out.println("Required rounds: \n" + Arrays.toString(requiredRounds));
				System.out.println("Empty slots: \n" + Arrays.toString(emptySlots));
				System.out.println("Coverage:\n" + Arrays.toString(coverage));
			}

			private StatisticsTracker benchmark(int clients, int avgs) {
				StatisticsTracker tracker = new StatisticsTracker();
				FingerprintScheduling s = new FingerprintScheduling(clients, clients, numBits, tracker);
				int steps = (avgs/100) + 1;
				for(int i = 1; i < avgs; i++) {
					s.schedule();
					if(i%steps == 0) System.out.print(".");
				}
				return tracker;
			}
		};

		Action pfitzmannAction = new Action() {
			private int numSlots, numBits;

			@Override
			public void execute(ArgSet args) {
				int avgs = args.fetchInteger();
				numSlots = args.fetchInteger();
				
				int[] clients = new int[] {1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000};
				int cc = clients.length;
				double[] bytesPerReservation = new double[cc];
				double[] coverage = new double[cc];
				// System.out.println("Executing benchmark for " + numSlots + " slots");
				for(int i = 0; i < cc; i++) {
					System.out.print("["+(i+1)+"/"+cc+"]\t" + clients[i] + " client(s)\t" + clients[i]*numSlots + " slots\t");
					StatisticsTracker tracker = benchmark(clients[i], avgs);
					bytesPerReservation[i] = tracker.getAverageBytesPerReservation();
					coverage[i] = tracker.getCoverage();
					System.out.println("\t [DONE]");
				}

				System.out.println(" DONE");
				System.out.println("Clients: \n" + Arrays.toString(clients));
				System.out.println("Sent bytes per reservation:\n" + Arrays.toString(bytesPerReservation));
				System.out.println("Coverage:\n" + Arrays.toString(coverage));
			}

			private StatisticsTracker benchmark(int clients, int avgs) {
				StatisticsTracker tracker = new StatisticsTracker();
				PfitzmannScheduling s = new PfitzmannScheduling(clients, clients * numSlots, tracker);
				int steps = (avgs/100) + 1;
				for(int i = 1; i < avgs; i++) {
					s.schedule();
					if(i%steps == 0) System.out.print(".");
				}
				return tracker;
			}
		};

		mapCommand("schedule", scheduleAction);
		mapCommand("fingerprint", fingerprintAction);
		mapCommand("pfitzmann", pfitzmannAction);
	}

}