package com.oceanoptics.spectrometer;

public class Spectrum {
	private double[] spectrum;
	private double[] darkPixels;
	private boolean saturated;

	public Spectrum(int totalpixels, int darkPixels) {
		this.spectrum = new double[totalpixels];
		this.darkPixels = new double[darkPixels];
	}

	public Spectrum(double[] spectrum, double[] darkPixels) {
		this.spectrum = spectrum;
		this.darkPixels = darkPixels;
	}

	public double[] getSpectrum() {
		return this.spectrum;
	}

	public void setSpectrum(double[] spectrum) {
		this.spectrum = spectrum;
	}

	public void setDark(double[] dark) {
		this.darkPixels = dark;
	}
	
	public boolean isOfSize(int numberOfPixels, int numberOfDarkPixels) {
		return (this.spectrum.length == numberOfPixels)
				&& (this.darkPixels.length == numberOfDarkPixels);
	}

	public int getNumberOfDarkPixels() {
		return this.darkPixels.length;
	}

	public double[] getDarkPixels() {
		return this.darkPixels;
	}

	public boolean isSameSizeAs(Spectrum other) {
		return (other.getSpectrum().length == this.spectrum.length)
				&& (other.getNumberOfDarkPixels() == this.darkPixels.length);
	}

	public boolean isSaturated() {
		return this.saturated;
	}

	public void setSaturated(boolean saturated) {
		this.saturated = saturated;
	}
}
