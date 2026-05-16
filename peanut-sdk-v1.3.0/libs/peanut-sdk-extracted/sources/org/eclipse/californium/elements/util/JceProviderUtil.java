package org.eclipse.californium.elements.util;

import java.lang.reflect.Method;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import javax.crypto.Cipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/JceProviderUtil.class */
public class JceProviderUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(JceProviderUtil.class);
    private static volatile JceProviderUtil features;
    private static final String NET_I2P_CRYPTO_EDDSA = "net.i2p.crypto.eddsa";
    private static final String NET_I2P_CRYPTO_EDDSA_PROVIDER = "net.i2p.crypto.eddsa.EdDSASecurityProvider";
    private static final String BOUNCY_CASTLE_JCE_PROVIDER = "org.bouncycastle.jce.provider.BouncyCastleProvider";
    private static final String BOUNCY_CASTLE_JSSE_PROVIDER = "org.bouncycastle.jsse.provider.BouncyCastleJsseProvider";
    private static final String CALIFORNIUM_JCE_PROVIDER = "CALIFORNIUM_JCE_PROVIDER";
    private static final String JCE_PROVIDER_SYSTEM = "SYSTEM";
    private static final String JCE_PROVIDER_BOUNCY_CASTLE = "BC";
    private static final String JCE_PROVIDER_NET_I2P_CRYPTO = "I2P";
    private static final String JSSE_PROVIDER_BOUNCY_CASTLE = "BCJSSE";
    private static final String AES = "AES";
    private final boolean useBc;
    private final boolean rsa;
    private final boolean ec;
    private final boolean ed25519;
    private final boolean ed448;
    private final boolean strongEncryption;

    static {
        setupJce();
    }

    private static boolean isBouncyCastle(Provider provider) {
        return provider != null && provider.getName().equals(JCE_PROVIDER_BOUNCY_CASTLE);
    }

    private static void configureBouncyCastle(Provider provider) {
        if (isBouncyCastle(provider)) {
            configure(provider, "Alg.Alias.KeyFactory.OID.1.3.101.112", Asn1DerDecoder.ED25519);
            configure(provider, "Alg.Alias.KeyFactory.OID.1.3.101.113", Asn1DerDecoder.ED448);
        }
    }

    private static void configure(Provider provider, String key, String value) {
        String current = provider.getProperty(key);
        if (!value.equals(current)) {
            provider.setProperty(key, value);
        }
    }

    private static Provider loadProvider(String clzName) {
        try {
            Class<?> clz = Class.forName(clzName);
            Provider provider = (Provider) clz.getConstructor(new Class[0]).newInstance(new Object[0]);
            LOGGER.info("Loaded {}", clzName);
            return provider;
        } catch (Throwable e) {
            LOGGER.trace("Loading {} failed!", clzName, e);
            return null;
        }
    }

    private static void setupLoggingBridge() {
        try {
            Class<?> clz = Class.forName("org.slf4j.bridge.SLF4JBridgeHandler");
            Method method = clz.getMethod("removeHandlersForRootLogger", new Class[0]);
            method.invoke(null, new Object[0]);
            Method method2 = clz.getMethod("install", new Class[0]);
            method2.invoke(null, new Object[0]);
        } catch (Throwable e) {
            LOGGER.warn("Setup BC logging failed!", e);
        }
    }

    private static void setupJce() {
        Provider newProvider;
        boolean tryJce = true;
        boolean tryBc = false;
        boolean tryEd25519Java = true;
        String jce = StringUtil.getConfiguration(CALIFORNIUM_JCE_PROVIDER);
        if (jce != null && !jce.isEmpty()) {
            LOGGER.info("JCE setup: {}", jce);
            if (JCE_PROVIDER_SYSTEM.equalsIgnoreCase(jce)) {
                tryBc = false;
                tryEd25519Java = false;
            } else if (JCE_PROVIDER_BOUNCY_CASTLE.equalsIgnoreCase(jce)) {
                tryBc = true;
                tryJce = false;
                tryEd25519Java = false;
            } else if (JCE_PROVIDER_NET_I2P_CRYPTO.equalsIgnoreCase(jce)) {
                tryJce = false;
                tryBc = false;
            }
        }
        boolean found = false;
        Provider provider = null;
        try {
            KeyFactory factory = KeyFactory.getInstance(Asn1DerDecoder.EDDSA);
            provider = factory.getProvider();
            if (tryJce) {
                found = true;
                LOGGER.trace("EdDSA from default jce {}", provider.getName());
            }
        } catch (NoSuchAlgorithmException e) {
        }
        if (!found && tryBc) {
            if (isBouncyCastle(provider)) {
                found = true;
                LOGGER.trace("EdDSA from BC");
            } else {
                setupLoggingBridge();
                Provider newProvider2 = loadProvider(BOUNCY_CASTLE_JCE_PROVIDER);
                if (newProvider2 != null) {
                    try {
                        KeyFactory.getInstance(Asn1DerDecoder.EDDSA, newProvider2);
                        Security.removeProvider(newProvider2.getName());
                        Security.insertProviderAt(newProvider2, 1);
                        provider = newProvider2;
                        found = true;
                        LOGGER.trace("EdDSA from BC");
                    } catch (SecurityException e2) {
                    } catch (NoSuchAlgorithmException e3) {
                    }
                }
                if (found && Security.getProvider(JSSE_PROVIDER_BOUNCY_CASTLE) == null && (newProvider = loadProvider(BOUNCY_CASTLE_JSSE_PROVIDER)) != null) {
                    Security.setProperty("ssl.KeyManagerFactory.algorithm", "PKIX");
                    Security.setProperty("ssl.TrustManagerFactory.algorithm", "PKIX");
                    try {
                        Security.insertProviderAt(newProvider, 2);
                        LOGGER.info("TLS from BC");
                    } catch (SecurityException e4) {
                    }
                }
            }
        }
        if (!found && tryEd25519Java) {
            if (provider != null && provider.getClass().getName().equals(NET_I2P_CRYPTO_EDDSA_PROVIDER)) {
                found = true;
                LOGGER.trace("EdDSA from {}", NET_I2P_CRYPTO_EDDSA);
            } else {
                Provider newProvider3 = loadProvider(NET_I2P_CRYPTO_EDDSA_PROVIDER);
                if (newProvider3 != null) {
                    try {
                        KeyFactory.getInstance(Asn1DerDecoder.ED25519, newProvider3);
                        Security.removeProvider(newProvider3.getName());
                        Security.addProvider(newProvider3);
                        provider = newProvider3;
                        found = true;
                        LOGGER.trace("EdDSA from {}", NET_I2P_CRYPTO_EDDSA);
                    } catch (SecurityException e5) {
                    } catch (NoSuchAlgorithmException e6) {
                    }
                }
            }
        }
        boolean strongEncryption = false;
        try {
            strongEncryption = Cipher.getMaxAllowedKeyLength(AES) >= 256;
        } catch (NoSuchAlgorithmException e7) {
        }
        boolean ec = false;
        boolean rsa = false;
        try {
            KeyFactory.getInstance(Asn1DerDecoder.RSA);
            rsa = true;
        } catch (NoSuchAlgorithmException e8) {
        }
        try {
            KeyFactory.getInstance(Asn1DerDecoder.EC);
            ec = true;
        } catch (NoSuchAlgorithmException e9) {
        }
        LOGGER.debug("RSA: {}, EC: {}, strong encryption: {}", new Object[]{Boolean.valueOf(rsa), Boolean.valueOf(ec), Boolean.valueOf(strongEncryption)});
        boolean ed25519 = false;
        boolean ed448 = false;
        if (found && provider != null) {
            configureBouncyCastle(provider);
            try {
                KeyFactory.getInstance(Asn1DerDecoder.ED25519);
                ed25519 = true;
            } catch (NoSuchAlgorithmException e10) {
            }
            try {
                KeyFactory.getInstance(Asn1DerDecoder.ED448);
                ed448 = true;
            } catch (NoSuchAlgorithmException e11) {
            }
            LOGGER.debug("EdDSA supported by {}, Ed25519: {}, Ed448: {}", new Object[]{provider.getName(), Boolean.valueOf(ed25519), Boolean.valueOf(ed448)});
        } else {
            provider = null;
            LOGGER.debug("EdDSA not supported!");
        }
        JceProviderUtil newSupport = new JceProviderUtil(isBouncyCastle(provider), rsa, ec, ed25519, ed448, strongEncryption);
        if (!newSupport.equals(features)) {
            features = newSupport;
        }
    }

    public static void init() {
    }

    public static boolean usesBouncyCastle() {
        return features.useBc;
    }

    public static boolean hasStrongEncryption() {
        return features.strongEncryption;
    }

    public static boolean isSupported(String algorithm) {
        if (Asn1DerDecoder.EC.equalsIgnoreCase(algorithm)) {
            return features.ec;
        }
        if (Asn1DerDecoder.RSA.equalsIgnoreCase(algorithm)) {
            return features.rsa;
        }
        String oid = Asn1DerDecoder.getEdDsaStandardAlgorithmName(algorithm, null);
        if (Asn1DerDecoder.OID_ED25519.equals(oid)) {
            return features.ed25519;
        }
        if (Asn1DerDecoder.OID_ED448.equals(oid)) {
            return features.ed448;
        }
        if (Asn1DerDecoder.EDDSA.equalsIgnoreCase(algorithm)) {
            return features.ed25519 || features.ed448;
        }
        return false;
    }

    private JceProviderUtil(boolean useBc, boolean rsa, boolean ec, boolean ed25519, boolean ed448, boolean strongEncryption) {
        this.useBc = useBc;
        this.rsa = rsa;
        this.ec = ec;
        this.ed25519 = ed25519;
        this.ed448 = ed448;
        this.strongEncryption = strongEncryption;
    }

    public int hashCode() {
        int result = (31 * 1) + (this.ed25519 ? 41 : 37);
        return (31 * ((31 * ((31 * ((31 * ((31 * result) + (this.ed448 ? 41 : 37))) + (this.strongEncryption ? 41 : 37))) + (this.ec ? 41 : 37))) + (this.rsa ? 41 : 37))) + (this.useBc ? 41 : 37);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        JceProviderUtil other = (JceProviderUtil) obj;
        if (this.ed25519 != other.ed25519 || this.ed448 != other.ed448 || this.strongEncryption != other.strongEncryption || this.ec != other.ec || this.rsa != other.rsa || this.useBc != other.useBc) {
            return false;
        }
        return true;
    }
}
