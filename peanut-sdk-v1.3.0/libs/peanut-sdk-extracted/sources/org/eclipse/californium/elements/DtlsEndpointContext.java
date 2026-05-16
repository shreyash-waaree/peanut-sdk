package org.eclipse.californium.elements;

import java.net.InetSocketAddress;
import java.security.Principal;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/DtlsEndpointContext.class */
public class DtlsEndpointContext extends MapBasedEndpointContext {
    public static final Definition<Bytes> KEY_SESSION_ID = new Definition<>("DTLS_SESSION_ID", Bytes.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<Integer> KEY_EPOCH = new Definition<>("DTLS_EPOCH", Integer.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<String> KEY_CIPHER = new Definition<>("DTLS_CIPHER", String.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<Long> KEY_HANDSHAKE_TIMESTAMP = new Definition<>("DTLS_HANDSHAKE_TIMESTAMP", Long.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<Bytes> KEY_READ_CONNECTION_ID = new Definition<>("DTLS_READ_CONNECTION_ID", Bytes.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<Bytes> KEY_WRITE_CONNECTION_ID = new Definition<>("DTLS_WRITE_CONNECTION_ID", Bytes.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<String> KEY_VIA_ROUTER = new Definition<>("*DTLS_VIA_ROUTER", String.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<String> KEY_HANDSHAKE_MODE = new Definition<>("*DTLS_HANDSHAKE_MODE", String.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<Integer> KEY_AUTO_HANDSHAKE_TIMEOUT = new Definition<>("*DTLS_AUTO_HANDSHAKE_TIMEOUT", Integer.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<Integer> KEY_MESSAGE_SIZE_LIMIT = new Definition<>("*DTLS_MESSAGE_SIZE_LIMIT", Integer.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<Boolean> KEY_EXTENDED_MASTER_SECRET = new Definition<>("*DTLS_EXTENDED_MASTER_SECRET", Boolean.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<Boolean> KEY_NEWEST_RECORD = new Definition<>("*DTLS_NEWEST_RECORD", Boolean.class, ATTRIBUTE_DEFINITIONS);
    public static final Definition<InetSocketAddress> KEY_PREVIOUS_ADDRESS = new Definition<>("*DTLS_PREVIOUS_ADDRESS", InetSocketAddress.class, ATTRIBUTE_DEFINITIONS);
    public static final String HANDSHAKE_MODE_NONE = "none";
    public static final MapBasedEndpointContext.Attributes ATTRIBUTE_HANDSHAKE_MODE_NONE = new MapBasedEndpointContext.Attributes().add(KEY_HANDSHAKE_MODE, HANDSHAKE_MODE_NONE).lock();
    public static final String HANDSHAKE_MODE_AUTO = "auto";
    public static final MapBasedEndpointContext.Attributes ATTRIBUTE_HANDSHAKE_MODE_AUTO = new MapBasedEndpointContext.Attributes().add(KEY_HANDSHAKE_MODE, HANDSHAKE_MODE_AUTO).lock();
    public static final String HANDSHAKE_MODE_PROBE = "probe";
    public static final MapBasedEndpointContext.Attributes ATTRIBUTE_HANDSHAKE_MODE_PROBE = new MapBasedEndpointContext.Attributes().add(KEY_HANDSHAKE_MODE, HANDSHAKE_MODE_PROBE).lock();
    public static final String HANDSHAKE_MODE_FORCE = "force";
    public static final MapBasedEndpointContext.Attributes ATTRIBUTE_HANDSHAKE_MODE_FORCE = new MapBasedEndpointContext.Attributes().add(KEY_HANDSHAKE_MODE, HANDSHAKE_MODE_FORCE).lock();
    public static final String HANDSHAKE_MODE_FORCE_FULL = "full";
    public static final MapBasedEndpointContext.Attributes ATTRIBUE_HANDSHAKE_MODE_FORCE_FULL = new MapBasedEndpointContext.Attributes().add(KEY_HANDSHAKE_MODE, HANDSHAKE_MODE_FORCE_FULL).lock();

    public DtlsEndpointContext(InetSocketAddress peerAddress, String virtualHost, Principal peerIdentity, Bytes sessionId, int epoch, String cipher, long timestamp) {
        super(peerAddress, virtualHost, peerIdentity, new MapBasedEndpointContext.Attributes().add(KEY_SESSION_ID, sessionId).add(KEY_CIPHER, cipher).add(KEY_EPOCH, Integer.valueOf(epoch)).add(KEY_HANDSHAKE_TIMESTAMP, Long.valueOf(timestamp)));
    }

    public DtlsEndpointContext(InetSocketAddress peerAddress, String virtualHost, Principal peerIdentity, Bytes sessionId, int epoch, String cipher, long timestamp, Bytes writeCid, Bytes readCid, String via) {
        super(peerAddress, virtualHost, peerIdentity, new MapBasedEndpointContext.Attributes().add(KEY_SESSION_ID, sessionId).add(KEY_CIPHER, cipher).add(KEY_EPOCH, Integer.valueOf(epoch)).add(KEY_HANDSHAKE_TIMESTAMP, Long.valueOf(timestamp)).add(KEY_WRITE_CONNECTION_ID, writeCid).add(KEY_READ_CONNECTION_ID, readCid).add(KEY_VIA_ROUTER, via));
    }

    public DtlsEndpointContext(InetSocketAddress peerAddress, String virtualHost, Principal peerIdentity, MapBasedEndpointContext.Attributes attributes) {
        super(peerAddress, virtualHost, peerIdentity, attributes);
    }

    public final Bytes getSessionId() {
        return (Bytes) get(KEY_SESSION_ID);
    }

    public final Number getEpoch() {
        return (Number) get(KEY_EPOCH);
    }

    public final String getCipher() {
        return (String) get(KEY_CIPHER);
    }

    public final Number getHandshakeTimestamp() {
        return (Number) get(KEY_HANDSHAKE_TIMESTAMP);
    }

    @Override // org.eclipse.californium.elements.MapBasedEndpointContext, org.eclipse.californium.elements.AddressEndpointContext
    public String toString() {
        return String.format("DTLS(%s,ID:%s)", getPeerAddressAsString(), StringUtil.trunc(getSessionId().getAsString(), 10));
    }
}
