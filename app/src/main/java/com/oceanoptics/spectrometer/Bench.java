package com.oceanoptics.spectrometer;

public class Bench {
	protected String filterWavelength;
	protected String slitSize;
	protected String grating;

	public boolean isDefined() {
		return true;
	}

	public String getFilterWavelength() {
		return this.filterWavelength;
	}

	public void setFilterWavelength(String value) {
		this.filterWavelength = value;
	}

	public String getSlitSize() {
		return this.slitSize;
	}

	public void setSlitSize(String value) {
		this.slitSize = value;
	}

	public String getGrating() {
		return this.grating;
	}

	public void setGrating(String value) {
		this.grating = value;
	}
}
