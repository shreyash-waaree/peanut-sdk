package com.keenon.sdk.proxy.adapter;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.base.HederaTransportProtocol;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/proxy/adapter/HederaUdpServer.class */
public class HederaUdpServer {
    private static final String TAG = "[HederaUdpServer]";
    private static HederaUdpServer mInstance;
    private DatagramSocket serverSocket;
    private InetAddress clientHost;
    private int clientPort;
    private UdpReceiveThread mReceiveThread;
    public volatile boolean exit = false;
    private HederaTransportProtocol transportProtocol;

    private HederaUdpServer() {
    }

    public static HederaUdpServer getInstance() {
        if (mInstance == null) {
            synchronized (HederaUdpServer.class) {
                if (mInstance == null) {
                    mInstance = new HederaUdpServer();
                }
            }
        }
        return mInstance;
    }

    public void setHederaTransportProtocol(HederaTransportProtocol transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    public void init(HederaTransportProtocol transportProtocol) {
        LogUtils.i(PeanutConstants.TAG_SDK, "[HederaUdpServer][init]");
        try {
            this.transportProtocol = transportProtocol;
            DatagramSocket serverSocket = new DatagramSocket((SocketAddress) null);
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(5683));
            setSocket(serverSocket);
            if (this.mReceiveThread == null) {
                this.exit = false;
                LogUtils.i(PeanutConstants.TAG_SDK, "[HederaUdpServer][init][new thread]");
                this.mReceiveThread = new UdpReceiveThread("UdpProxy#");
                this.mReceiveThread.start();
            }
        } catch (SocketException e) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
        }
    }

    public void close() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[HederaUdpServer][close]");
        this.exit = true;
        this.mReceiveThread = null;
        if (this.serverSocket != null) {
            this.serverSocket.close();
        }
    }

    public boolean isCoapProtocal(Byte[] data) {
        return data[0].byteValue() != 0;
    }

    public void sendResponse(Byte[] data, int size) {
        LogUtils.v(PeanutConstants.TAG_SDK, "[HederaUdpServer][sendResponse][received --->: " + size + " Bytes]");
        if (isCoapProtocal(data)) {
            DatagramPacket response = new DatagramPacket(ByteUtils.ObjectToByte(data), size, this.clientHost, this.clientPort);
            try {
                this.serverSocket.send(response);
                return;
            } catch (IOException e) {
                LogUtils.e(PeanutConstants.TAG_SDK, TAG, (Exception) e);
                return;
            }
        }
        LogUtils.w(PeanutConstants.TAG_SDK, "[HederaUdpServer][sendResponse][not coap protocol]");
    }

    public void setSocket(DatagramSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/proxy/adapter/HederaUdpServer$UdpReceiveThread.class */
    private class UdpReceiveThread extends Thread {
        UdpReceiveThread(String name) {
            super(name);
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            super.run();
            while (!HederaUdpServer.this.exit) {
                try {
                    DatagramPacket request = new DatagramPacket(new byte[2048], 2048);
                    HederaUdpServer.this.serverSocket.receive(request);
                    HederaUdpServer.this.clientHost = request.getAddress();
                    HederaUdpServer.this.clientPort = request.getPort();
                    if (HederaUdpServer.this.transportProtocol != null) {
                        LogUtils.v(PeanutConstants.TAG_SDK, "[HederaUdpServer][UdpReceiveThread send --->:  " + request.getLength() + " Bytes]");
                        HederaUdpServer.this.transportProtocol.sendData(ByteUtils.ByteToObject(request.getData(), request.getLength()));
                    }
                } catch (IOException e) {
                    LogUtils.e(PeanutConstants.TAG_SDK, HederaUdpServer.TAG, (Exception) e);
                }
            }
        }
    }
}
