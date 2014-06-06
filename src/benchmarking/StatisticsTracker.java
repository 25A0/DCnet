package benchmarking;

public class StatisticsTracker {

	private long overallBytes;
	private int overallReservations;
	private int overallRequiredRounds;
	private int overallCollisions;
	private int overallFreeSlots;
	private int numScheduled;
	private int overallClients;

	public StatisticsTracker() {

	}

	/**
	 * Reports a successful reservation.
	 * @param bytes  The number of bytes that the station needed to send in order to achieve this reservation.
	 */
	public void reportReservation(long bytes) {
		overallBytes += bytes;
		overallReservations++;
	}

	public void reportCollisions(int collisions) {
		overallCollisions+= collisions;
	}

	public void reportRequiredRounds(int requiredRounds) {
		overallRequiredRounds += requiredRounds;
	}

	public void reportFreeSlots(int freeSlots) {
		overallFreeSlots+= freeSlots;
	}

	public void reportCoverage(int numScheduled, int overallClients) {
		this.numScheduled = numScheduled;
		this.overallClients = overallClients;
	}

	public double getAverageBytesPerReservation() {
		return (double) overallBytes / (double) overallReservations;
	}

	public double getAverageCollisions(int numSamples) {
		return (double) overallCollisions / (double) numSamples;
	}

	public double getAverageFreeSlots(int numSamples) {
		return (double) overallFreeSlots / (double) numSamples;
	}

	public double getAverageRequiredRounds(int numSamples) {
		return (double) overallRequiredRounds / (double) numSamples;
	}

	public double getCoverage() {
		return (double) numScheduled / (double) overallClients;
	}

}