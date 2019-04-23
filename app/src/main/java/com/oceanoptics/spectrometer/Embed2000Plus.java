package com.oceanoptics.spectrometer;

import android.util.Log;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;

/**
 * Created by cheng.mingming on 2016/11/26.
 */

public class Embed2000Plus extends UsbSpectrometer {
    private int pipeSize = 2048 * 2 + 1;
    private int numberOfCCDPixels = 2048;
    private int numberOfDarkCCDPixels = 16;
    private int maxIntensity;
    private int integrationTimeMinimum;
    private int integrationTimeMaximum;
    private int integrationTimeIncrement;
    private int integrationTimeBase;
    private int integrationTime;
    private boolean correctForElectricalDark = false;
    private boolean correctForDetectorNonlinearity = false;
    private int smoothingWindowSize;

    protected byte[] rawData = new byte[numberOfCCDPixels * 2 + 1];
    private AutonullingConfiguration autonullingConfiguration;
    private Coefficients coefficients;
    private double nlCoeff0;
    private double nlCoeff1;
    private double nlCoeff2;
    private double nlCoeff3;
    private double nlCoeff4;
    private double nlCoeff5;
    private double nlCoeff6;
    private double nlCoeff7;
    protected double[] wavelengths = new double[0];
    private int benchSlot = -1;
    private int spectrometerConfigSlot = -1;
    private int detectorSerialNumberSlot = -1;
    private Configuration configuration;

    private static final Embed2000Plus INSTANCE = new Embed2000Plus();
    public static Embed2000Plus getInstance() { return INSTANCE; }

    private Embed2000Plus() {
    }

    public int getSpectrometerCounts() throws IOException {
        if (openDevice(0)) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    protected void initialize() throws IOException {
        integrationTimeMinimum = 1000;
        integrationTimeMaximum = 65535000;
        integrationTimeIncrement = 10;
        integrationTimeBase = 1;
        numberOfCCDPixels = 2048;
        numberOfDarkCCDPixels = 16;
        maxIntensity = 65535;
        benchSlot = 15;
        spectrometerConfigSlot = 16;
        detectorSerialNumberSlot = 0;

        synchronized (this.out) {
            this.out[0] = 1;
            bulkOut(this.dataOutEndPoint, this.out, 1);
        }

        autonullingConfiguration = readAutonullingConfigurationFromSpectrometer();
        getCoefficientsFromSpectrometer();
        getConfigurationFromSpectrometer();
    }

    private AutonullingConfiguration readAutonullingConfigurationFromSpectrometer() throws IOException {
        AutonullingConfiguration cfg = new AutonullingConfiguration();
        synchronized (this.in) {
            synchronized (this.out) {
                out[0] = 5;
                out[1] = 17;
                bulkOut(this.dataOutEndPoint, this.out, 2);
                bulkIn(this.lowSpeedInEndPoint, this.in, 17);

                cfg.setEnabled(this.in[2]);
                cfg.setTemperatureCompensationEnabled(this.in[3]);
                cfg.setDarkValue(ByteRoutines.makeWord(this.in[5], this.in[4]));

                long temp = ByteRoutines.makeDWord((byte) 0, (byte) 0,
                        this.in[7], this.in[6]);
                cfg.setSaturationValue(temp);
            }
        }
        return cfg;
    }

    public String getSerialNumber() {
        return getInfo(0);
    }

    public void setSerialNumber(String serialNumber) throws IOException {
        setInfo(0, serialNumber);
    }

    public String getInfo(int slot) {
        synchronized (this.in) {
            synchronized (this.out) {
                this.out[0] = 5;
                this.out[1] = ByteRoutines.getLowByte(ByteRoutines.getLowWord(slot));
                String strRet = "";
                bulkOut(dataOutEndPoint, out, 2);
                bulkIn(lowSpeedInEndPoint, in, 17);
                int i = 2;
                while ((this.in[i] != 0) && (i < 17)) {
                    strRet = strRet + (char) this.in[i];
                    i++;
                }
                if ((5 == slot) && (i < 16) && (this.in[i] == 0)
                        && (this.in[(i + 1)] != 0)) {
                    i++;
                    strRet = strRet + " ";
                    while ((this.in[i] != 0) && (i < 17)) {
                        strRet = strRet + (char) this.in[i];
                        i++;
                    }
                }
                return strRet;
            }
        }
    }

    public void setInfo(int slot, String str) throws IOException {
        clearOutBuffer();
        synchronized (this.out) {
            this.out[0] = 6;
            this.out[1] = ByteRoutines.getLowByte(ByteRoutines.getLowWord(slot));
            try {
                byte[] bytes = str.getBytes("US-ASCII");
                for (int i = 0; i < bytes.length; i++) {
                    this.out[(2 + i)] = bytes[i];
                }
            } catch (UnsupportedEncodingException e) {
                throw new IOException("Error encoding configuration variables: " + e.getMessage());
            }
            bulkOut(this.dataOutEndPoint, this.out, 17);
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                throw new IOException("EEPROM write might not have completed. Please verify.");
            }
        }
    }

