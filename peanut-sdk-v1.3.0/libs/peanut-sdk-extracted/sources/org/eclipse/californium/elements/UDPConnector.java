package org.eclipse.californium.elements;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.californium.elements.exception.EndpointMismatchException;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.ClockUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/UDPConnector.class */
public class UDPConnector implements Connector {
    public static final Logger LOGGER = LoggerFactory.getLogger(UDPConnector.class);
    static final ThreadGroup ELEMENTS_THREAD_GROUP = new ThreadGroup("Californium/Elements");
    protected final InetSocketAddress localAddr;
    private final BlockingQueue<RawData> outgoing;
    private final int senderCount;
    private final int receiverCount;
    private final int receiverPacketSize;
    private final Integer configReceiveBufferSize;
    private final Integer configSendBufferSize;
    protected volatile boolean running;
    private volatile DatagramSocket socket;
    protected volatile InetSocketAddress effectiveAddr;
    private volatile EndpointContextMatcher endpointContextMatcher;
    private volatile RawDataChannel receiver;
    private Integer receiveBufferSize;
    private Integer sendBufferSize;
    private boolean reuseAddress;
    protected boolean multicast;
    private final List<Thread> receiverThreads = new LinkedList();
    private final List<Thread> senderThreads = new LinkedList();
    private final List<UdpMulticastConnector> multicastReceivers = new CopyOnWriteArrayList();

    static {
        ELEMENTS_THREAD_GROUP.setDaemon(false);
    }

    public UDPConnector(InetSocketAddress address, Configuration configuration) {
        if (address == null) {
            this.localAddr = new InetSocketAddress(0);
        } else {
            this.localAddr = address;
        }
        this.running = false;
        this.effectiveAddr = this.localAddr;
        this.outgoing = new LinkedBlockingQueue(((Integer) configuration.get(UdpConfig.UDP_CONNECTOR_OUT_CAPACITY)).intValue());
        this.receiverCount = ((Integer) configuration.get(UdpConfig.UDP_RECEIVER_THREAD_COUNT)).intValue();
        this.senderCount = ((Integer) configuration.get(UdpConfig.UDP_SENDER_THREAD_COUNT)).intValue();
        this.receiverPacketSize = ((Integer) configuration.get(UdpConfig.UDP_DATAGRAM_SIZE)).intValue();
        this.configReceiveBufferSize = (Integer) configuration.get(UdpConfig.UDP_RECEIVE_BUFFER_SIZE);
        this.configSendBufferSize = (Integer) configuration.get(UdpConfig.UDP_SEND_BUFFER_SIZE);
        this.receiveBufferSize = this.configReceiveBufferSize;
        this.sendBufferSize = this.configSendBufferSize;
    }

    @Override // org.eclipse.californium.elements.Connector
    public boolean isRunning() {
        return this.running;
    }

    @Override // org.eclipse.californium.elements.Connector
    public synchronized void start() throws IOException {
        if (this.running) {
            return;
        }
        for (UdpMulticastConnector multicastReceiver : this.multicastReceivers) {
            multicastReceiver.start();
        }
        DatagramSocket socket = new DatagramSocket((SocketAddress) null);
        socket.setReuseAddress(this.reuseAddress);
        socket.bind(this.localAddr);
        init(socket);
    }

    protected void init(DatagramSocket socket) throws IOException {
        this.socket = socket;
        this.effectiveAddr = (InetSocketAddress) socket.getLocalSocketAddress();
        if (this.configReceiveBufferSize != null) {
            socket.setReceiveBufferSize(this.configReceiveBufferSize.intValue());
        }
        this.receiveBufferSize = Integer.valueOf(socket.getReceiveBufferSize());
        if (this.configSendBufferSize != null) {
            socket.setSendBufferSize(this.configSendBufferSize.intValue());
        }
        this.sendBufferSize = Integer.valueOf(socket.getSendBufferSize());
        this.running = true;
        LOGGER.info("UDPConnector starts up {} sender threads and {} receiver threads", Integer.valueOf(this.senderCount), Integer.valueOf(this.receiverCount));
        for (int i = 0; i < this.receiverCount; i++) {
            this.receiverThreads.add(new Receiver("UDP-Receiver-" + this.localAddr + "[" + i + "]"));
        }
        if (!this.multicast) {
            for (int i2 = 0; i2 < this.senderCount; i2++) {
                this.senderThreads.add(new Sender("UDP-Sender-" + this.localAddr + "[" + i2 + "]"));
            }
        }
        for (Thread t : this.receiverThreads) {
            t.start();
        }
        for (Thread t2 : this.senderThreads) {
            t2.start();
        }
        LOGGER.info("UDPConnector listening on {}, recv buf = {}, send buf = {}, recv packet size = {}", new Object[]{this.effectiveAddr, this.receiveBufferSize, this.sendBufferSize, Integer.valueOf(this.receiverPacketSize)});
    }

