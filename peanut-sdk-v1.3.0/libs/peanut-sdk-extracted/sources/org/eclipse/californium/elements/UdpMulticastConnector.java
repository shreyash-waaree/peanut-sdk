package org.eclipse.californium.elements;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.NetworkInterfacesUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/UdpMulticastConnector.class */
public class UdpMulticastConnector extends UDPConnector {
    public static final Logger LOGGER = LoggerFactory.getLogger(UdpMulticastConnector.class);
    private NetworkInterface outgoingInterface;
    private InetAddress outgoingAddress;
    private List<Join> groups;
    private boolean loopbackDisable;

    private UdpMulticastConnector(InetSocketAddress localSocketAddress, InetAddress outgoingAddress, NetworkInterface outgoingInterface, List<Join> groups, boolean multicastReceiver, Configuration configuration) {
        super(localSocketAddress, configuration);
        this.groups = new ArrayList();
        setReuseAddress(true);
        this.outgoingInterface = outgoingInterface;
        this.outgoingAddress = outgoingAddress;
        this.groups.addAll(groups);
        InetAddress localAddress = this.localAddr.getAddress();
        boolean noGroups = this.groups.isEmpty();
        if (NetworkInterfacesUtil.isBroadcastAddress(localAddress)) {
            if (multicastReceiver) {
                if (noGroups) {
                    this.multicast = true;
                    return;
                }
                throw new IllegalArgumentException("Broadcast and additional multicast addresses are not supported for multicast receiver function!");
            }
            return;
        }
        if (noGroups) {
            if (localAddress.isMulticastAddress()) {
                this.groups.add(new Join(localAddress));
            } else {
                throw new IllegalArgumentException("missing multicast address to join!");
            }
        }
        if (multicastReceiver) {
            if (this.groups.size() == 1) {
                this.multicast = true;
                this.effectiveAddr = new InetSocketAddress(this.groups.get(0).multicastGroup, this.localAddr.getPort());
                return;
            }
            throw new IllegalArgumentException("Multiple multicast addresses are nor supported for multicast receiver function!");
        }
    }

    public void setLoopbackMode(boolean disable) {
        this.loopbackDisable = disable;
    }

    public boolean isMutlicastReceiver() {
        return this.multicast;
    }

