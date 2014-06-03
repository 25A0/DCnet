package net;

import java.util.Collection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.InputMismatchException;

import dc.DCConfig;

public abstract class NetStatPackage {
	/**
	 * Applies the changes introduced by this package to the given network
	 * @param net The network to be changed
	 */
	public abstract void apply(Network net);

	/**
	 * Turns the content of this package into a byte array that can be sent via network.
	 */
	public abstract byte[] toByteArray();

	
	/**
	 * This variant of the package is used to transmit a snapshot of the current
	 * network state. It will contain a list of network members.
	 */
	public static class Snapshot extends NetStatPackage {
		private Collection<String> stations;

		public Snapshot(Collection<String> stations) {
			this.stations = stations;
		}

		public void apply(Network net) {
			net.initialise(stations);
		}

		/**
		 * Returns the set of involved stations.
		 * This function should only be called for reference. In order to apply the 
		 * content of this package to a given network, {@code apply} should be called.
		 * @return The set of stations in this snapshot.
		 */
		public Collection<String> getStations() {
			return stations;
		}

		public byte[] toByteArray() {
			int length = stations.size();
			int numHeaders = 0;
			do { // to make sure that there is at least one header byte.
				numHeaders++;
				length >>= 6;
			} while(length > 0);
			int bufferSize = numHeaders + stations.size() * DCConfig.ALIAS_LENGTH;
			ByteBuffer bb = ByteBuffer.wrap(new byte[bufferSize]);
			length = stations.size();
			//Make header
			for(int i = 0; i < numHeaders; i++) {
				byte b = (byte) (1 << 7);
				// Set the bit that indicates whether there's another header
				b |= (byte) (i < numHeaders - 1? 0:1) << 6;
				// Add lower 6 bits of number of stations
				b |= length & 0x3F;
				length >>= 6;
				bb.put(b);
			}
			// Append station aliases
			Iterator<String> it = stations.iterator();
			while (it.hasNext()) {
				String alias = it.next();
				if(alias.length() <= DCConfig.ALIAS_LENGTH) {
					//pad alias
					byte[] padding = new byte[DCConfig.ALIAS_LENGTH - alias.length()];
					bb.put(padding);
				}
				bb.put(alias.getBytes());
			}
			return bb.array();

		}
	}

	/**
	 * This variant is used to communicate that a station has left the network.
	 */
	public static class Leaving extends NetStatPackage {
		private String station;

		public Leaving(String alias) {
			station = alias;
		}

		public void apply(Network net) {
			net.removeStation(station);
		}

		/**
		 * Returns the alias of the station that left.
		 * This function should only be called for reference. In order to apply the 
		 * content of this package to a given network, {@code apply} should be called.
		 */
		public String getStation() {
			return station;
		}

		public byte[] toByteArray() {
			byte[] output = new byte[1 + DCConfig.ALIAS_LENGTH];
			output[0] = (byte) 1;
			byte[] alias = station.getBytes();
			for(int i = 0; i < DCConfig.ALIAS_LENGTH; i++) {
				output[1 + i] = alias[i];
			}
			return output;
		}
	}

	/**
	 * This variant is used to communicate that a station has entered the network.
	 */
	public static class Joining extends NetStatPackage {
		private String station;

		public Joining(String alias) {
			station = alias;
		}

		/**
		 * Returns the alias of the station that joins
		 * This function should only be called for reference. In order to apply the 
		 * content of this package to a given network, {@code apply} should be called.
		*/
		public String getStation() {
			return station;
		}

		@Override
		public void apply(Network net) {
			net.addStation(station);
		}

		public byte[] toByteArray() {
			byte[] output = new byte[1 + DCConfig.ALIAS_LENGTH];
			output[0] = (byte) 0;
			byte[] alias = station.getBytes();
			int pos;
			if(alias.length <= DCConfig.ALIAS_LENGTH) {
				pos = output.length - alias.length;
			} else {
				pos = 1;
			}
			for(int i = 0; i < DCConfig.ALIAS_LENGTH && i < alias.length; i++) {
				output[pos + i] = alias[i];
			}
			return output;
		}
	}
}