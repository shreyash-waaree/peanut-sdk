package org.apache.commons.net;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.Charset;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/apache-common-net.jar:org/apache/commons/net/DatagramSocketClient.class */
public abstract class DatagramSocketClient {
    private static final DatagramSocketFactory DEFAULT_SOCKET_FACTORY = new DefaultDatagramSocketFactory();
    private Charset charset = Charset.defaultCharset();
    protected DatagramSocket _socket_ = null;
    protected int _timeout_ = 0;
    protected boolean _isOpen_ = false;
    protected DatagramSocketFactory _socketFactory_ = DEFAULT_SOCKET_FACTORY;

    public void close() {
        if (this._socket_ != null) {
            this._socket_.close();
        }
        this._socket_ = null;
        this._isOpen_ = false;
    }

    public Charset getCharset() {
        return this.charset;
    }

    @Deprecated
    public String getCharsetName() {
        return this.charset.name();
    }

    public int getDefaultTimeout() {
        return this._timeout_;
    }

    public InetAddress getLocalAddress() {
        return this._socket_.getLocalAddress();
    }

    public int getLocalPort() {
        return this._socket_.getLocalPort();
    }

    public int getSoTimeout() throws SocketException {
        return this._socket_.getSoTimeout();
    }

    public boolean isOpen() {
        return this._isOpen_;
    }

    public void open() throws SocketException {
        this._socket_ = this._socketFactory_.createDatagramSocket();
        this._socket_.setSoTimeout(this._timeout_);
        this._isOpen_ = true;
    }

    public void open(int port) throws SocketException {
        this._socket_ = this._socketFactory_.createDatagramSocket(port);
        this._socket_.setSoTimeout(this._timeout_);
        this._isOpen_ = true;
    }

    public void open(int port, InetAddress laddr) throws SocketException {
        this._socket_ = this._socketFactory_.createDatagramSocket(port, laddr);
        this._socket_.setSoTimeout(this._timeout_);
        this._isOpen_ = true;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void setDatagramSocketFactory(DatagramSocketFactory factory) {
        if (factory == null) {
            this._socketFactory_ = DEFAULT_SOCKET_FACTORY;
        } else {
            this._socketFactory_ = factory;
        }
    }

    public void setDefaultTimeout(int timeout) {
        this._timeout_ = timeout;
    }

    public void setSoTimeout(int timeout) throws SocketException {
        this._socket_.setSoTimeout(timeout);
    }
}
