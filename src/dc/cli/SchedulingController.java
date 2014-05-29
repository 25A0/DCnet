package dc.cli;

import cli.CLC;
import cli.ArgSet;
import dc.testing.Scheduling;

import java.util.Arrays;

public class SchedulingController extends CLC {

	public SchedulingController() {
		Action scheduleAction = new Action() {
			@Override
			public void execute(ArgSet args) {
				int numClients = args.fetchInteger();
				int numSlots = args.fetchInteger();
				int numBits = args.fetchInteger();
				Scheduling s = new Scheduling(numClients, numSlots, numBits);
				s.schedule();
				System.out.println("Scheduling succeeded: " + s.succeeded);
				System.out.println("Required rounds: " + s.requiredRounds);
				System.out.println("Unused slots: " + s.emptySlots);
			}
		};

		Action benchmarkAction = new Action() {
			private int maxClients, numSlots, numBits;
			private final int steps = 100;
			private int factor;

			@Override
			public void execute(ArgSet args) {
				int avgs = args.fetchInteger();
				maxClients = args.fetchInteger();
				numSlots = args.fetchInteger();
				numBits = args.fetchInteger();
				factor = maxClients / steps;
				int clients[][] = new int[avgs][steps];
				int succeeded[][] = new int[avgs][steps];
				int requiredRounds[][] = new int[avgs][steps];
				int emptySlots[][] = new int[avgs][steps];
				System.out.println("Executing benchmark for " + maxClients + " clients, " + numSlots + " slots and " + numBits + " bits per slot");
				for(int i = 0; i < avgs; i++) {
					System.out.print("["+(i+1)+"/"+(avgs)+"]\t");
					benchmark(clients[i], succeeded[i], requiredRounds[i], emptySlots[i]);
					System.out.println(" [DONE]");
				}

				System.out.println(" DONE");
				System.out.println("Clients: \n" + Arrays.toString(avg(avgs, clients)));
				System.out.println("Success vector: \n" + Arrays.toString(avg(avgs, succeeded)));
				System.out.println("Required rounds vector: \n" + Arrays.toString(avg(avgs, requiredRounds)));
				System.out.println("Empty slots vector: \n" + Arrays.toString(avg(avgs, emptySlots)));
			}

			private void benchmark(int[] clients, int[] succeeded, int[] requiredRounds, int[] emptySlots) {
				for(int i = 1; i < steps; i++) {
					// Scheduling s = new Scheduling(i*factor, numSlots, numBits);
					Scheduling s = new Scheduling(maxClients, numSlots, numBits);
					s.schedule();
					clients[i] = i*factor;
					succeeded[i] = s.succeeded? 1 : 0;
					requiredRounds[i] = s.requiredRounds;
					emptySlots[i] = s.emptySlots;
					System.out.print(".");
				}

			}

			private double[] avg(int avgs, int[][] data) {
				int l = data[0].length;
				double[] result = new double[l];
				for(int i = 0; i < avgs; i++) {
					for(int j = 0; j < l; j++) {
						result[j] += data[i][j];
					}
				}
				for(int i = 0; i < l; i++) {
					result[i] = result[i] / (double) avgs;
				}
				return result;
			}
		};

		mapCommand("schedule", scheduleAction);
		mapCommand("benchmark", benchmarkAction);
	}

}