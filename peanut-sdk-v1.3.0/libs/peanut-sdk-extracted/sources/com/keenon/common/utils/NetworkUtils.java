package com.keenon.common.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.webkit.WebView;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.sdk.constant.ApiConstants;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/NetworkUtils.class */
public class NetworkUtils {
    public static final String TAG = "[NetworkUtils]";
    public static final String CTWAP = "ctwap";
    public static final String CMWAP = "cmwap";
    public static final String WAP_3G = "3gwap";
    public static final String UNIWAP = "uniwap";
    public static final int APN_TYPE_DISABLED = -1;
    public static final int APN_TYPE_OTHER = 0;
    public static final int APN_TYPE_CM_CU_WAP = 1;
    public static final int APN_TYPE_CT_WAP = 2;
    public static final int APN_TYPE_NET = 3;
    public static Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");
    public static String USER_AGENT = "Mozilla/5.0 (Linux; U; Android " + Build.VERSION.RELEASE + "; Build/" + Build.ID + ")";
    private static Context context;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/NetworkUtils$ConnectionType.class */
    public static class ConnectionType {
        public static int Unknown = 0;
        public static int Ethernet = 1;
        public static int Wifi = 2;
        public static int Unknown_Generation = 3;
        public static int G2 = 4;
        public static int G3 = 5;
        public static int G4 = 6;
    }

    public static boolean isNetworkAvailable() {
        return getConnectedNetworkInfo() != null;
    }

    public static void init(Context app) {
        context = app;
    }

