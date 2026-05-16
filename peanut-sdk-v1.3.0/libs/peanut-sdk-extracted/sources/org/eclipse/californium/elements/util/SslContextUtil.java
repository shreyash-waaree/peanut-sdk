package org.eclipse.californium.elements.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import org.eclipse.californium.elements.util.Asn1DerDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SslContextUtil.class */
public class SslContextUtil {
    public static final String CLASSPATH_SCHEME = "classpath://";
    public static final String PARAMETER_SEPARATOR = "#";
    public static final String JKS_ENDING = ".jks";
    public static final String BKS_ENDING = ".bks";
    public static final String PKCS12_ENDING = ".p12";
    public static final String PEM_ENDING = ".pem";
    public static final String CRT_ENDING = ".crt";
    public static final String DEFAULT_ENDING = "*";
    public static final String JKS_TYPE = "JKS";
    public static final String BKS_TYPE = "BKS";
    public static final String PKCS12_TYPE = "PKCS12";
    public static final String DEFAULT_SSL_PROTOCOL = "TLSv1.2";
    private static final String SCHEME_DELIMITER = "://";
    private static final String DEFAULT_ALIAS = "californium";
    private static final TrustManager TRUST_ALL;
    public static final Logger LOGGER = LoggerFactory.getLogger(SslContextUtil.class);
    private static final Map<String, KeyStoreType> KEY_STORE_TYPES = new ConcurrentHashMap();
    private static final Map<String, InputStreamFactory> INPUT_STREAM_FACTORIES = new ConcurrentHashMap();
    private static final KeyManager ANONYMOUS = new AnonymousX509ExtendedKeyManager();

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SslContextUtil$InputStreamFactory.class */
    public interface InputStreamFactory {
        InputStream create(String str) throws IOException;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SslContextUtil$SimpleKeyStore.class */
    public interface SimpleKeyStore {
        Credentials load(InputStream inputStream) throws GeneralSecurityException, IOException;
    }

    static {
        TrustManager trustAll;
        JceProviderUtil.init();
        configureDefaults();
        try {
            trustAll = new X509ExtendedTrustAllManager();
        } catch (NoClassDefFoundError e) {
            trustAll = new X509TrustAllManager();
        }
        TRUST_ALL = trustAll;
    }

    public static Certificate[] loadTrustedCertificates(String trust) throws GeneralSecurityException, IOException {
        if (null == trust) {
            throw new NullPointerException("trust must be provided!");
        }
        String[] parameters = trust.split(PARAMETER_SEPARATOR, 3);
        if (1 == parameters.length) {
            KeyStoreType configuration = getKeyStoreTypeFromUri(parameters[0]);
            if (configuration.simpleStore != null) {
                return loadTrustedCertificates(parameters[0], null, null);
            }
        }
        if (3 != parameters.length) {
            throw new IllegalArgumentException("trust must comply the pattern <keystore#hexstorepwd#aliaspattern>");
        }
        return loadTrustedCertificates(parameters[0], parameters[2], StringUtil.hex2CharArray(parameters[1]));
    }

    public static Credentials loadCredentials(String credentials) throws GeneralSecurityException, IOException {
        if (null == credentials) {
            throw new NullPointerException("credentials must be provided!");
        }
        String[] parameters = credentials.split(PARAMETER_SEPARATOR, 4);
        if (1 == parameters.length) {
            KeyStoreType configuration = getKeyStoreTypeFromUri(parameters[0]);
            if (configuration.simpleStore != null) {
                return loadCredentials(parameters[0], null, null, null);
            }
        }
        if (4 != parameters.length) {
            throw new IllegalArgumentException("credentials must comply the pattern <keystore#hexstorepwd#hexkeypwd#alias>");
        }
        return loadCredentials(parameters[0], parameters[3], StringUtil.hex2CharArray(parameters[1]), StringUtil.hex2CharArray(parameters[2]));
    }

    public static TrustManager[] loadTrustManager(String keyStoreUri, String aliasPattern, char[] storePassword) throws GeneralSecurityException, IOException {
        Certificate[] trustedCertificates = loadTrustedCertificates(keyStoreUri, aliasPattern, storePassword);
        return createTrustManager("trusts", trustedCertificates);
    }

