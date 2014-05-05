package dc.scheduling;

import dc.DCPackage;

public class PrimitiveScheduler implements Scheduler {
	private int currentRound = -1;
	private int roundRange;

	public PrimitiveScheduler() {

	}

	@Override
	public boolean addPackage(DCPackage p) {
		currentRound = p.getNumber();
		roundRange = p.getNumberRange();
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

	@Override
	public int getNextRound() {
		if(currentRound == -1) {
			return -1;
		} else {
			int offset = (int) (Math.random() * (double) (roundRange - 1)) + 1;
			return (currentRound + offset)%roundRange;
		}
	}

}