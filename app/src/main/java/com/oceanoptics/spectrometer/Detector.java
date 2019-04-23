package com.oceanoptics.spectrometer;

public class Detector {
	protected String serialNumber;
	protected String arrayCoatingMfg;
	protected boolean lensInstalled = false;
	protected String arrayWavelength;

	public boolean isDefined() {
		if ((this.serialNumber == null) || (this.arrayCoatingMfg == null)
				|| (this.arrayWavelength == null)) {
			return false;
		}
		return true;
	}

	public String getSerialNumber() {
		return this.serialNumber;
	}

	public void setSerialNumber(String value) {
		this.serialNumber = value;
	}

	public String getArrayCoatingMfg() {
		return this.arrayCoatingMfg;
	}

	public void setArrayCoatingMfg(String value) {
		this.arrayCoatingMfg = value;
	}

	public boolean isLensInstalled() {
		return this.lensInstalled;
	}

	public void setLensInstalled(boolean value) {
		this.lensInstalled = value;
	}

	public String getArrayWavelength() {
		return this.arrayWavelength;
	}

	public void setArrayWavelength(String value) {
		this.arrayWavelength = value;
	}
}
