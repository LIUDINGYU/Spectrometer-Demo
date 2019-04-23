package com.oceanoptics.spectrometer;

public class AutonullingConfiguration {
	public final byte SLOT_AUTONULLING = 17;
	private int enabled;
	private int temperatureCompensatationEnabled;
	private int darkValue;
	private double saturationValue;

	public AutonullingConfiguration() {
		this.enabled = -1;
		this.temperatureCompensatationEnabled = -1;
		this.darkValue = -1;
		this.saturationValue = -1.0D;
	}

	public AutonullingConfiguration(int ena, int temp, int dark, double sat) {
		this.enabled = ena;
		this.temperatureCompensatationEnabled = temp;
		this.darkValue = dark;
		this.saturationValue = sat;
	}

	public int getEnabled() {
		return this.enabled;
	}

	public void setEnabled(int ena) {
		this.enabled = ena;
	}

	public int getTemperatureCompensationEnabled() {
		return this.temperatureCompensatationEnabled;
	}

	public void setTemperatureCompensationEnabled(int ena) {
		this.temperatureCompensatationEnabled = ena;
	}

	public int getDarkValue() {
		return this.darkValue;
	}

	protected void setDarkValue(int value) {
		this.darkValue = value;
	}

	public double getSaturationValue() {
		return this.saturationValue;
	}

	protected void setSaturationValue(double value) {
		this.saturationValue = value;
	}
}