    protected void clearOutBuffer() {
        synchronized (this.out) {
            for (int i = 0; i < this.out.length; i++) {
                this.out[i] = 0;
            }
        }
    }

    public String getFirmwareVersion() {
        synchronized (this.in) {
            String firmwareVersionString = getUSBStringManufacturerName();
            firmwareVersionString = firmwareVersionString.substring(firmwareVersionString.indexOf(' ') + 1);

            return firmwareVersionString;
        }
    }

    public int getFPGA(byte reg) throws IOException {
        synchronized (this.out) {
            synchronized (this.in) {
                short filler = 0;
                out[0] = 107;
                out[1] = reg;
                bulkOut(this.dataOutEndPoint, this.out, 2);
                bulkIn(this.lowSpeedInEndPoint, this.in, 3);
                return ByteRoutines.makeDWord(filler,
                        ByteRoutines.makeWord(this.in[2], this.in[1]));
            }
        }
    }

    public String getFPGAFirmwareVersion() throws IOException {
        int v = getFPGA((byte) 4);
        byte lo = ByteRoutines.getLowByte(ByteRoutines.getLowWord(v));
        byte hi = ByteRoutines.getHighByte(ByteRoutines.getLowWord(v));
        return makeVersionString(lo, hi);
    }

    private String makeVersionString(byte lo, byte hi) {
        String version = "invalid";
        String low = Integer.toHexString(lo);
        String high = Integer.toHexString(hi);
        try {
            version = high.substring(0, 1) + "."
                    + high.substring(1, high.length()) + low.substring(0, 1)
                    + "." + low.substring(1, low.length());
        } catch (NumberFormatException ne) {
            version = high + low;
        }
        return version;
    }

    public int getIntegrationTime() throws IOException {
        return integrationTime;
    }