    @Override // org.eclipse.californium.elements.UDPConnector, org.eclipse.californium.elements.Connector
    public synchronized void start() throws IOException {
        if (this.running) {
            return;
        }
        InetAddress effectiveInterface = this.localAddr.getAddress();
        MulticastSocket socket = new MulticastSocket((SocketAddress) null);
        socket.setLoopbackMode(this.loopbackDisable);
        try {
            socket.bind(this.localAddr);
            LOGGER.info("socket {}, loopback mode {}", StringUtil.toString((InetSocketAddress) socket.getLocalSocketAddress()), Boolean.valueOf(socket.getLoopbackMode()));
            if (this.outgoingAddress != null && !this.outgoingAddress.isAnyLocalAddress()) {
                try {
                    socket.setInterface(this.outgoingAddress);
                    effectiveInterface = this.outgoingAddress;
                    LOGGER.info("interface {}", StringUtil.toString(this.outgoingAddress));
                } catch (SocketException ex) {
                    LOGGER.error("error: multicast set interface", ex);
                }
            } else if (this.outgoingInterface != null) {
                try {
                    socket.setNetworkInterface(this.outgoingInterface);
                    LOGGER.info("interface {}", this.outgoingInterface.getDisplayName());
                } catch (SocketException ex2) {
                    LOGGER.error("error: multicast set interface", ex2);
                }
            }
            for (Join join : this.groups) {
                try {
                    boolean supportJoinWithInterface = true;
                    if (join.networkInterface != null) {
                        try {
                            socket.joinGroup(new InetSocketAddress(join.multicastGroup, 0), join.networkInterface);
                            LOGGER.info("joined group {} with {}", StringUtil.toString(join.multicastGroup), join.networkInterface.getDisplayName());
                        } catch (UnsupportedOperationException e) {
                            supportJoinWithInterface = false;
                        }
                    }
                    if (!supportJoinWithInterface || join.networkInterface == null) {
                        socket.joinGroup(join.multicastGroup);
                        LOGGER.info("joined group {}", StringUtil.toString(join.multicastGroup));
                    }
                } catch (SocketException ex3) {
                    socket.close();
                    if (join.multicastGroup instanceof Inet4Address) {
                        if ((effectiveInterface.isAnyLocalAddress() && !NetworkInterfacesUtil.isAnyIpv4()) || (effectiveInterface instanceof Inet6Address)) {
                            throw new SocketException("IPv6 only interface doesn't support IPv4 multicast!");
                        }
                    } else if ((join.multicastGroup instanceof Inet6Address) && ((effectiveInterface.isAnyLocalAddress() && !NetworkInterfacesUtil.isAnyIpv6()) || (effectiveInterface instanceof Inet4Address))) {
                        throw new SocketException("IPv4 only interface doesn't support IPv6 multicast!");
                    }
                    throw ex3;
                }
            }
            init(socket);
            if (this.multicast && this.groups.size() == 1) {
                this.effectiveAddr = new InetSocketAddress(this.groups.get(0).multicastGroup, socket.getLocalPort());
            }
        } catch (BindException ex4) {
            socket.close();
            LOGGER.error("can't bind to {}", StringUtil.toString(this.localAddr));
            throw ex4;
        } catch (SocketException ex5) {
            socket.close();
            LOGGER.error("can't bind to {}", StringUtil.toString(this.localAddr));
            throw ex5;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/UdpMulticastConnector$Join.class */
    private static class Join {
        private final InetAddress multicastGroup;
        private final NetworkInterface networkInterface;

        private Join(InetAddress multicastGroup) {
            this.multicastGroup = multicastGroup;
            this.networkInterface = null;
        }

        private Join(InetAddress multicastGroup, NetworkInterface networkInterface) {
            this.multicastGroup = multicastGroup;
            this.networkInterface = networkInterface;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/UdpMulticastConnector$Builder.class */
    public static class Builder {
        private InetSocketAddress localSocketAddress;
        private InetAddress outgoingAddress;
        private NetworkInterface outgoingInterface;
        private List<Join> groups = new ArrayList();
        private boolean multicastReceiver;
        private Configuration configuration;

        public InetSocketAddress getLocalAddress() {
            return this.localSocketAddress;
        }

        public Builder setLocalPort(int port) {
            this.localSocketAddress = new InetSocketAddress(port);
            return this;
        }

        public Builder setLocalAddress(InetAddress localAddress, int port) {
            if (localAddress == null) {
                throw new NullPointerException("local address must not be null!");
            }
            this.localSocketAddress = new InetSocketAddress(localAddress, port);
            return this;
        }

        public Builder setLocalAddress(InetSocketAddress localSocketAddress) {
            if (localSocketAddress == null) {
                throw new NullPointerException("local socket address must not be null!");
            }
            this.localSocketAddress = localSocketAddress;
            return this;
        }

        public Builder setOutgoingMulticastInterface(InetAddress outgoingAddress) {
            this.outgoingAddress = outgoingAddress;
            this.outgoingInterface = null;
            return this;
        }

        public Builder setOutgoingMulticastInterface(NetworkInterface outgoingInterface) {
            this.outgoingAddress = null;
            this.outgoingInterface = outgoingInterface;
            return this;
        }

        public Builder addMulticastGroup(InetAddress multicastGroup) {
            this.groups.add(new Join(multicastGroup));
            return this;
        }

        public Builder addMulticastGroup(InetAddress multicastGroup, NetworkInterface networkInterface) {
            this.groups.add(new Join(multicastGroup, networkInterface));
            return this;
        }

        public Builder setMulticastReceiver(boolean enable) {
            this.multicastReceiver = enable;
            return this;
        }

        public Builder setConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public UdpMulticastConnector build() {
            if (this.configuration == null) {
                this.configuration = Configuration.getStandard();
            }
            return new UdpMulticastConnector(this.localSocketAddress, this.outgoingAddress, this.outgoingInterface, this.groups, this.multicastReceiver, this.configuration);
        }
    }
}
