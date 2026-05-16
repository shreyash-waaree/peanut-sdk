package org.eclipse.californium.core.network.interceptors;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.util.LeastRecentlyUsedCache;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/interceptors/AnonymizedOriginTracer.class */
public final class AnonymizedOriginTracer extends MessageInterceptorAdapter {
    private static final int ID_LENGTH = 6;
    private static final int INITIAL_CAPACITY = 1000;
    private static final int MAX_CAPACITY = 10000;
    private static final long DEFAULT_MESSAGE_FILTER_TIMEOUT_IN_SECONDS = 60;
    private static final Mac HMAC;
    private static final SecretKeySpec KEY;
    private final LeastRecentlyUsedCache<InetSocketAddress, String> currentTests;
    private final String scheme;
    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymizedOriginTracer.class);
    private static final long HOST_TIMEOUT_IN_SECONDS = 86400;
    private static final LeastRecentlyUsedCache<InetAddress, String> CLIENT_CACHE = new LeastRecentlyUsedCache<>(1000, 10000, HOST_TIMEOUT_IN_SECONDS);

    static {
        SecureRandom rng = new SecureRandom();
        byte[] rd = new byte[32];
        rng.nextBytes(rd);
        KEY = new SecretKeySpec(rd, "MAC");
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
        }
        HMAC = mac;
        CLIENT_CACHE.setEvictingOnReadAccess(true);
    }

    public AnonymizedOriginTracer(String scheme) {
        this(scheme, 60L);
    }

    public AnonymizedOriginTracer(String scheme, long filterTimeout) {
        this.currentTests = new LeastRecentlyUsedCache<>(1000, 10000, 60L);
        this.scheme = scheme;
        this.currentTests.setExpirationThreshold(filterTimeout);
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptorAdapter, org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveRequest(Request request) {
        log(request);
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptorAdapter, org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveEmptyMessage(EmptyMessage message) {
        if (message.getType() == CoAP.Type.CON) {
            log(message);
        }
    }

    public boolean log(Message message) {
        InetSocketAddress address = message.getSourceContext().getPeerAddress();
        synchronized (this.currentTests) {
            if (this.currentTests.get(address) != null) {
                return false;
            }
            this.currentTests.put(address, this.scheme);
            String id = getAnonymizedOrigin(address.getAddress());
            if (id != null) {
                if (this.scheme == null) {
                    LOGGER.trace("{}:{}", id, Integer.valueOf(address.getPort()));
                    return true;
                }
                LOGGER.trace("{}://{}:{}", new Object[]{this.scheme, id, Integer.valueOf(address.getPort())});
                return true;
            }
            return false;
        }
    }

    public static String getAnonymizedOrigin(InetAddress address) {
        String str;
        synchronized (CLIENT_CACHE) {
            String id = CLIENT_CACHE.get(address);
            if (id == null) {
                byte[] raw = (byte[]) address.getAddress().clone();
                try {
                    if (HMAC == null) {
                        byte[] mask = KEY.getEncoded();
                        for (int index = 0; index < raw.length; index++) {
                            int i = index;
                            raw[i] = (byte) (raw[i] ^ mask[index]);
                        }
                    } else {
                        HMAC.init(KEY);
                        raw = HMAC.doFinal(raw);
                    }
                } catch (InvalidKeyException e) {
                }
                id = StringUtil.byteArray2HexString(raw, (char) 0, 6);
                CLIENT_CACHE.put(address, id);
            }
            str = id;
        }
        return str;
    }
}
