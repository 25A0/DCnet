package dc;

public class DCConfig {
	
	public static final String VERSION = "0.1";
	
	// The minimum number of keys that have to be shared with other members of the network before communcation starts.
	// So, a value of 2 means that a station has to share keys with to 2 other stations, leading to networks
	// of a minimum size of 3.
	public static final int MIN_NUM_KEYS = 2;
}