    public static KeyManager[] loadKeyManager(String keyStoreUri, String aliasPattern, char[] storePassword, char[] keyPassword) throws GeneralSecurityException, IOException {
        KeyStoreType configuration = getKeyStoreTypeFromUri(keyStoreUri);
        if (configuration.simpleStore != null) {
            Credentials credentials = loadSimpleKeyStore(keyStoreUri, configuration);
            if (credentials.privateKey == null) {
                throw new IllegalArgumentException("credentials missing! No private key found!");
            }
            if (credentials.chain == null) {
                throw new IllegalArgumentException("credentials missing! No certificate chain found!");
            }
            return createKeyManager(DEFAULT_ALIAS, credentials.privateKey, credentials.chain);
        }
        if (null == keyPassword) {
            throw new NullPointerException("keyPassword must be provided!");
        }
        KeyStore ks = loadKeyStore(keyStoreUri, storePassword, configuration);
        if (aliasPattern != null && !aliasPattern.isEmpty()) {
            boolean found = false;
            Pattern pattern = Pattern.compile(aliasPattern);
            KeyStore ksAlias = KeyStore.getInstance(ks.getType());
            ksAlias.load(null);
            Enumeration<String> e = ks.aliases();
            while (e.hasMoreElements()) {
                String alias = e.nextElement();
                Matcher matcher = pattern.matcher(alias);
                if (matcher.matches()) {
                    KeyStore.Entry entry = ks.getEntry(alias, new KeyStore.PasswordProtection(keyPassword));
                    if (null != entry) {
                        ksAlias.setEntry(alias, entry, new KeyStore.PasswordProtection(keyPassword));
                        found = true;
                    } else {
                        throw new GeneralSecurityException("key stores '" + keyStoreUri + "' doesn't contain credentials for '" + alias + "'");
                    }
                }
            }
            if (!found) {
                throw new GeneralSecurityException("no credentials found in '" + keyStoreUri + "' for '" + aliasPattern + "'!");
            }
            ks = ksAlias;
        }
        return createKeyManager(ks, keyPassword);
    }

    public static Certificate[] loadTrustedCertificates(String keyStoreUri, String aliasPattern, char[] storePassword) throws GeneralSecurityException, IOException {
        KeyStoreType configuration = getKeyStoreTypeFromUri(keyStoreUri);
        if (configuration.simpleStore != null) {
            Credentials credentials = loadSimpleKeyStore(keyStoreUri, configuration);
            if (credentials.trusts == null) {
                throw new IllegalArgumentException("no trusted x509 certificates found in '" + keyStoreUri + "'!");
            }
            return credentials.trusts;
        }
        KeyStore ks = loadKeyStore(keyStoreUri, storePassword, configuration);
        Pattern pattern = null;
        if (null != aliasPattern && !aliasPattern.isEmpty()) {
            pattern = Pattern.compile(aliasPattern);
        }
        List<Certificate> trustedCertificates = new ArrayList<>();
        Enumeration<String> e = ks.aliases();
        while (e.hasMoreElements()) {
            String alias = e.nextElement();
            if (null != pattern) {
                Matcher matcher = pattern.matcher(alias);
                if (!matcher.matches()) {
                }
            }
            Certificate certificate = ks.getCertificate(alias);
            if (!trustedCertificates.contains(certificate)) {
                trustedCertificates.add(certificate);
            }
        }
        if (trustedCertificates.isEmpty()) {
            throw new IllegalArgumentException("no trusted x509 certificates found in '" + keyStoreUri + "' for '" + aliasPattern + "'!");
        }
        return (Certificate[]) trustedCertificates.toArray(new Certificate[trustedCertificates.size()]);
    }

