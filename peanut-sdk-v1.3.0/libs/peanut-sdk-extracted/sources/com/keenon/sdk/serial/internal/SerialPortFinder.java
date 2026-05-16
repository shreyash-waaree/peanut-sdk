package com.keenon.sdk.serial.internal;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.Vector;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/internal/SerialPortFinder.class */
public class SerialPortFinder {
    private static final String TAG = "[SerialPort]";
    private Vector<Driver> mDrivers = null;

    Vector<Driver> getDrivers() throws IOException {
        if (this.mDrivers == null) {
            this.mDrivers = new Vector<>();
            LineNumberReader r = new LineNumberReader(new FileReader("/proc/tty/drivers"));
            while (true) {
                String l = r.readLine();
                if (l == null) {
                    break;
                }
                String drivername = l.substring(0, 21).trim();
                String[] w = l.split(" +");
                if (w.length >= 5 && w[w.length - 1].equals("serial")) {
                    LogUtils.i(PeanutConstants.TAG_SDK, "[SerialPort]Found new driver " + drivername + " on " + w[w.length - 4]);
                    this.mDrivers.add(new Driver(drivername, w[w.length - 4]));
                }
            }
            r.close();
        }
        return this.mDrivers;
    }

    public String[] getAllDevices() {
        Vector<String> devices = new Vector<>();
        try {
            for (Driver driver : getDrivers()) {
                Iterator<File> itdev = driver.getDevices().iterator();
                while (itdev.hasNext()) {
                    String device = itdev.next().getName();
                    String value = String.format("%s (%s)", device, driver.getName());
                    devices.add(value);
                }
            }
        } catch (IOException e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
        }
        return (String[]) devices.toArray(new String[devices.size()]);
    }

    public String[] getAllDevicesPath() {
        Vector<String> devices = new Vector<>();
        try {
            for (Driver driver : getDrivers()) {
                Iterator<File> itdev = driver.getDevices().iterator();
                while (itdev.hasNext()) {
                    String device = itdev.next().getAbsolutePath();
                    devices.add(device);
                }
            }
        } catch (IOException e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
        }
        return (String[]) devices.toArray(new String[devices.size()]);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/serial/internal/SerialPortFinder$Driver.class */
    public class Driver {
        Vector<File> mDevices = null;
        private String mDriverName;
        private String mDeviceRoot;

        public Driver(String name, String root) {
            this.mDriverName = name;
            this.mDeviceRoot = root;
        }

        public Vector<File> getDevices() {
            if (this.mDevices == null) {
                this.mDevices = new Vector<>();
                File dev = new File("/dev");
                File[] files = dev.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getAbsolutePath().startsWith(this.mDeviceRoot)) {
                        LogUtils.i(PeanutConstants.TAG_SDK, "[SerialPort]Found new device: " + files[i]);
                        this.mDevices.add(files[i]);
                    }
                }
            }
            return this.mDevices;
        }

        public String getName() {
            return this.mDriverName;
        }
    }
}
