package org.eclipse.californium.elements.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/NetworkInterfacesUtil.class */
public class NetworkInterfacesUtil {
    public static final int MAX_MTU = 65535;
    public static final int DEFAULT_IPV6_MTU = 1280;
    public static final int DEFAULT_IPV4_MTU = 576;
    private static int anyMtu;
    private static int ipv4Mtu;
    private static int ipv6Mtu;
    private static boolean anyIpv4;
    private static boolean anyIpv6;
    private static Inet4Address broadcastIpv4;
    private static Inet4Address multicastInterfaceIpv4;
    private static Inet6Address multicastInterfaceIpv6;
    private static NetworkInterface multicastInterface;
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkInterfacesUtil.class);
    private static final Set<InetAddress> broadcastAddresses = new HashSet();
    private static final Set<String> ipv6Scopes = new HashSet();

    private static synchronized void initialize() {
        Enumeration<NetworkInterface> interfaces;
        InetAddress address;
        if (anyMtu == 0) {
            clear();
            int mtu = 65535;
            int ipv4mtu = 65535;
            int ipv6mtu = 65535;
            Pattern filter = null;
            String regex = StringUtil.getConfiguration("COAP_NETWORK_INTERFACES");
            if (regex != null && !regex.isEmpty()) {
                filter = Pattern.compile(regex);
            }
            try {
                interfaces = NetworkInterface.getNetworkInterfaces();
            } catch (SocketException ex) {
                LOGGER.warn("discover the <any> interface failed!", ex);
                anyIpv4 = true;
                anyIpv6 = true;
            }
            if (interfaces == null) {
                throw new SocketException("Network interfaces not available!");
            }
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isUp() && !iface.isLoopback() && (filter == null || filter.matcher(iface.getName()).matches())) {
                    int ifaceMtu = iface.getMTU();
                    if (ifaceMtu > 0 && ifaceMtu < mtu) {
                        mtu = ifaceMtu;
                    }
                    if (iface.supportsMulticast() && (multicastInterfaceIpv4 == null || multicastInterfaceIpv6 == null || broadcastIpv4 == null)) {
                        Inet4Address broad4 = null;
                        Inet4Address link4 = null;
                        Inet4Address site4 = null;
                        Inet6Address link6 = null;
                        Inet6Address site6 = null;
                        int count = broadcastIpv4 != null ? 0 - 1 : 0;
                        if (multicastInterfaceIpv4 != null) {
                            count--;
                        }
                        if (multicastInterfaceIpv6 != null) {
                            count--;
                        }
                        Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
                        while (inetAddresses.hasMoreElements()) {
                            InetAddress address2 = inetAddresses.nextElement();
                            if (address2 instanceof Inet4Address) {
                                anyIpv4 = true;
                                if (ifaceMtu > 0 && ifaceMtu < ipv4mtu) {
                                    ipv4mtu = ifaceMtu;
                                }
                                if (site4 == null) {
                                    if (address2.isSiteLocalAddress()) {
                                        site4 = (Inet4Address) address2;
                                    } else if (link4 == null && address2.isLinkLocalAddress()) {
                                        link4 = (Inet4Address) address2;
                                    }
                                }
                            } else if (address2 instanceof Inet6Address) {
                                Inet6Address address6 = (Inet6Address) address2;
                                anyIpv6 = true;
                                if (ifaceMtu > 0 && ifaceMtu < ipv6mtu) {
                                    ipv6mtu = ifaceMtu;
                                }
                                if (site6 == null) {
                                    if (address2.isSiteLocalAddress()) {
                                        site6 = address6;
                                    } else if (link4 == null && address2.isLinkLocalAddress()) {
                                        link6 = address6;
                                    }
                                }
                                if (address6.getScopeId() > 0) {
                                    ipv6Scopes.add(iface.getName());
                                }
                            }
                        }
                        for (InterfaceAddress interfaceAddress : iface.getInterfaceAddresses()) {
                            InetAddress broadcast = interfaceAddress.getBroadcast();
                            if (broadcast != null && !broadcast.isAnyLocalAddress() && (address = interfaceAddress.getAddress()) != null && !address.equals(broadcast)) {
                                broadcastAddresses.add(broadcast);
                                LOGGER.debug("Found broadcast address {} - {}.", broadcast, iface.getName());
                                if (broad4 == null) {
                                    broad4 = (Inet4Address) broadcast;
                                    count++;
                                }
                            }
                        }
                        if (link4 != null || site4 != null) {
                            count++;
                        }
                        if (link6 != null || site6 != null) {
                            count++;
                        }
                        if (count > 0) {
                            multicastInterface = iface;
                            broadcastIpv4 = broad4;
                            multicastInterfaceIpv4 = site4 == null ? link4 : site4;
                            multicastInterfaceIpv6 = site6 == null ? link6 : site6;
                        }
                    } else {
                        Enumeration<InetAddress> inetAddresses2 = iface.getInetAddresses();
                        while (inetAddresses2.hasMoreElements()) {
                            InetAddress address3 = inetAddresses2.nextElement();
                            if (address3 instanceof Inet4Address) {
                                anyIpv4 = true;
                                if (ifaceMtu > 0 && ifaceMtu < ipv4mtu) {
                                    ipv4mtu = ifaceMtu;
                                }
                            } else if (address3 instanceof Inet6Address) {
                                anyIpv6 = true;
                                if (ifaceMtu > 0 && ifaceMtu < ipv6mtu) {
                                    ipv6mtu = ifaceMtu;
                                }
                            }
                        }
                    }
                }
            }
            if (broadcastAddresses.isEmpty()) {
                LOGGER.info("no broadcast address found!");
            }
            if (ipv4mtu == 65535) {
                ipv4mtu = 576;
            }
            if (ipv6mtu == 65535) {
                ipv6mtu = 1280;
            }
            if (mtu == 65535) {
                mtu = Math.min(ipv4mtu, ipv6mtu);
            }
            ipv4Mtu = ipv4mtu;
            ipv6Mtu = ipv6mtu;
            anyMtu = mtu;
        }
    }

    public static synchronized void clear() {
        anyMtu = 0;
        ipv4Mtu = 0;
        ipv6Mtu = 0;
        anyIpv4 = false;
        anyIpv6 = false;
        ipv6Scopes.clear();
        broadcastAddresses.clear();
        broadcastIpv4 = null;
        multicastInterfaceIpv4 = null;
        multicastInterfaceIpv6 = null;
        multicastInterface = null;
    }

    public static int getAnyMtu() {
        initialize();
        return anyMtu;
    }

    public static int getIPv4Mtu() {
        initialize();
        return ipv4Mtu;
    }

    public static int getIPv6Mtu() {
        initialize();
        return ipv6Mtu;
    }

    public static boolean isAnyIpv4() {
        initialize();
        return anyIpv4;
    }

    public static boolean isAnyIpv6() {
        initialize();
        return anyIpv6;
    }

    public static Inet4Address getBroadcastIpv4() {
        initialize();
        return broadcastIpv4;
    }

    public static Inet4Address getMulticastInterfaceIpv4() {
        initialize();
        return multicastInterfaceIpv4;
    }

    public static Inet6Address getMulticastInterfaceIpv6() {
        initialize();
        return multicastInterfaceIpv6;
    }

    public static NetworkInterface getMulticastInterface() {
        initialize();
        return multicastInterface;
    }

    public static Collection<InetAddress> getNetworkInterfaces() {
        Enumeration<NetworkInterface> nets;
        Collection<InetAddress> interfaces = new LinkedList<>();
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            LOGGER.error("could not fetch all interface addresses", e);
        }
        if (nets == null) {
            throw new SocketException("Network interfaces not available!");
        }
        while (nets.hasMoreElements()) {
            NetworkInterface networkInterface = nets.nextElement();
            if (networkInterface.isUp()) {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    interfaces.add(inetAddresses.nextElement());
                }
            }
        }
        return interfaces;
    }

    public static Set<String> getIpv6Scopes() {
        initialize();
        return Collections.unmodifiableSet(ipv6Scopes);
    }

    public static boolean isBroadcastAddress(InetAddress address) {
        initialize();
        return broadcastAddresses.contains(address);
    }

    public static boolean isMultiAddress(InetAddress address) {
        initialize();
        return address != null && (address.isMulticastAddress() || broadcastAddresses.contains(address));
    }

    public static boolean equals(InetAddress address1, InetAddress address2) {
        return address1 == address2 || (address1 != null && address1.equals(address2));
    }

    public static boolean equals(SocketAddress address1, SocketAddress address2) {
        return address1 == address2 || (address1 != null && address1.equals(address2));
    }
}