    public void setIntegrationTime(int intTime) throws IOException {
        synchronized (this.out) {
            boolean needStabilityScan = this.integrationTime != intTime;
            if (!needStabilityScan) {
                Log.d("Spectrometer", "Desired integration time already set, not pushing to spectrometer");
                return;
            }
            int maxTime = getIntegrationTimeMaximum();
            int minTime = getIntegrationTimeMinimum();
            if (intTime < minTime) {
                intTime = minTime;
            } else if (intTime > maxTime) {
                intTime = maxTime;
            }
            this.integrationTime = intTime;
            intTime /= getIntegrationTimeBase();

            this.out[0] = 2;
            this.out[1] = ByteRoutines.getLowByte(ByteRoutines
                    .getLowWord(intTime));
            this.out[2] = ByteRoutines.getHighByte(ByteRoutines
                    .getLowWord(intTime));
            this.out[3] = ByteRoutines.getLowByte(ByteRoutines
                    .getHighWord(intTime));
            this.out[4] = ByteRoutines.getHighByte(ByteRoutines
                    .getHighWord(intTime));

            bulkOut(this.dataOutEndPoint, this.out, 5);
            /*
            if ((needStabilityScan) && (isStabilityScan())) {
                doStabilityScan(1);
            }
            */
            Log.d("Spectrometer", "Integration time set to: " + intTime);
        }
        try {
            Thread.sleep(10);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getIntegrationTimeMinimum() {
        return this.integrationTimeMinimum;
    }

    public int getIntegrationTimeMaximum() {
        return this.integrationTimeMaximum;
    }

    public int getIntegrationTimeBase() {
        return this.integrationTimeBase;
    }

    public int getIntegrationTimeIncrement() {
        return this.integrationTimeIncrement;
    }

    public int getNumberOfPixels() {
        return numberOfCCDPixels;
    }

    public int getNumberOfDarkPixels() {
        return numberOfDarkCCDPixels;
    }

    public int getMaxIntensity() {
        return this.maxIntensity;
    }

    public String getCodeVersion(String fileName) {
        return "0.1";
    }

    public Spectrum getSpectrum(Spectrum spectrum) throws IOException {
        synchronized (this.out) {
            synchronized (this.in) {
                this.timeoutOccurredFlag = false;
                try {
                    requestSpectrum();
                    readSpectrum();
                } catch (IOException exception) {
                    timeoutOccurredFlag = determineWhetherTimeoutOccurred(exception);
                    throw exception;
                }
            }
        }
        spectrum.setSaturated(false);
        formatData(rawData, spectrum);
        if (this.correctForElectricalDark) {
            correctForElectricalDarkSignal(spectrum);
        }

        if (this.correctForDetectorNonlinearity) {
            correctForDetectorNonlinearity(spectrum);
        }

        if (this.smoothingWindowSize > 0) {
            boxcarAverage(spectrum);
        }

        return spectrum;
    }

    public void correctForElectricalDarkSignal(Spectrum spectrum) {
        double avgDark = 0.0D;
        int darkPixelCount = 16;
        int startPix = 6;
        double[] pixels = spectrum.getSpectrum();
        for (int i = startPix; i < startPix + darkPixelCount; i++) {
            avgDark += pixels[i];
        }
        avgDark /= darkPixelCount;
        applyElectricalDarkCorrection(avgDark, pixels);
    }

    private void applyElectricalDarkCorrection(double averageDarkPixelValue, double[] pixels) {
        for (int index = 1; index < pixels.length; index++) {
            pixels[index] -= averageDarkPixelValue;
        }
    }

    public void correctForDetectorNonlinearity(Spectrum spectrum) {
        double[] pixels = spectrum.getSpectrum();

        this.nlCoeff0 = this.coefficients.getNlCoef0();
        this.nlCoeff1 = this.coefficients.getNlCoef1();
        this.nlCoeff2 = this.coefficients.getNlCoef2();
        this.nlCoeff3 = this.coefficients.getNlCoef3();
        this.nlCoeff4 = this.coefficients.getNlCoef4();
        this.nlCoeff5 = this.coefficients.getNlCoef5();
        this.nlCoeff6 = this.coefficients.getNlCoef6();
        this.nlCoeff7 = this.coefficients.getNlCoef7();
        for (int i = 0; i < pixels.length; i++) {
            double factor = this.nlCoeff0;
            double pixel;
            double accumulator = pixel = pixels[i];

            factor += accumulator * this.nlCoeff1;
            accumulator *= pixel;

            factor += accumulator * this.nlCoeff2;
            accumulator *= pixel;

            factor += accumulator * this.nlCoeff3;
            accumulator *= pixel;

            factor += accumulator * this.nlCoeff4;
            accumulator *= pixel;
            if (this.coefficients.getNlOrder() == 7) {
                factor += accumulator * this.nlCoeff5;
                accumulator *= pixel;

                factor += accumulator * this.nlCoeff6;
                accumulator *= pixel;

                factor += accumulator * this.nlCoeff7;
            }
            pixels[i] = (pixel / factor);
        }
    }

    public void boxcarAverage(Spectrum spectrum) {
        double[] pixels = spectrum.getSpectrum();

        double[] temp = new double[pixels.length];
        System.arraycopy(pixels, 0, temp, 0, pixels.length);
        int end = pixels.length;
        for (int i = this.smoothingWindowSize; i < end
                - this.smoothingWindowSize; i++) {
            double sum = 0.0D;
            for (int j = -this.smoothingWindowSize; j < this.smoothingWindowSize + 1; j++) {
                sum += temp[(i + j)];
            }
            sum /= (2.0D * this.smoothingWindowSize + 1.0D);
            pixels[i] = sum;
        }
        spectrum.setSpectrum(pixels);
    }

    private void requestSpectrum() throws IOException {
        synchronized (this.out) {
            out[0] = 9;
            if (bulkOut(dataOutEndPoint, out, 1) < 0) {
                throw new IOException("cause:timeout");
            }
        }
        try {
            Thread.sleep(10);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void readSpectrum() throws IOException {
        synchronized (this.rawData) {
            synchronized (this.in) {
                if (bulkIn(this.highSpeedInEndPoint1, this.rawData, this.pipeSize) < 0) {
                    throw new IOException("cause:timeout");
                };
            }
        }
    }

    protected boolean determineWhetherTimeoutOccurred(Exception exception) {
        if (exception.getMessage().indexOf("cause:timeout") != -1) {
            return true;
        }
        return false;
    }

    protected Spectrum formatData(byte[] data, Spectrum doubleSpectrum)
            throws IOException {
        byte zero = 0;
        double[] spectrum = doubleSpectrum.getSpectrum();

        double saturationValue = this.autonullingConfiguration.getSaturationValue();

        doubleSpectrum.setSaturated(false);
        if ((data[(numberOfCCDPixels * 2)] != 0x69)) {
            throw new IOException("Lost synchronization");
        }
        for (int i = 0; i < numberOfCCDPixels; i++) {
            byte LSB = data[(2 * i)];
            byte MSB = data[(2 * i + 1)];
            int pixel = ByteRoutines.makeDWord(zero, zero, MSB, LSB);
            if (pixel >= saturationValue) {
                doubleSpectrum.setSaturated(true);
            }
            spectrum[i] = (pixel * 65535.0D / saturationValue);
        }
        // this.autonulling.setAutonullingScanStatus((int) spectrum[0]);
        // this.autonulling.setAutonullingScanValue((int) spectrum[1]);
        spectrum[0] = spectrum[2];
        spectrum[1] = spectrum[2];
        return doubleSpectrum;
    }

    public Spectrum getSpectrumRaw(Spectrum spectrum) throws IOException {
        Log.d("Spectrometer", "Getting spectrum...");
        synchronized (this.out) {
            synchronized (this.in) {
                try {
                    requestSpectrum();
                    readSpectrum();
                } catch (IOException e) {
                    return null;
                }
            }
        }
        spectrum.setSaturated(false);
        formatDataRaw(this.rawData, spectrum);

        return spectrum;
    }

    protected Spectrum formatDataRaw(byte[] data, Spectrum doubleSpectrum)
            throws IOException {
        return formatData(data, doubleSpectrum);
    }

    public double getBoardTemperatureCelsius() throws IOException {
        synchronized (this.out) {
            synchronized (this.in) {
                double value = -1.0D;
                out[0] = 108;
                bulkOut(this.dataOutEndPoint, this.out, 1);
                bulkIn(this.lowSpeedInEndPoint, this.in, 3);
                if (this.in[0] == 8) {
                    value = ByteRoutines.makeWord(this.in[2], this.in[1]);
                    value *= 0.003906D;
                } else {
                    throw new IOException("Invalid temperature response");
                }
                return value;
            }
        }
    }

    public void writeWavelengthCoefficientsToSpectrometer(Coefficients c)
            throws IOException {
        synchronized (this.out) {
            synchronized (this.in) {
                setWavelengthCalibrationCoefficients(c);
                setWavelengthCalibration();
                setWavelengths(getAllWavelengths());
            }
        }
    }

    private void setWavelengthCalibrationCoefficients(Coefficients c) {
        coefficients = c;
    }

    private void setWavelengthCalibration() throws IOException {
        DecimalFormat format = new DecimalFormat("0.000000E000");
        setInfo(1, format.format(this.coefficients.getWlIntercept()));
        setInfo(2, format.format(this.coefficients.getWlFirst()));
        setInfo(3, format.format(this.coefficients.getWlSecond()));
        setInfo(4, format.format(this.coefficients.getWlThird()));
    }

    public void setWavelengths(double[] wavelengths) {
        this.wavelengths = wavelengths;
    }

    public double[] getAllWavelengths() {
        this.wavelengths = new double[this.numberOfCCDPixels];
        for (int i = 0; i < this.numberOfCCDPixels; i++) {
            this.wavelengths[i] = getWavelength(i);
        }
        return this.wavelengths;
    }

    public double getWavelength(int pixel) {
        if ((pixel < 0) || (pixel > this.numberOfCCDPixels)) {
            throw new IllegalArgumentException("Pixel must betwen 0 and "
                    + this.numberOfCCDPixels + "; argument was: " + pixel);
        }
        double dp = pixel;
        return this.coefficients.getWlIntercept() + dp
                * this.coefficients.getWlFirst() + dp * dp
                * this.coefficients.getWlSecond() + dp * dp * dp
                * this.coefficients.getWlThird();
    }

    private double[] getAllStoredWavelengths() {
        return this.wavelengths;
    }

    public double[] getWavelengths() {
        if (wavelengths.length == 0) {
            return getAllWavelengths();
        } else {
            return getAllStoredWavelengths();
        }
    }

    public Coefficients getWavelengthCalibrationCoefficients() throws IOException {
        getCoefficientsFromSpectrometer();
        return coefficients;
    }

    public Coefficients getNonlinearityCoefficients() throws IOException {
        getCoefficientsFromSpectrometer();
        return coefficients;
    }

    private void getCoefficientsFromSpectrometer() throws IOException {
        coefficients = new Coefficients();
        try {
            coefficients.setWlIntercept(Double.parseDouble(getInfo(1)));
            this.coefficients.setWlFirst(Double.parseDouble(getInfo(2)));
            this.coefficients.setWlSecond(Double.parseDouble(getInfo(3)));
            this.coefficients.setWlThird(Double.parseDouble(getInfo(4)));
        } catch (NumberFormatException nfe) {
            this.coefficients.setWlIntercept(0.0D);
            this.coefficients.setWlFirst(1.0D);
            this.coefficients.setWlSecond(0.0D);
            this.coefficients.setWlThird(0.0D);

            Log.e("Spectrometer", "ERROR: " + nfe.getMessage());
            Log.e("Spectrometer", "ERROR: cannot read wavelength coefficients from spectrometer. Setting to pixel indices.");
            Log.e("Spectrometer", "Intercept was [" + getInfo(1) + "]");
            Log.e("Spectrometer", "First was [" + getInfo(2) + "]");
            Log.e("Spectrometer", "Second was [" + getInfo(3) + "]");
            Log.e("Spectrometer", "Third was [" + getInfo(4) + "]");
        }
        try {
            byte[] bytes = getInfoBytes(5);
            int i = 0;
            String str = new String();
            while ((i < bytes.length) && (0 != bytes[i])) {
                str = str + (char) bytes[i];
                i++;
            }
            if ((i < bytes.length - 1) && (0 == bytes[i])
                    && (bytes[(i + 1)] != 0) && (bytes[(i + 1)] != 255)) {
                i++;
                str = str + " ";
                while ((i < bytes.length) && (bytes[i] != 0)) {
                    str = str + (char) bytes[i];
                    i++;
                }
            }
            if (str.indexOf(' ') < 0) {
                this.coefficients.setStrayLight(Double.parseDouble(str));
            } else {
                String[] terms = str.split("\\s");
                this.coefficients.setStrayLight(Double.parseDouble(terms[0]),
                        Double.parseDouble(terms[1]));
            }
        } catch (NumberFormatException nfe) {
            Log.e("Spectrometer",
                    "ERROR: cannot read stray light coefficient from spectrometer. Setting to zero.");
            Log.e("Spectrometer", "Value was [" + getInfo(5) + "]");
            coefficients.setStrayLight(0.0D);
        } catch (ArrayIndexOutOfBoundsException ex) {
            Log.e("Spectrometer",
                    "ERROR: cannot read stray light coefficient from spectrometer. Setting to zero.");
            Log.e("Spectrometer", "Value was [" + getInfo(5) + "]");
            coefficients.setStrayLight(0.0D);
        }
        try {
            coefficients.setNlCoef0(Double.parseDouble(getInfo(6)));
            coefficients.setNlCoef1(Double.parseDouble(getInfo(7)));
            coefficients.setNlCoef2(Double.parseDouble(getInfo(8)));
            coefficients.setNlCoef3(Double.parseDouble(getInfo(9)));
            coefficients.setNlCoef4(Double.parseDouble(getInfo(10)));
            coefficients.setNlCoef5(Double.parseDouble(getInfo(11)));
            coefficients.setNlCoef6(Double.parseDouble(getInfo(12)));
            coefficients.setNlCoef7(Double.parseDouble(getInfo(13)));
            coefficients.setNlOrder(Integer.parseInt(getInfo(14)));
        } catch (NumberFormatException nfe) {
            System.out
                    .println("ERROR: cannot read nonlinearity coefficients from spectrometer. Setting to zero.");

            System.out.println("Values were: [" + getInfo(6)
                    + "] " + "[" + getInfo(7) + "] " + "["
                    + getInfo(8) + "] " + "["
                    + getInfo(9) + "] " + "["
                    + getInfo(10) + "] " + "["
                    + getInfo(11) + "] " + "["
                    + getInfo(12) + "] " + "["
                    + getInfo(13) + "] " + "["
                    + getInfo(14) + "]");

            coefficients.setNlCoef0(0.0D);
            coefficients.setNlCoef1(0.0D);
            coefficients.setNlCoef2(0.0D);
            coefficients.setNlCoef3(0.0D);
            coefficients.setNlCoef4(0.0D);
            coefficients.setNlCoef5(0.0D);
            coefficients.setNlCoef6(0.0D);
            coefficients.setNlCoef7(0.0D);
            coefficients.setNlOrder(0);
        }
    }

    private void getConfigurationFromSpectrometer() throws IOException {
        configuration = new Configuration(this);
        configuration.getConfigurationFromSpectrometer();
    }

    private byte[] getInfoBytes(int slot) throws IOException {
        synchronized (this.in) {
            synchronized (this.out) {
                byte[] byteArray = new byte[15];
                this.out[0] = 5;
                this.out[1] = ByteRoutines.getLowByte(ByteRoutines.getLowWord(slot));
                bulkOut(this.dataOutEndPoint, this.out, 2);
                bulkIn(this.lowSpeedInEndPoint, this.in, 17);
                for (int i = 0; i < 15; i++) {
                    byteArray[i] = this.in[(i + 2)];
                }
                return byteArray;
            }
        }
    }

    public void setInfoBytes(int slot, byte[] byteArray) throws IOException {
        clearOutBuffer();
        synchronized (this.out) {
            this.out[0] = 6;
            this.out[1] = ByteRoutines.getLowByte(ByteRoutines.getLowWord(slot));
            if (byteArray.length > 15) {
                throw new IOException("Byte array longer than 15 bytes");
            }
            for (int i = 0; i < byteArray.length; i++) {
                this.out[(i + 2)] = byteArray[i];
            }
            bulkOut(this.dataOutEndPoint, this.out, 17);
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                throw new IOException(
                        "EEPROM write might not have completed. Please verify.");
            }
        }
    }

    public void setPowerState(boolean power) throws IOException {
        synchronized (this.out) {
            int sd = !power ? 0 : 1;
            this.out[0] = 4;
            this.out[1] = ByteRoutines.getLowByte(ByteRoutines.getLowWord(sd));
            this.out[2] = ByteRoutines.getHighByte(ByteRoutines.getLowWord(sd));
            bulkOut(this.dataOutEndPoint, this.out, 5);
        }
    }

    public void setBoxcarWidth(int boxcarWindowSize) {
        this.smoothingWindowSize = boxcarWindowSize;
    }

    public int getBoxcarWidth() {
        return this.smoothingWindowSize;
    }

    public void setCorrectForElectricalDark(boolean correctForElectricalDark,
                                            boolean compatabilityMode) {
        this.correctForElectricalDark = correctForElectricalDark;
    }

    public boolean getCorrectForElectricalDark(int spectrometerIndex,
                                               int channelIndex) {
        return this.correctForElectricalDark;
    }

    public boolean setCorrectForDetectorNonlinearity(int enable) {
        if (enable != 0) {
            if (!hasNonlinearityCorrectionCoefficients()) {
                return false;
            }
        }

        setInnerCorrectForDetectorNonlinearity(enable != 0);
        return true;
    }

    public void setInnerCorrectForDetectorNonlinearity(boolean correctForDetectorNonlinearity) {
        this.correctForDetectorNonlinearity = correctForDetectorNonlinearity;
    }

    public boolean getCorrectForDetectorNonlinearity(int spectrometerIndex,
                                                     int channelIndex) {
        return this.correctForDetectorNonlinearity;
    }

    public boolean hasNonlinearityCorrectionCoefficients() {
        double sum = this.coefficients.getNlCoef0();
        sum += this.coefficients.getNlCoef1();
        sum += this.coefficients.getNlCoef2();
        sum += this.coefficients.getNlCoef3();
        sum += this.coefficients.getNlCoef4();
        if (this.coefficients.getNlOrder() == 7) {
            sum += this.coefficients.getNlCoef5();
            sum += this.coefficients.getNlCoef6();
            sum += this.coefficients.getNlCoef7();
        }
        if (sum == 0.0D) {
            return false;
        }
        return true;
    }

    public boolean setEEPromInfo(int slot, String str)  throws IOException {
        if (slot < 0) {
            return false;
        }
        setInfo(slot, str);
        return true;
    }

    public Bench getBench() {
        return getConfiguration().getBench();
    }

    public int getSpectrometerConfigSlot() {
        return this.spectrometerConfigSlot;
    }

    public int getBenchSlot() {
        return this.benchSlot;
    }

    public Detector getDetector() {
        return getConfiguration().getDetector();
    }

    public int getDetectorSerialNumberSlot() {
        return this.detectorSerialNumberSlot;
    }

    private Configuration getConfiguration() {
        return this.configuration;
    }

    public AutonullingConfiguration getAutonullingConfiguration() {
        return autonullingConfiguration;
    }
}