    public static Credentials loadCredentials(String keyStoreUri, String alias, char[] storePassword, char[] keyPassword) throws GeneralSecurityException, IOException {
        KeyStoreType configuration = getKeyStoreTypeFromUri(keyStoreUri);
        if (configuration.simpleStore != null) {
            Credentials credentials = loadSimpleKeyStore(keyStoreUri, configuration);
            if (credentials.getTrustedCertificates() != null) {
                try {
                    CertificateFactory factory = CertificateFactory.getInstance("X.509");
                    CertPath certPath = factory.generateCertPath(Arrays.asList(credentials.getTrustedCertificates()));
                    List<? extends Certificate> path = certPath.getCertificates();
                    X509Certificate[] x509Certificates = (X509Certificate[]) path.toArray(new X509Certificate[path.size()]);
                    credentials = new Credentials(null, null, x509Certificates);
                    throw new IncompleteCredentialsException(credentials, "credentials missing! No private key found!");
                } catch (GeneralSecurityException ex) {
                    LOGGER.warn("Load PEM {}:", keyStoreUri, ex);
                }
            }
            if (credentials.publicKey == null && credentials.privateKey == null) {
                throw new IllegalArgumentException("credentials missing! No keys found!");
            }
            if (credentials.privateKey == null) {
                throw new IncompleteCredentialsException(credentials, "credentials missing! No private key found!");
            }
            if (credentials.publicKey == null) {
                throw new IncompleteCredentialsException(credentials, "credentials missing! Neither certificate chain nor public key found!");
            }
            return credentials;
        }
        if (null == alias) {
            throw new NullPointerException("alias must be provided!");
        }
        if (alias.isEmpty()) {
            throw new IllegalArgumentException("alias must not be empty!");
        }
        if (null == keyPassword) {
            throw new NullPointerException("keyPassword must be provided!");
        }
        KeyStore ks = loadKeyStore(keyStoreUri, storePassword, configuration);
        if (ks.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
            KeyStore.Entry entry = ks.getEntry(alias, new KeyStore.PasswordProtection(keyPassword));
            if (entry instanceof KeyStore.PrivateKeyEntry) {
                KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) entry;
                Certificate[] chain = pkEntry.getCertificateChain();
                X509Certificate[] x509Chain = asX509Certificates(chain);
                return new Credentials(pkEntry.getPrivateKey(), null, x509Chain);
            }
        }
        throw new IllegalArgumentException("no credentials found for '" + alias + "' in '" + keyStoreUri + "'!");
    }