    public static int getNetworkType() {
        NetworkInfo networkInfo = getConnectedNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.getType();
        }
        return -1;
    }

    public static String getMake() {
        try {
            return URLEncoder.encode(Build.MANUFACTURER, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    public static String getIMEI(Context context2) throws SecurityException {
        TelephonyManager telephonyManager = (TelephonyManager) context2.getSystemService("phone");
        return telephonyManager.getDeviceId();
    }

    public static NetworkInfo getConnectedNetworkInfo() {
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService("connectivity");
            if (connectivity == null) {
                LogUtils.e(PeanutConstants.TAG_UTIL, "[NetworkUtils]couldn't get connectivity manager");
            } else {
                NetworkInfo info = connectivity.getActiveNetworkInfo();
                if (info != null) {
                    return info;
                }
            }
            return null;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return null;
        }
    }

    public static boolean isMobileNetwork(Context context2) {
        return 0 == getNetworkType();
    }

    public static boolean isWifiNetwork() {
        return 1 == getNetworkType();
    }

    public static int getConnectionType(Context context2) {
        if (isWifiNetwork()) {
            return ConnectionType.Wifi;
        }
        NetworkInfo networkInfo = getConnectedNetworkInfo();
        if (networkInfo == null) {
            return ConnectionType.Unknown;
        }
        int nType = networkInfo.getType();
        if (nType != 0) {
            return ConnectionType.Unknown;
        }
        try {
            Cursor c = context2.getContentResolver().query(PREFERRED_APN_URI, null, null, null, null);
            if (c != null) {
                c.moveToFirst();
                String user = c.getString(c.getColumnIndex("user"));
                if (!android.text.TextUtils.isEmpty(user)) {
                    LogUtils.d(PeanutConstants.TAG_UTIL, "[NetworkUtils]===>代理：" + c.getString(c.getColumnIndex("proxy")));
                    if (user.startsWith(CTWAP)) {
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[NetworkUtils]===>电信wap网络");
                        return ConnectionType.G2;
                    }
                }
            }
            c.close();
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
        }
        String extraInfo = networkInfo.getExtraInfo();
        LogUtils.d(PeanutConstants.TAG_UTIL, "[NetworkUtils]extraInfo:" + extraInfo);
        if (extraInfo != null) {
            String extraInfo2 = extraInfo.toLowerCase();
            if (extraInfo2.equals(CMWAP) || extraInfo2.equals(WAP_3G) || extraInfo2.equals(UNIWAP)) {
                LogUtils.d(PeanutConstants.TAG_UTIL, "[NetworkUtils] ======>移动联通wap网络");
                return ConnectionType.G2;
            }
        }
        LogUtils.d(PeanutConstants.TAG_UTIL, "[NetworkUtils] ======>net网络");
        return ConnectionType.G3;
    }

    public static int getAPNType(Context context2) {
        NetworkInfo networkInfo = getConnectedNetworkInfo();
        if (networkInfo == null) {
            return -1;
        }
        int nType = networkInfo.getType();
        if (nType != 0) {
            return 0;
        }
        try {
            Cursor c = context2.getContentResolver().query(PREFERRED_APN_URI, null, null, null, null);
            if (c != null) {
                c.moveToFirst();
                String user = c.getString(c.getColumnIndex("user"));
                if (!android.text.TextUtils.isEmpty(user)) {
                    LogUtils.d(PeanutConstants.TAG_UTIL, "[NetworkUtils]===>代理：" + c.getString(c.getColumnIndex("proxy")));
                    if (user.startsWith(CTWAP)) {
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[NetworkUtils]===>电信wap网络");
                        return 2;
                    }
                }
            }
            c.close();
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
        }
        String extraInfo = networkInfo.getExtraInfo();
        LogUtils.d(PeanutConstants.TAG_UTIL, "[NetworkUtils]extraInfo:" + extraInfo);
        if (extraInfo != null) {
            String extraInfo2 = extraInfo.toLowerCase();
            if (extraInfo2.equals(CMWAP) || extraInfo2.equals(WAP_3G) || extraInfo2.equals(UNIWAP)) {
                LogUtils.d(PeanutConstants.TAG_UTIL, "[NetworkUtils] ======>移动联通wap网络");
                return 1;
            }
        }
        LogUtils.d(PeanutConstants.TAG_UTIL, "[NetworkUtils] ======>net网络");
        return 3;
    }

    public static boolean is3GWAP(Context context2) {
        String extraInfo;
        try {
            NetworkInfo networkInfo = getConnectedNetworkInfo();
            if (networkInfo == null || (extraInfo = networkInfo.getExtraInfo()) == null) {
                return false;
            }
            if (WAP_3G.equalsIgnoreCase(extraInfo)) {
                return true;
            }
            if (UNIWAP.equalsIgnoreCase(extraInfo)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, "[NetworkUtils]CHECK 3GWAP ERROR");
            return false;
        }
    }

    public static String getWifiSSID(Context context2) {
        if (isWifiNetwork()) {
            try {
                WifiManager wifi = (WifiManager) context2.getSystemService("wifi");
                WifiInfo info = wifi.getConnectionInfo();
                return info.getSSID();
            } catch (Exception e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
                return null;
            }
        }
        return null;
    }

    public static boolean isNetworkRoaming() {
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService("connectivity");
            if (connectivity == null) {
                LogUtils.e(PeanutConstants.TAG_UTIL, "[NetworkUtils]couldn't get connectivity manager");
            } else {
                NetworkInfo info = connectivity.getActiveNetworkInfo();
                if (info != null && info.getType() == 0) {
                    try {
                        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
                        return telephonyManager.isNetworkRoaming();
                    } catch (Exception e) {
                        LogUtils.e(PeanutConstants.TAG_UTIL, "[NetworkUtils]isNetworkRoaming error: " + e.toString());
                    }
                }
            }
            return false;
        } catch (Exception e2) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e2);
            return false;
        }
    }

    public static String getEtherIp() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();
                if (intf != null && intf.getName() != null && intf.getName().equals("eth0")) {
                    Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                    while (enumIpAddr.hasMoreElements()) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                            LogUtils.i(PeanutConstants.TAG_UTIL, inetAddress.getHostAddress() + " ");
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
            return null;
        } catch (SocketException e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, (Exception) e);
            return null;
        }
    }

    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();
                Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return null;
        }
    }

    public static String getLine1Number() {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
            return telephonyManager.getLine1Number();
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, "[NetworkUtils]getLine1Number error: " + e.toString());
            return "";
        }
    }

    public static boolean isPortUsed(int port) {
        String[] cmds = {"netstat", "-an"};
        Process process = null;
        InputStream is = null;
        DataInputStream dis = null;
        boolean ret = false;
        try {
            try {
                Runtime runtime = Runtime.getRuntime();
                process = runtime.exec(cmds);
                is = process.getInputStream();
                dis = new DataInputStream(is);
                while (true) {
                    String line = dis.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.contains(":" + port)) {
                        ret = true;
                        break;
                    }
                }
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (Exception e) {
                        LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
                    }
                }
                if (is != null) {
                    is.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e2) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e2);
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (Exception e3) {
                        LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e3);
                    }
                }
                if (is != null) {
                    is.close();
                }
                if (process != null) {
                    process.destroy();
                }
            }
            return ret;
        } catch (Throwable th) {
            if (dis != null) {
                try {
                    dis.close();
                } catch (Exception e4) {
                    LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e4);
                    throw th;
                }
            }
            if (is != null) {
                is.close();
            }
            if (process != null) {
                process.destroy();
            }
            throw th;
        }
    }

    public static String getMacAddressPure() {
        String addr = getMacAddress();
        if (addr == null) {
            return "";
        }
        String addrNoColon = addr.replaceAll(":", "");
        String addrNoLine = addrNoColon.replaceAll("-", "");
        return addrNoLine.toUpperCase();
    }

    private static String getWifiMacAddress() {
        try {
            WifiManager wifi = (WifiManager) context.getSystemService("wifi");
            WifiInfo info = wifi.getConnectionInfo();
            return info.getMacAddress();
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return "";
        }
    }

    public static String getMacAddress() {
        String way = "default";
        String addr = getMacAddrInFile("/sys/class/net/eth0/address");
        if (android.text.TextUtils.isEmpty(addr) || "02:00:00:00:00:00".equals(addr)) {
            addr = getMacAddrInFile("/sys/class/net/wlan0/address");
        }
        if (android.text.TextUtils.isEmpty(addr) || "02:00:00:00:00:00".equals(addr)) {
            addr = getLocalEthernetMacAddress();
            way = "getLocalEthernetMacAddress";
        }
        if (android.text.TextUtils.isEmpty(addr) || "02:00:00:00:00:00".equals(addr)) {
            addr = getWifiMacAddress();
            way = "getWifiMacAddress";
        }
        if (android.text.TextUtils.isEmpty(addr) || "02:00:00:00:00:00".equals(addr)) {
            addr = getMacAddressByNetworkInterface();
            way = "getMacAddressByNetworkInterface";
        }
        if (!"default".equals(way)) {
            LogUtils.i(PeanutConstants.TAG_UTIL, "getMacAddress way:" + way);
        }
        return addr;
    }

    /* JADX WARN: Code restructure failed: missing block: B:11:0x002f, code lost:
    
        r4 = convertToMac(r0.getHardwareAddress());
     */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0038, code lost:
    
        if (r4 == null) goto L17;
     */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x0041, code lost:
    
        if (r4.startsWith("0:") == false) goto L17;
     */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0044, code lost:
    
        r4 = "0" + r4;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static java.lang.String getLocalEthernetMacAddress() {
        /*
            java.lang.String r0 = "02:00:00:00:00:00"
            r4 = r0
            java.util.Enumeration r0 = java.net.NetworkInterface.getNetworkInterfaces()     // Catch: java.net.SocketException -> L61
            r5 = r0
        L7:
            r0 = r5
            boolean r0 = r0.hasMoreElements()     // Catch: java.net.SocketException -> L61
            if (r0 == 0) goto L5e
            r0 = r5
            java.lang.Object r0 = r0.nextElement()     // Catch: java.net.SocketException -> L61
            java.net.NetworkInterface r0 = (java.net.NetworkInterface) r0     // Catch: java.net.SocketException -> L61
            r6 = r0
            r0 = r6
            java.lang.String r0 = r0.getDisplayName()     // Catch: java.net.SocketException -> L61
            r7 = r0
            r0 = r7
            if (r0 != 0) goto L26
            goto L7
        L26:
            java.lang.String r0 = "eth0"
            r1 = r7
            boolean r0 = r0.equals(r1)     // Catch: java.net.SocketException -> L61
            if (r0 == 0) goto L5b
            r0 = r6
            byte[] r0 = r0.getHardwareAddress()     // Catch: java.net.SocketException -> L61
            java.lang.String r0 = convertToMac(r0)     // Catch: java.net.SocketException -> L61
            r4 = r0
            r0 = r4
            if (r0 == 0) goto L5e
            r0 = r4
            java.lang.String r1 = "0:"
            boolean r0 = r0.startsWith(r1)     // Catch: java.net.SocketException -> L61
            if (r0 == 0) goto L5e
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.net.SocketException -> L61
            r1 = r0
            r1.<init>()     // Catch: java.net.SocketException -> L61
            java.lang.String r1 = "0"
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.net.SocketException -> L61
            r1 = r4
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.net.SocketException -> L61
            java.lang.String r0 = r0.toString()     // Catch: java.net.SocketException -> L61
            r4 = r0
            goto L5e
        L5b:
            goto L7
        L5e:
            goto L6a
        L61:
            r5 = move-exception
            java.lang.String r0 = "UTIL--"
            java.lang.String r1 = "[NetworkUtils]"
            r2 = r5
            com.keenon.common.utils.LogUtils.e(r0, r1, r2)
        L6a:
            r0 = r4
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.keenon.common.utils.NetworkUtils.getLocalEthernetMacAddress():java.lang.String");
    }

    private static String getMacAddressByNetworkInterface() {
        try {
            List<NetworkInterface> nis = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : nis) {
                if (ni.getName().equalsIgnoreCase("wlan0")) {
                    byte[] macBytes = ni.getHardwareAddress();
                    if (macBytes != null && macBytes.length > 0) {
                        StringBuilder res1 = new StringBuilder();
                        for (byte b : macBytes) {
                            res1.append(String.format("%02x:", Byte.valueOf(b)));
                        }
                        return res1.deleteCharAt(res1.length() - 1).toString();
                    }
                }
            }
            return "02:00:00:00:00:00";
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return "02:00:00:00:00:00";
        }
    }

    private static String getMacAddrInFile(String filepath) {
        File f = new File(filepath);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            BufferedReader rd = new BufferedReader(new InputStreamReader(fis));
            String str = rd.readLine().replaceAll(" ", "");
            String p = str.replaceAll("-", "");
            if (p.replaceAll(":", "").matches("0*")) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                }
                return null;
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e2) {
                }
            }
            return str;
        } catch (Exception e3) {
            if (fis == null) {
                return null;
            }
            try {
                fis.close();
                return null;
            } catch (IOException e4) {
                return null;
            }
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sbuf = new StringBuilder();
        for (byte b : bytes) {
            int intVal = b & 255;
            if (intVal < 16) {
                sbuf.append("0");
            }
            sbuf.append(Integer.toHexString(intVal).toUpperCase());
        }
        return sbuf.toString();
    }

    public static byte[] getUTF8Bytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    public static String loadFileAsString(String filename) throws IOException {
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), 1024);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            byte[] bytes = new byte[1024];
            boolean isUTF8 = false;
            int count = 0;
            while (true) {
                int read = is.read(bytes);
                if (read == -1) {
                    break;
                }
                if (count == 0 && bytes[0] == -17 && bytes[1] == -69 && bytes[2] == -65) {
                    isUTF8 = true;
                    baos.write(bytes, 3, read - 3);
                } else {
                    baos.write(bytes, 0, read);
                }
                count += read;
            }
            return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName == null || intf.getName().equalsIgnoreCase(interfaceName)) {
                    byte[] mac = intf.getHardwareAddress();
                    if (mac == null) {
                        return "";
                    }
                    StringBuilder buf = new StringBuilder();
                    for (byte b : mac) {
                        buf.append(String.format("%02X:", Byte.valueOf(b)));
                    }
                    if (buf.length() > 0) {
                        buf.deleteCharAt(buf.length() - 1);
                    }
                    return buf.toString();
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = addr instanceof Inet4Address;
                        if (useIPv4) {
                            if (isIPv4) {
                                return sAddr;
                            }
                        } else if (!isIPv4) {
                            int delim = sAddr.indexOf(37);
                            return delim < 0 ? sAddr : sAddr.substring(0, delim);
                        }
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    public static void initUserAgent(Context c) {
        String ua = null;
        try {
            WebView mes = new WebView(c);
            ua = mes.getSettings().getUserAgentString();
        } catch (Throwable e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, "init agent error:" + e.toString());
        }
        if (!android.text.TextUtils.isEmpty(ua)) {
            USER_AGENT = ua;
        } else {
            try {
                ua = System.getProperty("http.agent");
            } catch (Throwable e2) {
                LogUtils.e(PeanutConstants.TAG_UTIL, "init agent error2:" + e2.toString());
            }
            if (!android.text.TextUtils.isEmpty(ua)) {
                USER_AGENT = ua;
            }
        }
        LogUtils.e(PeanutConstants.TAG_UTIL, "UA:" + USER_AGENT);
    }

    public static String getFxMacAddress() {
        String macAddStr = getMacAddStr();
        if (!android.text.TextUtils.isEmpty(macAddStr)) {
            return macAddStr;
        }
        return getFxWifiMacAddr();
    }

    /* JADX WARN: Code restructure failed: missing block: B:14:0x0034, code lost:
    
        r4 = convertToMac(r0.getHardwareAddress());
     */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x003d, code lost:
    
        if (r4 == null) goto L20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x0046, code lost:
    
        if (r4.startsWith("0:") == false) goto L20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x0049, code lost:
    
        r4 = "0" + r4;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private static java.lang.String getMacAddStr() {
        /*
            r0 = 0
            r4 = r0
            java.util.Enumeration r0 = java.net.NetworkInterface.getNetworkInterfaces()     // Catch: java.net.SocketException -> L66
            r5 = r0
            r0 = r5
            if (r0 != 0) goto Lc
            r0 = 0
            return r0
        Lc:
            r0 = r5
            boolean r0 = r0.hasMoreElements()     // Catch: java.net.SocketException -> L66
            if (r0 == 0) goto L63
            r0 = r5
            java.lang.Object r0 = r0.nextElement()     // Catch: java.net.SocketException -> L66
            java.net.NetworkInterface r0 = (java.net.NetworkInterface) r0     // Catch: java.net.SocketException -> L66
            r6 = r0
            r0 = r6
            java.lang.String r0 = r0.getDisplayName()     // Catch: java.net.SocketException -> L66
            r7 = r0
            r0 = r7
            if (r0 != 0) goto L2b
            goto Lc
        L2b:
            r0 = r7
            java.lang.String r1 = "eth0"
            boolean r0 = r0.equals(r1)     // Catch: java.net.SocketException -> L66
            if (r0 == 0) goto L60
            r0 = r6
            byte[] r0 = r0.getHardwareAddress()     // Catch: java.net.SocketException -> L66
            java.lang.String r0 = convertToMac(r0)     // Catch: java.net.SocketException -> L66
            r4 = r0
            r0 = r4
            if (r0 == 0) goto L63
            r0 = r4
            java.lang.String r1 = "0:"
            boolean r0 = r0.startsWith(r1)     // Catch: java.net.SocketException -> L66
            if (r0 == 0) goto L63
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.net.SocketException -> L66
            r1 = r0
            r1.<init>()     // Catch: java.net.SocketException -> L66
            java.lang.String r1 = "0"
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.net.SocketException -> L66
            r1 = r4
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.net.SocketException -> L66
            java.lang.String r0 = r0.toString()     // Catch: java.net.SocketException -> L66
            r4 = r0
            goto L63
        L60:
            goto Lc
        L63:
            goto L6f
        L66:
            r5 = move-exception
            java.lang.String r0 = "UTIL--"
            java.lang.String r1 = "[NetworkUtils]"
            r2 = r5
            com.keenon.common.utils.LogUtils.e(r0, r1, r2)
        L6f:
            r0 = r4
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.keenon.common.utils.NetworkUtils.getMacAddStr():java.lang.String");
    }

    private static String getFxWifiMacAddr() {
        String wifiMacAddress = getWifiMacAddress();
        if (android.text.TextUtils.isEmpty(wifiMacAddress) || "02:00:00:00:00:00".equals(wifiMacAddress)) {
            return getNewMac();
        }
        return wifiMacAddress;
    }

    private static String getNewMac() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (nif.getName().equalsIgnoreCase("wlan0")) {
                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return null;
                    }
                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(String.format("%02X:", Byte.valueOf(b)));
                    }
                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            }
            return null;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
            return null;
        }
    }

    private static String convertToMac(byte[] mac) {
        if (mac == null) {
            return null;
        }
        int len = mac.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            byte b = mac[i];
            if (b >= 0 && b <= 16) {
                sb.append("0" + Integer.toHexString(b));
            } else if (b > 16) {
                sb.append(Integer.toHexString(b));
            } else {
                int value = ApiConstants.CODE_INVALID_POS + b;
                sb.append(Integer.toHexString(value));
            }
            if (i != mac.length - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/NetworkUtils$NetworkReceiverUtil.class */
    public static class NetworkReceiverUtil {
        private static final String TAG = "[NetworkReceiver]";

        public static void registerNetworkReceive(Context context, BroadcastReceiver networkReceiver) {
            if (context == null || networkReceiver == null) {
                return;
            }
            try {
                IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
                context.registerReceiver(networkReceiver, intentFilter);
            } catch (IllegalArgumentException e) {
                LogUtils.e(PeanutConstants.TAG_UTIL, TAG, (Exception) e);
            }
        }

        public static void unregisterNetworkReceive(Context context, BroadcastReceiver networkReceiver) {
            if (context == null || networkReceiver == null) {
                return;
            }
            if (networkReceiver != null) {
                try {
                    context.unregisterReceiver(networkReceiver);
                } catch (Exception e) {
                    LogUtils.e(PeanutConstants.TAG_UTIL, "[NetworkReceiver]unregister error: " + e.toString());
                }
            }
        }
    }
}
