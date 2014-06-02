package net;

import dc.DCPackage;

public interface PackageListener {
	public void addInput(DCPackage message);

	public void addInput(NetStatPackage message);

	public void connectionLost();
}