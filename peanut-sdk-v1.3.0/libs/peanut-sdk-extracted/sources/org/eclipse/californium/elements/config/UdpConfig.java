package org.eclipse.californium.elements.config;

import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/UdpConfig.class */
public final class UdpConfig {
    public static final String MODULE = "UDP.";
    public static final IntegerDefinition UDP_RECEIVER_THREAD_COUNT = new IntegerDefinition("UDP.RECEIVER_THREAD_COUNT", "Number of UDP receiver threads.", 1, 0);
    public static final IntegerDefinition UDP_SENDER_THREAD_COUNT = new IntegerDefinition("UDP.SENDER_THREAD_COUNT", "Number of UDP sender threads.", 1, 0);
    public static final IntegerDefinition UDP_DATAGRAM_SIZE = new IntegerDefinition("UDP.DATAGRAM_SIZE", "Maxium size of UDP datagram.", 2048, 64);
    public static final IntegerDefinition UDP_RECEIVE_BUFFER_SIZE = new IntegerDefinition("UDP.RECEIVE_BUFFER_SIZE", "UDP receive-buffer size.", null, 64);
    public static final IntegerDefinition UDP_SEND_BUFFER_SIZE = new IntegerDefinition("UDP.SEND_BUFFER_SIZE", "UDP send-buffer size.", null, 64);
    public static final IntegerDefinition UDP_CONNECTOR_OUT_CAPACITY = new IntegerDefinition("UDP.CONNECTOR_OUT_CAPACITY", "Maximum number of pending outgoing messages.", Integer.MAX_VALUE, 32);
    public static final Configuration.ModuleDefinitionsProvider DEFINITIONS = new Configuration.ModuleDefinitionsProvider() { // from class: org.eclipse.californium.elements.config.UdpConfig.1
        @Override // org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider
        public String getModule() {
            return UdpConfig.MODULE;
        }

        @Override // org.eclipse.californium.elements.config.Configuration.DefinitionsProvider
        public void applyDefinitions(Configuration config) {
            int CORES = Runtime.getRuntime().availableProcessors();
            int THREADS = CORES > 3 ? 2 : 1;
            config.set(UdpConfig.UDP_RECEIVER_THREAD_COUNT, Integer.valueOf(THREADS));
            config.set(UdpConfig.UDP_SENDER_THREAD_COUNT, Integer.valueOf(THREADS));
            config.set(UdpConfig.UDP_DATAGRAM_SIZE, 2048);
            config.set(UdpConfig.UDP_RECEIVE_BUFFER_SIZE, null);
            config.set(UdpConfig.UDP_SEND_BUFFER_SIZE, null);
            config.set(UdpConfig.UDP_CONNECTOR_OUT_CAPACITY, Integer.MAX_VALUE);
        }
    };

    static {
        Configuration.addDefaultModule(DEFINITIONS);
    }

    public static void register() {
        SystemConfig.register();
    }
}