    @Override // org.eclipse.californium.elements.Connector
    public void stop() {
        List<RawData> pending = new ArrayList<>(this.outgoing.size());
        synchronized (this) {
            if (this.running) {
                this.running = false;
                LOGGER.debug("UDPConnector on [{}] stopping ...", this.effectiveAddr);
                for (Connector receiver : this.multicastReceivers) {
                    receiver.stop();
                }
                Iterator<Thread> it = this.senderThreads.iterator();
                while (it.hasNext()) {
                    it.next().interrupt();
                }
                Iterator<Thread> it2 = this.receiverThreads.iterator();
                while (it2.hasNext()) {
                    it2.next().interrupt();
                }
                this.outgoing.drainTo(pending);
                if (this.socket != null) {
                    this.socket.close();
                    this.socket = null;
                }
                for (Thread t : this.senderThreads) {
                    t.interrupt();
                    try {
                        t.join(1000L);
                    } catch (InterruptedException e) {
                    }
                }
                this.senderThreads.clear();
                for (Thread t2 : this.receiverThreads) {
                    t2.interrupt();
                    try {
                        t2.join(1000L);
                    } catch (InterruptedException e2) {
                    }
                }
                this.receiverThreads.clear();
                LOGGER.debug("UDPConnector on [{}] has stopped.", this.effectiveAddr);
                for (RawData data : pending) {
                    notifyMsgAsInterrupted(data);
                }
            }
        }
    }

    @Override // org.eclipse.californium.elements.Connector
    public void destroy() {
        stop();
        for (Connector receiver : this.multicastReceivers) {
            receiver.destroy();
        }
        this.receiver = null;
    }

    @Override // org.eclipse.californium.elements.Connector
    public void send(RawData msg) {
        boolean running;
        if (msg == null) {
            throw new NullPointerException("Message must not be null");
        }
        if (this.multicast) {
            throw new IllegalStateException("Connector is a multicast receiver!");
        }
        boolean added = false;
        synchronized (this) {
            running = this.running;
            if (running) {
                added = this.outgoing.offer(msg);
            }
        }
        if (!running) {
            notifyMsgAsInterrupted(msg);
        } else if (!added) {
            msg.onError(new InterruptedIOException("Connector overloaded."));
        }
    }

    @Override // org.eclipse.californium.elements.Connector
    public void setRawDataReceiver(RawDataChannel receiver) {
        this.receiver = receiver;
        for (UdpMulticastConnector multicastReceiver : this.multicastReceivers) {
            multicastReceiver.setRawDataReceiver(receiver);
        }
    }

    @Override // org.eclipse.californium.elements.Connector
    public void setEndpointContextMatcher(EndpointContextMatcher matcher) {
        this.endpointContextMatcher = matcher;
        for (UdpMulticastConnector multicastReceiver : this.multicastReceivers) {
            multicastReceiver.setEndpointContextMatcher(matcher);
        }
    }

    public void addMulticastReceiver(UdpMulticastConnector multicastReceiver) {
        if (multicastReceiver == null) {
            throw new NullPointerException("Connector must not be null!");
        }
        if (!multicastReceiver.isMutlicastReceiver()) {
            throw new IllegalArgumentException("Connector is no valid multicast receiver!");
        }
        if (this.multicast) {
            throw new IllegalStateException("Connector itself is a multicast receiver!");
        }
        this.multicastReceivers.add(multicastReceiver);
        multicastReceiver.setRawDataReceiver(this.receiver);
    }

    public void removeMulticastReceiver(UdpMulticastConnector multicastReceiver) {
        if (this.multicastReceivers.remove(multicastReceiver)) {
            multicastReceiver.setRawDataReceiver(null);
        }
    }

    @Override // org.eclipse.californium.elements.Connector
    public InetSocketAddress getAddress() {
        return this.effectiveAddr;
    }

