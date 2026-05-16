package org.eclipse.californium.elements.config;

import java.util.concurrent.TimeUnit;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/TcpConfig.class */
public final class TcpConfig {
    public static final String MODULE = "TCP.";
    public static final int DEFAULT_TCP_CONNECTION_IDLE_TIMEOUT_IN_SECONDS = 10;
    public static final int DEFAULT_TCP_CONNECT_TIMEOUT_IN_SECONDS = 10;
    public static final int DEFAULT_TLS_HANDSHAKE_TIMEOUT_IN_SECONDS = 10;
    public static final TimeDefinition TCP_CONNECTION_IDLE_TIMEOUT = new TimeDefinition("TCP.CONNECTION_IDLE_TIMEOUT", "TCP connection idle timeout.", 10, TimeUnit.SECONDS);
    public static final TimeDefinition TCP_CONNECT_TIMEOUT = new TimeDefinition("TCP.CONNECT_TIMEOUT", "TCP connect timeout.", 10, TimeUnit.SECONDS);
    public static final IntegerDefinition TCP_WORKER_THREADS = new IntegerDefinition("TCP.WORKER_THREADS", "Number of TCP worker threads.", 1, 1);
    public static final TimeDefinition TLS_HANDSHAKE_TIMEOUT = new TimeDefinition("TCP.HANDSHAKE_TIMEOUT", "TLS handshake timeout.", 10, TimeUnit.SECONDS);
    public static final TimeDefinition TLS_SESSION_TIMEOUT = new TimeDefinition("TCP.SESSION_TIMEOUT", "TLS session timeout.", 1, TimeUnit.HOURS);
    public static final EnumDefinition<CertificateAuthenticationMode> TLS_CLIENT_AUTHENTICATION_MODE = new EnumDefinition<>("TCP.CLIENT_AUTHENTICATION_MODE", "TLS client authentication mode.", CertificateAuthenticationMode.WANTED, CertificateAuthenticationMode.values());
    public static final BooleanDefinition TLS_VERIFY_SERVER_CERTIFICATES_SUBJECT = new BooleanDefinition("TCP.VERIFY_SERVER_CERTIFICATES_SUBJECT", "TLS verifies the server certificate's subjects.", true);
    public static final Configuration.ModuleDefinitionsProvider DEFINITIONS = new Configuration.ModuleDefinitionsProvider() { // from class: org.eclipse.californium.elements.config.TcpConfig.1
        @Override // org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider
        public String getModule() {
            return TcpConfig.MODULE;
        }

        @Override // org.eclipse.californium.elements.config.Configuration.DefinitionsProvider
        public void applyDefinitions(Configuration config) {
            config.set(TcpConfig.TCP_WORKER_THREADS, 1);
            config.set(TcpConfig.TCP_CONNECTION_IDLE_TIMEOUT, 10, TimeUnit.SECONDS);
            config.set(TcpConfig.TCP_CONNECT_TIMEOUT, 10, TimeUnit.SECONDS);
            config.set(TcpConfig.TLS_HANDSHAKE_TIMEOUT, 10, TimeUnit.SECONDS);
            config.set(TcpConfig.TLS_SESSION_TIMEOUT, 1, TimeUnit.HOURS);
            config.set(TcpConfig.TLS_CLIENT_AUTHENTICATION_MODE, CertificateAuthenticationMode.WANTED);
            config.set(TcpConfig.TLS_VERIFY_SERVER_CERTIFICATES_SUBJECT, true);
        }
    };

    static {
        Configuration.addDefaultModule(DEFINITIONS);
    }

    public static void register() {
        SystemConfig.register();
    }
}