    public static PrivateKey loadPrivateKey(String keyStoreUri, String alias, char[] storePassword, char[] keyPassword) throws GeneralSecurityException, IOException {
        KeyStoreType configuration = getKeyStoreTypeFromUri(keyStoreUri);
        if (configuration.simpleStore != null) {
            Credentials credentials = loadSimpleKeyStore(keyStoreUri, configuration);
            if (credentials.privateKey != null) {
                return credentials.privateKey;
            }
        } else {
            if (null == alias) {
                throw new NullPointerException("alias must be provided!");
            }
            if (alias.isEmpty()) {
                throw new IllegalArgumentException("alias must not be empty!");
            }
            if (null == keyPassword) {
                throw new NullPointerException("keyPassword must be provided!");
            }
            KeyStore ks = loadKeyStore(keyStoreUri, storePassword, configuration);
            if (ks.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                KeyStore.Entry entry = ks.getEntry(alias, new KeyStore.PasswordProtection(keyPassword));
                if (entry instanceof KeyStore.PrivateKeyEntry) {
                    KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) entry;
                    return pkEntry.getPrivateKey();
                }
            }
        }
        throw new IllegalArgumentException("no private key found for '" + alias + "' in '" + keyStoreUri + "'!");
    }

    public static PublicKey loadPublicKey(String keyStoreUri, String alias, char[] storePassword) throws GeneralSecurityException, IOException {
        KeyStoreType configuration = getKeyStoreTypeFromUri(keyStoreUri);
        if (configuration.simpleStore != null) {
            Credentials credentials = loadSimpleKeyStore(keyStoreUri, configuration);
            if (credentials.publicKey == null) {
                throw new IllegalArgumentException("no public key found for '" + alias + "' in '" + keyStoreUri + "'!");
            }
            return credentials.publicKey;
        }
        if (null == alias) {
            throw new NullPointerException("alias must be provided!");
        }
        if (alias.isEmpty()) {
            throw new IllegalArgumentException("alias must not be empty!");
        }
        KeyStore ks = loadKeyStore(keyStoreUri, storePassword, configuration);
        Certificate[] chain = ks.getCertificateChain(alias);
        return chain[0].getPublicKey();
    }

    public static X509Certificate[] loadCertificateChain(String keyStoreUri, String alias, char[] storePassword) throws GeneralSecurityException, IOException {
        KeyStoreType configuration = getKeyStoreTypeFromUri(keyStoreUri);
        if (configuration.simpleStore != null) {
            Credentials credentials = loadSimpleKeyStore(keyStoreUri, configuration);
            if (credentials.chain == null) {
                throw new IllegalArgumentException("No certificate chain found!");
            }
            return credentials.chain;
        }
        if (null == alias) {
            throw new NullPointerException("alias must be provided!");
        }
        if (alias.isEmpty()) {
            throw new IllegalArgumentException("alias must not be empty!");
        }
        KeyStore ks = loadKeyStore(keyStoreUri, storePassword, configuration);
        Certificate[] chain = ks.getCertificateChain(alias);
        return asX509Certificates(chain);
    }

    public static void configureDefaults() {
        KEY_STORE_TYPES.clear();
        KEY_STORE_TYPES.put(JKS_ENDING, new KeyStoreType(JKS_TYPE));
        KEY_STORE_TYPES.put(BKS_ENDING, new KeyStoreType(BKS_TYPE));
        KEY_STORE_TYPES.put(PKCS12_ENDING, new KeyStoreType(PKCS12_TYPE));
        KeyStoreType simple = new KeyStoreType(new SimpleKeyStore() { // from class: org.eclipse.californium.elements.util.SslContextUtil.1
            @Override // org.eclipse.californium.elements.util.SslContextUtil.SimpleKeyStore
            public Credentials load(InputStream inputStream) throws GeneralSecurityException, IOException {
                return SslContextUtil.loadPemCredentials(inputStream);
            }
        });
        KEY_STORE_TYPES.put(PEM_ENDING, simple);
        KEY_STORE_TYPES.put(CRT_ENDING, simple);
        KEY_STORE_TYPES.put("*", new KeyStoreType(KeyStore.getDefaultType()));
        INPUT_STREAM_FACTORIES.clear();
        INPUT_STREAM_FACTORIES.put(CLASSPATH_SCHEME, new ClassLoaderInputStreamFactory());
    }

    public static KeyStoreType configure(String ending, KeyStoreType type) {
        if (ending == null) {
            throw new NullPointerException("ending must not be null!");
        }
        if (!ending.equals("*") && !ending.startsWith(".")) {
            throw new IllegalArgumentException("ending must start with \".\"!");
        }
        if (type == null) {
            throw new NullPointerException("key store type must not be null!");
        }
        return KEY_STORE_TYPES.put(ending.toLowerCase(), type);
    }

    public static InputStreamFactory configure(String scheme, InputStreamFactory streamFactory) {
        if (scheme == null) {
            throw new NullPointerException("scheme must not be null!");
        }
        if (!scheme.endsWith("://")) {
            throw new IllegalArgumentException("scheme must end with \"://\"!");
        }
        if (streamFactory == null) {
            throw new NullPointerException("stream factory must not be null!");
        }
        return INPUT_STREAM_FACTORIES.put(scheme.toLowerCase(), streamFactory);
    }

    public static boolean isAvailableFromUri(String keyStoreUri) {
        try {
            InputStream in = getInputStreamFromUri(keyStoreUri);
            if (in != null) {
                in.close();
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private static KeyStoreType getKeyStoreTypeFromUri(String uri) throws GeneralSecurityException {
        KeyStoreType type = null;
        if (!uri.equals("*")) {
            int lastPartIndex = uri.lastIndexOf(47);
            int endingIndex = uri.lastIndexOf(46);
            if (lastPartIndex < endingIndex) {
                String ending = uri.substring(endingIndex).toLowerCase();
                type = KEY_STORE_TYPES.get(ending);
            }
        }
        if (type == null) {
            type = KEY_STORE_TYPES.get("*");
        }
        if (type == null) {
            throw new GeneralSecurityException("no key store type for " + uri);
        }
        return type;
    }

    private static String getSchemeFromUri(String uri) {
        int schemeIndex = uri.indexOf("://");
        if (0 < schemeIndex) {
            return uri.substring(0, schemeIndex + "://".length()).toLowerCase();
        }
        return null;
    }

    private static InputStream getInputStreamFromUri(String keyStoreUri) throws IOException {
        if (null == keyStoreUri) {
            throw new NullPointerException("keyStoreUri must be provided!");
        }
        InputStream inStream = null;
        String scheme = getSchemeFromUri(keyStoreUri);
        if (scheme == null) {
            String errorMessage = null;
            File file = new File(keyStoreUri);
            if (!file.exists()) {
                errorMessage = " doesn't exists!";
            } else if (!file.isFile()) {
                errorMessage = " is not a file!";
            } else if (!file.canRead()) {
                errorMessage = " could not be read!";
            }
            if (errorMessage == null) {
                inStream = new FileInputStream(file);
            } else {
                throw new IOException("URI: " + keyStoreUri + ", file: " + file.getAbsolutePath() + errorMessage);
            }
        } else {
            InputStreamFactory streamFactory = INPUT_STREAM_FACTORIES.get(scheme);
            if (streamFactory != null) {
                inStream = streamFactory.create(keyStoreUri);
            }
        }
        if (inStream == null) {
            URL url = new URL(keyStoreUri);
            inStream = url.openStream();
        }
        return inStream;
    }

    private static KeyStore loadKeyStore(String keyStoreUri, char[] storePassword, KeyStoreType configuration) throws GeneralSecurityException, IOException {
        if (null == storePassword) {
            throw new NullPointerException("storePassword must be provided!");
        }
        InputStream inStream = getInputStreamFromUri(keyStoreUri);
        KeyStore keyStore = KeyStore.getInstance(configuration.type);
        try {
            try {
                keyStore.load(inStream, storePassword);
                inStream.close();
                return keyStore;
            } catch (IOException ex) {
                throw new IOException(ex + ", URI: " + keyStoreUri + ", type: " + configuration.type + ", " + keyStore.getProvider().getName());
            }
        } catch (Throwable th) {
            inStream.close();
            throw th;
        }
    }

    private static Credentials loadSimpleKeyStore(String keyStoreUri, KeyStoreType configuration) throws GeneralSecurityException, IOException {
        InputStream inputStream = getInputStreamFromUri(keyStoreUri);
        try {
            Credentials credentialsLoad = configuration.simpleStore.load(inputStream);
            inputStream.close();
            return credentialsLoad;
        } catch (Throwable th) {
            inputStream.close();
            throw th;
        }
    }

    public static Credentials loadPemCredentials(InputStream inputStream) throws GeneralSecurityException, IOException {
        PemReader reader = new PemReader(inputStream);
        try {
            Asn1DerDecoder.Keys keys = new Asn1DerDecoder.Keys();
            List<Certificate> certificatesList = new ArrayList<>();
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            while (true) {
                String tag = reader.readNextBegin();
                if (tag != null) {
                    byte[] decode = reader.readToEnd();
                    if (decode != null) {
                        if (tag.contains("CERTIFICATE")) {
                            certificatesList.add(factory.generateCertificate(new ByteArrayInputStream(decode)));
                        } else if (tag.contains("PRIVATE KEY")) {
                            Asn1DerDecoder.Keys read = Asn1DerDecoder.readPrivateKey(decode);
                            if (read == null) {
                                throw new GeneralSecurityException("private key type not supported!");
                            }
                            keys.add(read);
                        } else if (tag.contains("PUBLIC KEY")) {
                            PublicKey read2 = Asn1DerDecoder.readSubjectPublicKey(decode);
                            if (read2 == null) {
                                throw new GeneralSecurityException("public key type not supported!");
                            }
                            keys.setPublicKey(read2);
                        } else {
                            LOGGER.warn("{} not supported!", tag);
                        }
                    }
                } else {
                    if (keys.getPrivateKey() == null && keys.getPublicKey() == null) {
                        List<Certificate> unique = new ArrayList<>();
                        for (Certificate certificate : certificatesList) {
                            if (!unique.contains(certificate)) {
                                unique.add(certificate);
                            }
                        }
                        Certificate[] certificates = (Certificate[]) unique.toArray(new Certificate[unique.size()]);
                        Credentials credentials = new Credentials(certificates);
                        reader.close();
                        return credentials;
                    }
                    CertPath certPath = factory.generateCertPath(certificatesList);
                    List<? extends Certificate> path = certPath.getCertificates();
                    X509Certificate[] x509Certificates = (X509Certificate[]) path.toArray(new X509Certificate[path.size()]);
                    Credentials credentials2 = new Credentials(keys.getPrivateKey(), keys.getPublicKey(), x509Certificates);
                    reader.close();
                    return credentials2;
                }
            }
        } catch (Throwable th) {
            reader.close();
            throw th;
        }
    }

    public static X509Certificate[] asX509Certificates(Certificate[] certificates) {
        if (null == certificates || 0 == certificates.length) {
            throw new IllegalArgumentException("certificates missing!");
        }
        X509Certificate[] x509Certificates = new X509Certificate[certificates.length];
        for (int index = 0; certificates.length > index; index++) {
            if (null == certificates[index]) {
                throw new IllegalArgumentException("[" + index + "] is null!");
            }
            try {
                x509Certificates[index] = (X509Certificate) certificates[index];
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("[" + index + "] is not a x509 certificate! Instead it's a " + certificates[index].getClass().getName());
            }
        }
        return x509Certificates;
    }

    public static X509KeyManager getX509KeyManager(KeyManager[] keyManagers) {
        if (keyManagers == null) {
            throw new NullPointerException("Key managers must not be null!");
        }
        if (keyManagers.length == 0) {
            throw new IllegalArgumentException("Key managers must not be empty!");
        }
        for (KeyManager manager : keyManagers) {
            if (manager instanceof X509KeyManager) {
                return (X509KeyManager) manager;
            }
        }
        throw new IllegalArgumentException("Missing a X509KeyManager in key managers!");
    }

    public static void ensureUniqueCertificates(X509Certificate[] certificates) {
        Set<X509Certificate> set = new HashSet<>();
        for (X509Certificate certificate : certificates) {
            if (!set.add(certificate)) {
                throw new IllegalArgumentException("Truststore contains certificates duplicates with subject: " + certificate.getSubjectX500Principal());
            }
        }
    }

    public static SSLContext createSSLContext(String alias, PrivateKey privateKey, X509Certificate[] chain, Certificate[] trusts) throws GeneralSecurityException {
        return createSSLContext(alias, privateKey, chain, trusts, DEFAULT_SSL_PROTOCOL);
    }

    public static SSLContext createSSLContext(String alias, PrivateKey privateKey, X509Certificate[] chain, Certificate[] trusts, String protocol) throws GeneralSecurityException {
        if (null == alias) {
            alias = DEFAULT_ALIAS;
        }
        KeyManager[] keyManager = createKeyManager(alias, privateKey, chain);
        TrustManager[] trustManager = createTrustManager(alias, trusts);
        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(keyManager, trustManager, null);
        return sslContext;
    }

    public static String[] getWeakCipherSuites(SSLContext sslContext) {
        SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
        List<String> weakCipherSuites = new ArrayList<>();
        String[] enabledCipherSuites = sslParameters.getCipherSuites();
        for (String suite : enabledCipherSuites) {
            if (suite.contains("AES_128")) {
                weakCipherSuites.add(suite);
            }
        }
        if (!weakCipherSuites.isEmpty() && weakCipherSuites.size() < enabledCipherSuites.length) {
            return (String[]) weakCipherSuites.toArray(new String[weakCipherSuites.size()]);
        }
        return null;
    }

    public static KeyManager[] createKeyManager(String alias, PrivateKey privateKey, X509Certificate[] chain) throws GeneralSecurityException {
        if (null == privateKey) {
            throw new NullPointerException("private key must be provided!");
        }
        if (null == chain) {
            throw new NullPointerException("certificate chain must be provided!");
        }
        if (0 == chain.length) {
            throw new IllegalArgumentException("certificate chain must not be empty!");
        }
        if (null == alias) {
            alias = DEFAULT_ALIAS;
        }
        try {
            char[] key = "intern".toCharArray();
            KeyStoreType configuration = getKeyStoreTypeFromUri("*");
            KeyStore ks = KeyStore.getInstance(configuration.type);
            ks.load(null);
            ks.setKeyEntry(alias, privateKey, key, chain);
            return createKeyManager(ks, key);
        } catch (IOException e) {
            throw new GeneralSecurityException(e.getMessage());
        }
    }

    public static TrustManager[] createTrustManager(String alias, Certificate[] trusts) throws GeneralSecurityException {
        if (null == trusts) {
            throw new NullPointerException("trusted certificates must be provided!");
        }
        if (0 == trusts.length) {
            throw new IllegalArgumentException("trusted certificates must not be empty!");
        }
        if (null == alias) {
            alias = DEFAULT_ALIAS;
        }
        try {
            int index = 1;
            KeyStoreType configuration = getKeyStoreTypeFromUri("*");
            KeyStore ks = KeyStore.getInstance(configuration.type);
            ks.load(null);
            for (Certificate certificate : trusts) {
                ks.setCertificateEntry(alias + index, certificate);
                index++;
            }
            return createTrustManager(ks);
        } catch (IOException e) {
            throw new GeneralSecurityException(e.getMessage());
        }
    }

    public static KeyManager[] createAnonymousKeyManager() {
        return new KeyManager[]{ANONYMOUS};
    }

    @NotForAndroid
    public static TrustManager[] createTrustAllManager() {
        return new TrustManager[]{TRUST_ALL};
    }

    private static KeyManager[] createKeyManager(KeyStore store, char[] keyPassword) throws GeneralSecurityException {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(store, keyPassword);
        return kmf.getKeyManagers();
    }

    private static TrustManager[] createTrustManager(KeyStore store) throws GeneralSecurityException {
        String algorithm = Security.getProperty("ssl.TrustManagerFactory.algorithm");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
        tmf.init(store);
        return tmf.getTrustManagers();
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SslContextUtil$ClassLoaderInputStreamFactory.class */
    private static class ClassLoaderInputStreamFactory implements InputStreamFactory {
        private ClassLoaderInputStreamFactory() {
        }

        @Override // org.eclipse.californium.elements.util.SslContextUtil.InputStreamFactory
        public InputStream create(String uri) throws IOException {
            String resource = uri.substring(SslContextUtil.CLASSPATH_SCHEME.length());
            InputStream inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            if (null == inStream) {
                throw new IOException("'" + uri + "' not found!");
            }
            return inStream;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SslContextUtil$Credentials.class */
    public static class Credentials {
        private final PrivateKey privateKey;
        private final PublicKey publicKey;
        private final X509Certificate[] chain;
        private final Certificate[] trusts;

        public Credentials(PrivateKey privateKey, PublicKey publicKey, X509Certificate[] chain) {
            if (chain != null) {
                if (chain.length == 0) {
                    chain = null;
                } else if (publicKey != null) {
                    if (!publicKey.equals(chain[0].getPublicKey())) {
                        throw new IllegalArgumentException("public key doesn't match certificate!");
                    }
                } else {
                    publicKey = chain[0].getPublicKey();
                }
            }
            this.privateKey = privateKey;
            this.chain = chain;
            this.publicKey = publicKey;
            this.trusts = null;
        }

        public Credentials(Certificate[] trusts) {
            this.privateKey = null;
            this.publicKey = null;
            this.chain = null;
            this.trusts = trusts;
        }

        public PrivateKey getPrivateKey() {
            return this.privateKey;
        }

        public PublicKey getPublicKey() {
            return this.publicKey;
        }

        public X509Certificate[] getCertificateChain() {
            return this.chain;
        }

        public List<X509Certificate> getCertificateChainAsList() {
            if (this.chain == null) {
                return null;
            }
            return Arrays.asList(this.chain);
        }

        public Certificate[] getTrustedCertificates() {
            return this.trusts;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SslContextUtil$IncompleteCredentialsException.class */
    public static class IncompleteCredentialsException extends IllegalArgumentException {
        private static final long serialVersionUID = -53656;
        private final Credentials incompleteCredentials;

        public IncompleteCredentialsException(Credentials incompleteCredentials) {
            this.incompleteCredentials = incompleteCredentials;
        }

        public IncompleteCredentialsException(Credentials incompleteCredentials, String message) {
            super(message);
            this.incompleteCredentials = incompleteCredentials;
        }

        public IncompleteCredentialsException(Credentials incompleteCredentials, String message, Throwable cause) {
            super(message, cause);
            this.incompleteCredentials = incompleteCredentials;
        }

        public Credentials getIncompleteCredentials() {
            return this.incompleteCredentials;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SslContextUtil$KeyStoreType.class */
    public static class KeyStoreType {
        public final String type;
        public final SimpleKeyStore simpleStore;

        public KeyStoreType(String type) {
            if (type == null) {
                throw new NullPointerException("key store type must not be null!");
            }
            if (type.isEmpty()) {
                throw new IllegalArgumentException("key store type must not be empty!");
            }
            this.type = type;
            this.simpleStore = null;
        }

        public KeyStoreType(SimpleKeyStore simpleStore) {
            if (simpleStore == null) {
                throw new NullPointerException("simple key store must not be null!");
            }
            this.type = null;
            this.simpleStore = simpleStore;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SslContextUtil$AnonymousX509ExtendedKeyManager.class */
    private static class AnonymousX509ExtendedKeyManager extends X509ExtendedKeyManager {
        private AnonymousX509ExtendedKeyManager() {
        }

        @Override // javax.net.ssl.X509ExtendedKeyManager
        public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
            return null;
        }

        @Override // javax.net.ssl.X509ExtendedKeyManager
        public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
            return null;
        }

        @Override // javax.net.ssl.X509KeyManager
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override // javax.net.ssl.X509KeyManager
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override // javax.net.ssl.X509KeyManager
        public X509Certificate[] getCertificateChain(String alias) {
            return null;
        }

        @Override // javax.net.ssl.X509KeyManager
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override // javax.net.ssl.X509KeyManager
        public PrivateKey getPrivateKey(String alias) {
            return null;
        }

        @Override // javax.net.ssl.X509KeyManager
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return null;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SslContextUtil$X509TrustAllManager.class */
    private static class X509TrustAllManager implements X509TrustManager {
        private static final X509Certificate[] EMPTY = new X509Certificate[0];

        private X509TrustAllManager() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static void validateChain(X509Certificate[] chain, boolean client) throws CertificateException {
            if (chain != null && chain.length > 0) {
                SslContextUtil.LOGGER.debug("check certificate {} for {}", chain[0].getSubjectX500Principal(), client ? "client" : "server");
                if (!CertPathUtil.canBeUsedForAuthentication(chain[0], client)) {
                    SslContextUtil.LOGGER.debug("check certificate {} for {} failed on key-usage!", chain[0].getSubjectX500Principal(), client ? "client" : "server");
                    throw new CertificateException("Key usage not proper for " + (client ? "client" : "server"));
                }
                SslContextUtil.LOGGER.trace("check certificate {} for {} succeeded on key-usage!", chain[0].getSubjectX500Principal(), client ? "client" : "server");
                CertPath path = CertPathUtil.generateValidatableCertPath(Arrays.asList(chain), null);
                try {
                    CertPathUtil.validateCertificatePathWithIssuer(true, path, EMPTY);
                    Logger logger = SslContextUtil.LOGGER;
                    Object[] objArr = new Object[3];
                    objArr[0] = chain[0].getSubjectX500Principal();
                    objArr[1] = Integer.valueOf(chain.length);
                    objArr[2] = client ? "client" : "server";
                    logger.trace("check certificate {} [chain.length={}] for {} validated!", objArr);
                } catch (GeneralSecurityException e) {
                    Logger logger2 = SslContextUtil.LOGGER;
                    Object[] objArr2 = new Object[3];
                    objArr2[0] = chain[0].getSubjectX500Principal();
                    objArr2[1] = client ? "client" : "server";
                    objArr2[2] = e.getMessage();
                    logger2.debug("check certificate {} for {} failed on {}!", objArr2);
                    if (e instanceof CertificateException) {
                        throw ((CertificateException) e);
                    }
                    throw new CertificateException(e);
                }
            }
        }

        @Override // javax.net.ssl.X509TrustManager
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            validateChain(chain, true);
        }

        @Override // javax.net.ssl.X509TrustManager
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            validateChain(chain, false);
        }

        @Override // javax.net.ssl.X509TrustManager
        public X509Certificate[] getAcceptedIssuers() {
            return EMPTY;
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SslContextUtil$X509ExtendedTrustAllManager.class */
    @NotForAndroid
    private static class X509ExtendedTrustAllManager extends X509ExtendedTrustManager {
        private X509ExtendedTrustAllManager() {
        }

        @Override // javax.net.ssl.X509TrustManager
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            X509TrustAllManager.validateChain(chain, true);
        }

        @Override // javax.net.ssl.X509TrustManager
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            X509TrustAllManager.validateChain(chain, false);
        }

        @Override // javax.net.ssl.X509TrustManager
        public X509Certificate[] getAcceptedIssuers() {
            return X509TrustAllManager.EMPTY;
        }

        @Override // javax.net.ssl.X509ExtendedTrustManager
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            X509TrustAllManager.validateChain(chain, true);
        }

        @Override // javax.net.ssl.X509ExtendedTrustManager
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            X509TrustAllManager.validateChain(chain, true);
        }

        @Override // javax.net.ssl.X509ExtendedTrustManager
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            X509TrustAllManager.validateChain(chain, false);
        }

        @Override // javax.net.ssl.X509ExtendedTrustManager
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            X509TrustAllManager.validateChain(chain, false);
        }
    }
}
