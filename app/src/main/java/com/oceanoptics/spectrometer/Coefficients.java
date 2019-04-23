package com.oceanoptics.spectrometer;

public class Coefficients {
	public static final int WL_INTERCEPT = 1;
	public static final int WL_FIRST = 2;
	public static final int WL_SECOND = 3;
	public static final int WL_THIRD = 4;
	public static final int STRAY_LIGHT = 5;
	public static final int NL_0 = 6;
	public static final int NL_1 = 7;
	public static final int NL_2 = 8;
	public static final int NL_3 = 9;
	public static final int NL_4 = 10;
	public static final int NL_5 = 11;
	public static final int NL_6 = 12;
	public static final int NL_7 = 13;
	public static final int NL_ORDER = 14;
	public double _WlIntercept;
	public double _WlFirst;
	public double _WlSecond;
	public double _WlThird;
	public double _StrayLight;
	public double strayLightSlope;
	public double _NlCoef0;
	public double _NlCoef1;
	public double _NlCoef2;
	public double _NlCoef3;
	public double _NlCoef4;
	public double _NlCoef5;
	public double _NlCoef6;
	public double _NlCoef7;
	public int _NlOrder;
	protected boolean has_WlIntercept = false;
	protected boolean has_WlFirst = false;
	protected boolean has_WlSecond = false;
	protected boolean has_WlThird = false;
	protected boolean has_StrayLight = false;
	protected boolean has_NlCoef0 = false;
	protected boolean has_NlCoef1 = false;
	protected boolean has_NlCoef2 = false;
	protected boolean has_NlCoef3 = false;
	protected boolean has_NlCoef4 = false;
	protected boolean has_NlCoef5 = false;
	protected boolean has_NlCoef6 = false;
	protected boolean has_NlCoef7 = false;
	protected boolean has_NlOrder = false;
	private final String[] DESCRIPTIONS = {
			"0th order wavelength calibration coefficient",
			"1st order wavelength calibration coefficient",
			"2nd order wavelength calibration coefficient",
			"3rd order wavelength calibration coefficient",
			"Stray light constant",
			"0th order non-linearity correction coefficient",
			"1st order non-linearity correction coefficient",
			"2nd order non-linearity correction coefficient",
			"3rd order non-linearity correction coefficient",
			"4th order non-linearity correction coefficient",
			"5th order non-linearity correction coefficient",
			"6th order non-linearity correction coefficient",
			"7th order non-linearity correction coefficient",
			"Polynomial order of non-linearity calibration" };

	public Coefficients() {
	}

	public Coefficients(Coefficients c) {
		setWlIntercept(c.getWlIntercept());
		setWlFirst(c.getWlFirst());
		setWlSecond(c.getWlSecond());
		setWlThird(c.getWlThird());
		setStrayLight(c.getStrayLight(), c.getStrayLightSlope());
		setNlCoef0(c.getNlCoef0());
		setNlCoef1(c.getNlCoef1());
		setNlCoef2(c.getNlCoef2());
		setNlCoef3(c.getNlCoef3());
		setNlCoef4(c.getNlCoef4());
		setNlCoef5(c.getNlCoef5());
		setNlCoef6(c.getNlCoef6());
		setNlCoef7(c.getNlCoef7());
		setNlOrder(c.getNlOrder());
	}

	public String[] getDescriptions() {
		return this.DESCRIPTIONS;
	}

	public double getWlIntercept() {
		return this._WlIntercept;
	}

	public void setWlIntercept(double value) {
		this._WlIntercept = value;
		this.has_WlIntercept = true;
	}

	public double getWlFirst() {
		return this._WlFirst;
	}

	public void setWlFirst(double value) {
		this._WlFirst = value;
		this.has_WlFirst = true;
	}

	public double getWlSecond() {
		return this._WlSecond;
	}

	public void setWlSecond(double value) {
		this._WlSecond = value;
		this.has_WlSecond = true;
	}

	public double getWlThird() {
		return this._WlThird;
	}

	public void setWlThird(double value) {
		this._WlThird = value;
		this.has_WlThird = true;
	}

	public double[] getWlCoefficients() {
		double[] wl = new double[4];
		wl[0] = this._WlIntercept;
		wl[1] = this._WlFirst;
		wl[2] = this._WlSecond;
		wl[3] = this._WlThird;
		return wl;
	}

