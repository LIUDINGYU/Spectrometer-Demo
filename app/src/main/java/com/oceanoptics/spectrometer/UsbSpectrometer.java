package com.oceanoptics.spectrometer;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by cheng.mingming on 2016/11/21.
 */

public class UsbSpectrometer {
    private final int VendorID = 0x2457;
    private final int ProductId = 0x101E;

    private UsbManager usbManager;
    private Context context;
    private String permission;
    private UsbDevice usbDevice;
    UsbDeviceConnection connection;
    private UsbInterface spectrometerIntf;
    protected UsbEndpoint dataOutEndPoint;
    protected UsbEndpoint highSpeedInEndPoint1;
    protected UsbEndpoint lowSpeedInEndPoint;
    protected byte[] out;
    protected byte[] in;
    protected boolean timeoutOccurredFlag = false;
    private int timeoutMilliseconds = 0;

    public boolean setupContext(UsbManager usbManager, Context context, String permission) {
        this.usbManager = usbManager;
        this.context = context;
        this.permission = permission;

        boolean retValue = false;
        try {
            retValue = openDevice(0);
        } catch (IOException e) {
            retValue = false;
        }
        return retValue;
    }

    protected boolean openDevice(int index) throws IOException {
        if (usbManager == null) {
            return false;
        }
        HashMap<String,UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList == null) {
            return false;
        }
        for (UsbDevice device : deviceList.values()) {
            if (isOceanOpticsDevice(device)) {
                usbDevice = device;
                if (!setupEndpoint()) {
                    return false;
                }
                initialize();
                return true;
            }
        }
        return false;
    }

   
    private boolean isOceanOpticsDevice(UsbDevice device) {
        return (device.getVendorId() == VendorID && device.getProductId() == ProductId);
    }

    private boolean setupEndpoint() {
        connection = usbManager.openDevice(usbDevice);
        if (connection != null) {
        } else {
            return false;
        }

        spectrometerIntf = usbDevice.getInterface(0);
        connection.claimInterface(spectrometerIntf, true);
        dataOutEndPoint = spectrometerIntf.getEndpoint(0);
        out = new byte[dataOutEndPoint.getMaxPacketSize()];
        highSpeedInEndPoint1 = spectrometerIntf.getEndpoint(1);
        lowSpeedInEndPoint = spectrometerIntf.getEndpoint(3);
        in = new byte[lowSpeedInEndPoint.getMaxPacketSize()];
        return true;
    }

    protected void initialize() throws IOException {
    }

    protected int bulkIn(UsbEndpoint inEndpoint, byte[] inBuffer, int len) {
        return transfer(inEndpoint, inBuffer, len);
    }

    private int transfer(UsbEndpoint endpoint, byte[] buffer, int length) {
        if (connection != null) {
            return connection.bulkTransfer(endpoint, buffer, length, timeoutMilliseconds);
        } else {
            return -1;
        }
    }


    public String getUSBStringManufacturerName() {
        return usbDevice.getManufacturerName();
    }

    protected int bulkOut(UsbEndpoint outEndpoint, byte[] outBuffer, int len) {
        return transfer(outEndpoint, outBuffer, len);
    }

    public int setTimeout(int timeoutMilliseconds) {
        this.timeoutMilliseconds = timeoutMilliseconds;
        return timeoutMilliseconds;
    }

    public boolean isTimeout() {
        return this.timeoutOccurredFlag;
    }
}
