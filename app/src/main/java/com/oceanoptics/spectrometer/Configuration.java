package com.oceanoptics.spectrometer;

import java.io.IOException;

public class Configuration {
	protected Bench bench = new Bench();
	protected Detector detector = new Detector();
	protected String cpldVersion;
	public Embed2000Plus spectrometer;

	public Configuration(Embed2000Plus spec) throws IOException {
		this.spectrometer = spec;
		getConfigurationFromSpectrometer();
	}

	public Configuration(Embed2000Plus spec, Configuration c) {
		this.spectrometer = spec;
		setBench(c.getBench());
		setDetector(c.getDetector());
		setCpldVersion(c.getCpldVersion());
	}

	public void getConfigurationFromSpectrometer() throws IOException {
		if (isBenchDefined()) {
			String[] slot = this.spectrometer.getInfo(
					this.spectrometer.getBenchSlot()).split(" ");
			if (slot.length == 3) {
				this.bench.setGrating(slot[0]);
				this.bench.setFilterWavelength(slot[1]);
				this.bench.setSlitSize(slot[2]);
			}
		}
		if (isSpectrometerConfigurationDefined()) {
			String[] configSlot = this.spectrometer.getInfo(
					this.spectrometer.getSpectrometerConfigSlot()).split(" +");
			if (configSlot.length == 2) {
				Detector d = getDetector();

				String temp = configSlot[0].substring(0, 1);
				if (temp.equalsIgnoreCase("P")) {
					temp = "Photometic";
				} else if (temp.equalsIgnoreCase("T")) {
					temp = "AST";
				}
				d.setArrayCoatingMfg(temp);

				d.setArrayWavelength(configSlot[0].substring(1, 2));

				String l2 = configSlot[0].substring(2);
				d.setLensInstalled(!l2.trim().equals("0"));

				setCpldVersion(configSlot[1]);
			}
		}
		if (isDetectorSerialNumberDefined()) {
			Detector d = getDetector();
			d.setSerialNumber(this.spectrometer.getInfo(this.spectrometer
					.getDetectorSerialNumberSlot()));
		}
	}

	public void setConfigurationToSpectrometer() throws IOException {
		if (isBenchDefined()) {
			Bench b = getBench();

			String gratingString = "0" + b.getGrating();
			gratingString = gratingString.substring(gratingString.length() - 2,
					gratingString.length());

			String filterString = "00" + b.getFilterWavelength();
			filterString = filterString.substring(filterString.length() - 3,
					filterString.length());

			String slitString = "00" + b.getSlitSize();
			slitString = slitString.substring(slitString.length() - 3,
					slitString.length());

			this.spectrometer.setInfo(this.spectrometer.getBenchSlot(),
					gratingString + " " + filterString + " " + slitString);
		}
		if (isSpectrometerConfigurationDefined()) {
			Detector d = getDetector();
			this.spectrometer.setInfo(
					this.spectrometer.getSpectrometerConfigSlot(),
					d.getArrayCoatingMfg() + d.getArrayWavelength()
							+ (d.isLensInstalled() ? "1" : "0") + "  "
							+ getCpldVersion());
		}
		if (isDetectorSerialNumberDefined()) {
			this.spectrometer.setInfo(
					this.spectrometer.getDetectorSerialNumberSlot(),
					getDetector().getSerialNumber());
		}
	}

	public Bench getBench() {
		return this.bench;
	}

	public void setBench(Bench value) {
		this.bench = value;
	}

	public Detector getDetector() {
		return this.detector;
	}

	public void setDetector(Detector value) {
		this.detector = value;
	}

	public String getCpldVersion() {
		return this.cpldVersion;
	}

	public void setCpldVersion(String value) {
		this.cpldVersion = value;
	}

	public boolean isBenchDefined() {
		return this.spectrometer.getBenchSlot() >= 0;
	}

	public boolean isSpectrometerConfigurationDefined() {
		return this.spectrometer.getSpectrometerConfigSlot() >= 0;
	}

	public boolean isDetectorSerialNumberDefined() {
		return this.spectrometer.getDetectorSerialNumberSlot() >= 0;
	}

	public boolean isCPLDVersionDefined() {
		return false;
	}

	public String toString() {
		Bench b = getBench();
		Detector d = getDetector();

		String s = "";
		if (isBenchDefined()) {
			s = s + "Optical Bench:\n\tGrating: " + b.getGrating()
					+ "\n\tFilter: " + b.getFilterWavelength()
					+ "\n\tSlit size: " + b.getSlitSize() + "\n";
		}
		if (isSpectrometerConfigurationDefined()) {
			s = s + "Detector:\n\tArray coating manufacturer: "
					+ d.getArrayCoatingMfg() + "\n\tArray wavelength: "
					+ d.getArrayWavelength() + "\n\tLens installed: "
					+ d.isLensInstalled() + "\n\tSerial number: "
					+ d.getSerialNumber() + "\n";
		}
		if (isDetectorSerialNumberDefined()) {
			s = s + "\tDetector serial number: " + d.getSerialNumber() + "\n";
		}
		s = s + "CPLD version: " + getCpldVersion() + "\n";

		return s;
	}
}