	public void setWlCoefficients(double[] wl) {
		this._WlIntercept = wl[0];
		this.has_WlIntercept = true;
		this._WlFirst = wl[1];
		this.has_WlFirst = true;
		this._WlSecond = wl[2];
		this.has_WlSecond = true;
		this._WlThird = wl[3];
		this.has_WlThird = true;
	}

	public double getNlCoef0() {
		return this._NlCoef0;
	}

	public void setNlCoef0(double value) {
		this._NlCoef0 = value;
		this.has_NlCoef0 = true;
	}

	public double getNlCoef1() {
		return this._NlCoef1;
	}

	public void setNlCoef1(double value) {
		this._NlCoef1 = value;
		this.has_NlCoef1 = true;
	}

	public double getNlCoef2() {
		return this._NlCoef2;
	}

	public void setNlCoef2(double value) {
		this._NlCoef2 = value;
		this.has_NlCoef2 = true;
	}

	public double getNlCoef3() {
		return this._NlCoef3;
	}

	public void setNlCoef3(double value) {
		this._NlCoef3 = value;
		this.has_NlCoef3 = true;
	}

	public double getNlCoef4() {
		return this._NlCoef4;
	}

	public void setNlCoef4(double value) {
		this._NlCoef4 = value;
		this.has_NlCoef4 = true;
	}

	public double getNlCoef5() {
		return this._NlCoef5;
	}

	public void setNlCoef5(double value) {
		this._NlCoef5 = value;
		this.has_NlCoef5 = true;
	}

	public double getNlCoef6() {
		return this._NlCoef6;
	}

	public void setNlCoef6(double value) {
		this._NlCoef6 = value;
		this.has_NlCoef6 = true;
	}

	public double getNlCoef7() {
		return this._NlCoef7;
	}

	public void setNlCoef7(double value) {
		this._NlCoef7 = value;
		this.has_NlCoef7 = true;
	}

	public int getNlOrder() {
		return this._NlOrder;
	}

	public void setNlOrder(int value) {
		this._NlOrder = value;
		this.has_NlOrder = true;
	}

	public double[] getNlCoefficients() {
		double[] nl = new double[8];
		nl[0] = this._NlCoef0;
		nl[1] = this._NlCoef1;
		nl[2] = this._NlCoef2;
		nl[3] = this._NlCoef3;
		nl[4] = this._NlCoef4;
		nl[5] = this._NlCoef5;
		nl[6] = this._NlCoef6;
		nl[7] = this._NlCoef7;
		return nl;
	}

	public void setNlCoefficients(double[] nl) {
		this._NlCoef0 = nl[0];
		this.has_NlCoef0 = true;
		this._NlCoef1 = nl[1];
		this.has_NlCoef1 = true;
		this._NlCoef2 = nl[2];
		this.has_NlCoef2 = true;
		this._NlCoef3 = nl[3];
		this.has_NlCoef3 = true;
		this._NlCoef4 = nl[4];
		this.has_NlCoef4 = true;
		this._NlCoef5 = nl[5];
		this.has_NlCoef5 = true;
		this._NlCoef6 = nl[6];
		this.has_NlCoef6 = true;
		this._NlCoef7 = nl[7];
		this.has_NlCoef7 = true;
	}

	public double getStrayLight() {
		return this._StrayLight;
	}

	public double getStrayLightSlope() {
		return this.strayLightSlope;
	}

	public void setStrayLight(double value) {
		this._StrayLight = value;
		this.has_StrayLight = true;
	}

	public void setStrayLight(double intercept, double slope) {
		setStrayLight(intercept);
		this.strayLightSlope = slope;
	}

	public String toString() {
		int i = 0;
		return this.DESCRIPTIONS[(i++)] + " " + getWlIntercept() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getWlFirst() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getWlSecond() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getWlThird() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getStrayLight() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getNlCoef0() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getNlCoef1() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getNlCoef2() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getNlCoef3() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getNlCoef4() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getNlCoef5() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getNlCoef6() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getNlCoef7() + "\n"
				+ this.DESCRIPTIONS[(i++)] + " " + getNlOrder() + "\n";
	}
}
