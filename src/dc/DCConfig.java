package dc;

import dc.scheduling.FingerprintScheduler;
import dc.scheduling.PrimitiveScheduler;
import dc.scheduling.Scheduler;

public class DCConfig {
	
	public static final String VERSION = "0.3 (Bachelor's thesis release)";
	
	// The minimum number of keys that have to be shared with other members of the network before communcation starts.
	// So, a value of 2 means that a station has to share keys with to 2 other stations, leading to networks
	// of a minimum size of 3.
	public static final int MIN_NUM_KEYS = 2;

	public static final int PACKAGE_SIZE = 1024;

	public static final int ALIAS_LENGTH = 8;

	public static final SchedulingMethod schedulingMethod = SchedulingMethod.FINGERPRINT;

	public enum SchedulingMethod {
		PRIMITIVE(){
			@Override
			public Scheduler getScheduler() {
				return new PrimitiveScheduler();
			}
		},
		FINGERPRINT(){
			@Override
			public Scheduler getScheduler() {
				return new FingerprintScheduler();
			}
		};

		public abstract Scheduler getScheduler();

	}
	
}