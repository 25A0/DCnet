package dc.scheduling;

public interface Scheduler {
	/**
	 * Forwards the result of a round to this Scheduler.
	 * @param  p The package that was the result of the last round
	 * @return   true if the outcome of this round results in a different schedule, false otherwise.
	 */
	public boolean addPackage(DCPackage p);

	/**
	 * Returns the index of the next round that is scheduled by this Scheduler, or -1 if there is no round scheduled currently.
	 */
	public int getNextRound();
	
}