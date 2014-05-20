package net;

import java.util.ArrayList;
import util.HashUtil;
import java.util.Iterator;
import java.util.Collection;
import java.lang.IllegalStateException;

public class Network {
	private static HashUtil hu = new HashUtil(HashUtil.SHA_256);
	private ArrayList<String> stations;

	public Network() {
		stations = new ArrayList<String>();
	}

	/**
	 * Adds a station to this network, and inserts it into the list of stations in such a way that the list is lexicographically ordered according to {@code String.compareTo(String someString)}.
	 * @param alias The alias of the station that was added to the network
	 */
	public synchronized void addStation(String alias) {
		Iterator<String> it = stations.iterator();
		for(int i = 0; i < stations.size(); i++) {
			// compare both strings
			int com = it.next().compareTo(alias);
			if(com == 0) {
				throw new IllegalStateException("The alias " + alias + " is already part of this network.");
			} else if(com > 0) {
				stations.add(i, alias);
				return;
			}
		}
		// In case no other condition triggered
		stations.add(alias);
	}

	/**
	 * Removes a station from the network.
	 * @param alias The alias of that station
	 */
	public synchronized void removeStation(String alias) {
		boolean contained = stations.remove(alias);
		if(!contained) {
			throw new IllegalStateException("The station with alias " + alias + " was not part of this network.");
		}
	}

	/**
	 * Returns a shallow copy of all members in this network.
	 */
	public Collection<String> getStations() {
		return new ArrayList<String>(stations);
	}

	/**
	 * Creates a hash that represents the current state of this network.
	 * The hash is constructed like this:
	 * The string s contains the aliases of all current network members, 
	 * ordered lexicographically, separated by line feeds (\\n), and concluded
	 * by the number of members.
	 * The returned byte array is the SHA-256 hash of that string s.
	 * @return A hash representing the current network state.
	 */
	public byte[] getHash() {
		StringBuilder sb = new StringBuilder();
		Iterator<String> it = stations.iterator();
		while(it.hasNext()){
			sb.append(it.next() + "\n");
		}
		sb.append(stations.size());
		return hu.digest(sb.toString().getBytes());
	}
}