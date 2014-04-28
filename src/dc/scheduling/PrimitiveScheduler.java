package dc.scheduling;

public class PrimitiveScheduler implements Scheduler {
	private int currentRound = -1;

	public PrimitiveScheduler() {

	}

	@Override
	public boolean addPackage(DcPackage p) {
		
	}

	@Override
	public int getNextRound() {
		if(currentRound == -1) {
			return -1;
		} else {
			return (currentRound + 1)%
		}
	}

}