    private void notifyMsgAsInterrupted(RawData msg) {
        msg.onError(new InterruptedIOException("Connector is not running."));
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/UDPConnector$NetworkStageThread.class */
    private abstract class NetworkStageThread extends Thread {
        protected abstract void work() throws Exception;

        protected NetworkStageThread(String name) {
            super(UDPConnector.ELEMENTS_THREAD_GROUP, name);
            setDaemon(true);
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            UDPConnector.LOGGER.debug("Starting network stage thread [{}]", getName());
            while (UDPConnector.this.running) {
                try {
                    work();
                    if (!UDPConnector.this.running) {
                        UDPConnector.LOGGER.debug("Network stage thread [{}] was stopped successfully", getName());
                        return;
                    }
                    continue;
                } catch (InterruptedIOException t) {
                    UDPConnector.LOGGER.trace("Network stage thread [{}] was stopped successfully at:", getName(), t);
                } catch (IOException t2) {
                    if (UDPConnector.this.running) {
                        UDPConnector.LOGGER.error("Exception in network stage thread [{}]:", getName(), t2);
                    } else {
                        UDPConnector.LOGGER.trace("Network stage thread [{}] was stopped successfully at:", getName(), t2);
                    }
                } catch (InterruptedException t3) {
                    UDPConnector.LOGGER.trace("Network stage thread [{}] was stopped successfully at:", getName(), t3);
                } catch (Throwable t4) {
                    UDPConnector.LOGGER.error("Exception in network stage thread [{}]:", getName(), t4);
                }
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/UDPConnector$Receiver.class */
    private class Receiver extends NetworkStageThread {
        private final DatagramPacket datagram;
        private final int size;

        private Receiver(String name) {
            super(name);
            this.size = UDPConnector.this.receiverPacketSize + 1;
            this.datagram = new DatagramPacket(new byte[this.size], this.size);
        }

        @Override // org.eclipse.californium.elements.UDPConnector.NetworkStageThread
        protected void work() throws IOException {
            this.datagram.setLength(this.size);
            DatagramSocket currentSocket = UDPConnector.this.socket;
            if (currentSocket != null) {
                currentSocket.receive(this.datagram);
                UDPConnector.this.processDatagram(this.datagram);
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/UDPConnector$Sender.class */
    private class Sender extends NetworkStageThread {
        private final DatagramPacket datagram;

        private Sender(String name) {
            super(name);
            this.datagram = new DatagramPacket(Bytes.EMPTY, 0);
        }

        @Override // org.eclipse.californium.elements.UDPConnector.NetworkStageThread
        protected void work() throws InterruptedException {
            RawData raw = (RawData) UDPConnector.this.outgoing.take();
            EndpointContext destination = raw.getEndpointContext();
            InetSocketAddress destinationAddress = destination.getPeerAddress();
            EndpointContext connectionContext = new UdpEndpointContext(destinationAddress);
            EndpointContextMatcher endpointMatcher = UDPConnector.this.endpointContextMatcher;
            if (endpointMatcher != null && !endpointMatcher.isToBeSent(destination, connectionContext)) {
                UDPConnector.LOGGER.warn("UDPConnector ({}) drops {} bytes to {}", new Object[]{UDPConnector.this.effectiveAddr, Integer.valueOf(this.datagram.getLength()), StringUtil.toLog(destinationAddress)});
                raw.onError(new EndpointMismatchException("UDP sending"));
                return;
            }
            this.datagram.setData(raw.getBytes());
            this.datagram.setSocketAddress(destinationAddress);
            DatagramSocket currentSocket = UDPConnector.this.socket;
            if (currentSocket != null) {
                try {
                    raw.onContextEstablished(connectionContext);
                    currentSocket.send(this.datagram);
                    raw.onSent();
                } catch (IOException ex) {
                    raw.onError(ex);
                }
                UDPConnector.LOGGER.debug("UDPConnector ({}) sent {} bytes to {}", new Object[]{this, Integer.valueOf(this.datagram.getLength()), StringUtil.toLog(destinationAddress)});
                return;
            }
            raw.onError(new IOException("socket already closed!"));
        }
    }

    @Override // org.eclipse.californium.elements.Connector
    public void processDatagram(DatagramPacket datagram) {
        InetSocketAddress connector = this.effectiveAddr;
        RawDataChannel dataReceiver = this.receiver;
        if (datagram.getLength() > this.receiverPacketSize) {
            LOGGER.debug("UDPConnector ({}) received truncated UDP datagram from {}. Maximum size allowed {}. Discarding ...", new Object[]{connector, StringUtil.toLog(datagram.getSocketAddress()), Integer.valueOf(this.receiverPacketSize)});
            return;
        }
        if (dataReceiver == null) {
            LOGGER.debug("UDPConnector ({}) received UDP datagram from {} without receiver. Discarding ...", connector, StringUtil.toLog(datagram.getSocketAddress()));
            return;
        }
        long timestamp = ClockUtil.nanoRealtime();
        String local = StringUtil.toString(connector);
        if (this.multicast) {
            local = "mc/" + local;
        }
        LOGGER.debug("UDPConnector ({}) received {} bytes from {}", new Object[]{local, Integer.valueOf(datagram.getLength()), StringUtil.toLog(datagram.getSocketAddress())});
        byte[] bytes = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getLength());
        RawData msg = RawData.inbound(bytes, new UdpEndpointContext(new InetSocketAddress(datagram.getAddress(), datagram.getPort())), this.multicast, timestamp, connector);
        dataReceiver.receiveData(msg);
    }

    public boolean getReuseAddress() {
        return this.reuseAddress;
    }

    public void setReuseAddress(boolean enable) {
        this.reuseAddress = enable;
    }

    public Integer getReceiveBufferSize() {
        return this.receiveBufferSize;
    }

    public Integer getSendBufferSize() {
        return this.sendBufferSize;
    }

    public int getReceiverThreadCount() {
        return this.receiverCount;
    }

    public int getSenderThreadCount() {
        return this.senderCount;
    }

    public int getReceiverPacketSize() {
        return this.receiverPacketSize;
    }

    @Override // org.eclipse.californium.elements.Connector
    public String getProtocol() {
        return CoAP.PROTOCOL_UDP;
    }

    public String toString() {
        return getProtocol() + "-" + StringUtil.toString(getAddress());
    }
}
