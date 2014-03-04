package cli;

/**
 * Static class to control debug output
 * @author moritz
 *
 */
public class Debugger {
	/**
	 * The debugging level. Can be changed by calling setlevel
	 */
	private static int level = 1;
	public static final int DEBUG_OFF = 0, DEBUG_SOME = 1, DEBUG_FULL = 2;
	
	public static void print(int level, String s) {
		if(level <= 0 || level > Debugger.level) return;
		else {
			System.out.print(s);
		}
	}
	
	public static void println(int level, String s) {
		print(level, s+"\n");
	}
	
	/**
	 * Change the current debug level
	 * @param level The new level. Values outside the bounds will be set to the closest bound.
	 */
	public static void setLevel(int level) {
		if(level <= 0) {
			Debugger.level = 0;
		} else if(level >= DEBUG_FULL) {
			Debugger.level = DEBUG_FULL;
		} else {
			Debugger.level = level;
		}
		System.out.println("[Debugger] Debugging level has been set to " + Debugger.level);
	}
	
}
