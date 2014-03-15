package dc.testing;


/**
 * A connection mockup that bundles two channels, one for each direction.
 * @author moritz
 *
 */
public class DummyConnection {
	public final DummyChannel chA, chB;

	public DummyConnection() {
		chA = new DummyChannel();
		chB = new DummyChannel();
	}

}
