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
			private int maxClients, numSlots, numBits, samples;
			private final int factor = 10;

			@Override
			public void execute(ArgSet args) {
				int avgs = args.fetchInteger();
				maxClients = args.fetchInteger();
				numSlots = args.fetchInteger();
				numBits = args.fetchInteger();
				samples = maxClients / factor;
				int clients[][] = new int[avgs][samples];
				int succeeded[][] = new int[avgs][samples];
				int requiredRounds[][] = new int[avgs][samples];
				int emptySlots[][] = new int[avgs][samples];
				for(int i = 0; i < avgs; i++) {
					benchmark(clients[i], succeeded[i], requiredRounds[i], emptySlots[i]);
				}

				System.out.println(" DONE");
				System.out.println("Clients: \n" + Arrays.toString(avg(avgs, clients)));
				System.out.println("Success vector: \n" + Arrays.toString(avg(avgs, succeeded)));
				System.out.println("Required rounds vector: \n" + Arrays.toString(avg(avgs, requiredRounds)));
				System.out.println("Empty slots vector: \n" + Arrays.toString(avg(avgs, emptySlots)));
			}

			private void benchmark(int[] clients, int[] succeeded, int[] requiredRounds, int[] emptySlots) {
				System.out.print("Benchmarking ");
				for(int i = 1 + numSlots/factor; i < samples; i++) {
					Scheduling s = new Scheduling(i*factor, numSlots, numBits);
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