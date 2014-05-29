package dc.scheduling;

import dc.DCPackage;

public interface Scheduler {
	/**
	 * Forwards the result of a round to this Scheduler.
	 * @param  p The package that was the result of the last round
	 * @return   true if the outcome of this round results in a different schedule, false otherwise.
	 */
	public boolean addPackage(DCPackage p);

	/**
	 * Returns the size of the scheduling block in bytes
	 */
	public int getScheduleSize();

	/**
	 * Returns the next scheduling block. This function has to return the same output until {@code addPackage} is called.
	 * @return a byte array holding the scheduling block that has to be sent 
	 */
	public byte[] getSchedule();

	/**
	 * Returns the index of the next round that is scheduled by this Scheduler, or -1 if there is no round scheduled currently.
	 */
	public int getNextRound();
	
}