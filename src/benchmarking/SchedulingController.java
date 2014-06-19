package benchmarking;

import cli.CLC;
import cli.ArgSet;


import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Arrays;

public class SchedulingController extends CLC {

	public SchedulingController() {

		Action fingerprintAction = new Action() {
			private int numSlots, numBits;
			private double activity;
			private boolean stopOnConvergence;
			private int avgs;

			@Override
			public void execute(ArgSet args) {
				numSlots = args.fetchInteger();
				numBits = args.fetchInteger();
				Integer[] clients;
				try {
					ArgSet cas = args.fetchList();
					ArrayList<Integer> cl = new ArrayList<Integer>();
					while(cas.hasIntArg()) {
						cl.add(cas.fetchInteger());
					}
					clients = cl.toArray(new Integer[cl.size()]);
				} catch(InputMismatchException e) {
					clients = new Integer[] {1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000};
				}
				if(args.hasArg()) {
					activity = Double.valueOf(args.pop());
					if(args.hasArg()) {
						stopOnConvergence = Boolean.valueOf(args.pop());
					} else {
						stopOnConvergence = false;
					}
				} else {
					activity = 1.0;
					stopOnConvergence = false;
				}
				
				int cc = clients.length;
				double[] bytesPerReservation = new double[cc];
				double[] collisions = new double[cc];
				double[] requiredRounds = new double[cc];
				double[] emptySlots = new double[cc];
				double[] coverage = new double[cc];
				System.out.println("Executing FINGERPRINT SCHEDULING benchmark for " +  numSlots + " slots and " + numBits + " bits per slot, " + activity*100 +"% client activity" + (stopOnConvergence?", stopping on convergence":""));
				for(int i = 0; i < cc; i++) {
					System.out.print("["+(i+1)+"/"+cc+"]\t" + clients[i] + " client(s)\t" + numSlots + " slots\t");
					StatisticsTracker tracker = benchmark(clients[i]);
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

			private StatisticsTracker benchmark(int clients) {
				StatisticsTracker tracker = new StatisticsTracker();
				FingerprintScheduling s = new FingerprintScheduling(clients, numSlots, numBits, activity, stopOnConvergence, tracker);
				int steps = (clients/100) + 1;
				avgs = 0;
				do {
					s.schedule();
					if(avgs%steps == 0) System.out.print(".");
					avgs++;
				} while(!s.finished());
				return tracker;
			}
		};

		Action pfitzmannAction = new Action() {
			private int numSlots;
			private double activity;
			private int avgs;

			@Override
			public void execute(ArgSet args) {
				numSlots = args.fetchInteger();
				activity = Double.valueOf(args.pop());

				Integer[] clients;
				try {
					ArgSet cas = args.fetchList();
					ArrayList<Integer> cl = new ArrayList<Integer>();
					while(cas.hasIntArg()) {
						cl.add(cas.fetchInteger());
					}
					clients = cl.toArray(new Integer[cl.size()]);
				} catch(InputMismatchException e) {
					clients = new Integer[] {1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000};
				}
				int cc = clients.length;
				double[] bytesPerReservation = new double[cc];
				double[] coverage = new double[cc];
				System.out.println("Executing PFITZMANN'S ALGORITHM benchmark for " + numSlots + " slots per client, " + activity*100 +"% client activity");
				for(int i = 0; i < cc; i++) {
					System.out.print("["+(i+1)+"/"+cc+"]\t" + clients[i] + " client(s)\t" + clients[i]*numSlots + " slots\t");
					StatisticsTracker tracker = benchmark(clients[i]);
					bytesPerReservation[i] = tracker.getAverageBytesPerReservation();
					coverage[i] = tracker.getCoverage();
					System.out.println("\t [DONE]");
				}

				System.out.println(" DONE");
				System.out.println("Clients: \n" + Arrays.toString(clients));
				System.out.println("Sent bytes per reservation:\n" + Arrays.toString(bytesPerReservation));
				System.out.println("Coverage:\n" + Arrays.toString(coverage));
			}

			private StatisticsTracker benchmark(int clients) {
				StatisticsTracker tracker = new StatisticsTracker();
				PfitzmannScheduling s = new PfitzmannScheduling(clients, clients * numSlots, activity, tracker);
				int steps = (clients/100) + 1;
				do {
					s.schedule();
					if(avgs%steps == 0) System.out.print(".");
					avgs++;
				} while(!s.finished());
				return tracker;
			}
		};

		mapCommand("fingerprint", fingerprintAction);
		mapCommand("pfitzmann", pfitzmannAction);
	}

}