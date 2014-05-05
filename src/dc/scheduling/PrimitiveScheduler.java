package dc.scheduling;

import dc.DCPackage;

public class PrimitiveScheduler implements Scheduler {
	private int currentRound;
	private int nextScheduledRound;
	private int roundRange;

	public PrimitiveScheduler() {
		currentRound = -1;
		nextScheduledRound = -1;
	}

	@Override
	public boolean addPackage(DCPackage p) {
		roundRange = p.getNumberRange();
		currentRound = p.getNumber();
		if(currentRound == nextScheduledRound || nextScheduledRound == -1) {
			nextScheduledRound = calculateNextRound();
		}
		return true;
	}

	@Override
	public int getScheduleSize() {
		return 0;
	}

	@Override
	public byte[] getSchedule() {
		return new byte[]{};
	}

	private int calculateNextRound() {
		int offset = (int) (Math.random() * (double) (roundRange - 1)) + 1;
		return (currentRound + offset)%roundRange;
	}

	@Override
	public int getNextRound() {
		return nextScheduledRound;
	}

}