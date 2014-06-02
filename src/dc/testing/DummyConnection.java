package dc.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.Connection;
import net.PackageListener;

/**
 * A connection mockup that bundles two channels, one for each direction.
 * @author moritz
 *
 */
public class DummyConnection extends Connection {
	
	public DummyConnection(InputStream is, OutputStream os, PackageListener listener) {
		super(is, os, listener);
	}

	
}
