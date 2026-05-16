package org.eclipse.californium.elements.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.security.auth.x500.X500Principal;
import org.eclipse.californium.core.coap.LinkFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/CertPathUtil.class */
public class CertPathUtil {
    private static final String TYPE_X509 = "X.509";
    private static final String SERVER_AUTHENTICATION = "1.3.6.1.5.5.7.3.1";
    private static final String CLIENT_AUTHENTICATION = "1.3.6.1.5.5.7.3.2";
    private static final int KEY_USAGE_SIGNATURE = 0;
    private static final int KEY_USAGE_CERTIFICATE_SIGNING = 5;
    private static final int SUBJECT_ALTERNATIVE_NAMES_DNS = 2;
    private static final int SUBJECT_ALTERNATIVE_NAMES_LITERAL_IP = 7;
    private static final Logger LOGGER = LoggerFactory.getLogger(CertPathUtil.class);
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s{2,}");

    public static boolean canBeUsedToVerifySignature(X509Certificate cert) {
        if (cert.getBasicConstraints() < 0) {
            LOGGER.debug("certificate: {}, not for CA!", cert.getSubjectX500Principal());
            return false;
        }
        if (cert.getKeyUsage() != null && !cert.getKeyUsage()[5]) {
            LOGGER.debug("certificate: {}, not for certificate signing!", cert.getSubjectX500Principal());
            return false;
        }
        return true;
    }

    public static boolean canBeUsedForAuthentication(X509Certificate cert, boolean client) {
        if (cert.getKeyUsage() != null && !cert.getKeyUsage()[0]) {
            LOGGER.debug("certificate: {}, not for signing!", cert.getSubjectX500Principal());
            return false;
        }
        try {
            List<String> list = cert.getExtendedKeyUsage();
            if (list != null && !list.isEmpty()) {
                LOGGER.trace("certificate: {}", cert.getSubjectX500Principal());
                String authentication = client ? CLIENT_AUTHENTICATION : SERVER_AUTHENTICATION;
                boolean foundUsage = false;
                for (String extension : list) {
                    LOGGER.trace("   extkeyusage {}", extension);
                    if (authentication.equals(extension)) {
                        foundUsage = true;
                    }
                }
                if (!foundUsage) {
                    LOGGER.debug("certificate: {}, not for {}!", cert.getSubjectX500Principal(), client ? "client" : "server");
                    return false;
                }
            } else {
                LOGGER.debug("certificate: {}, no extkeyusage!", cert.getSubjectX500Principal());
            }
            return true;
        } catch (CertificateParsingException e) {
            LOGGER.warn("x509 certificate:", e);
            return true;
        }
    }

    public static CertPath generateCertPath(List<X509Certificate> certificateChain) {
        if (certificateChain == null) {
            throw new NullPointerException("Certificate chain must not be null!");
        }
        return generateCertPath(certificateChain, certificateChain.size());
    }

    public static CertPath generateCertPath(List<X509Certificate> certificateChain, int size) {
        if (certificateChain == null) {
            throw new NullPointerException("Certificate chain must not be null!");
        }
        if (size > certificateChain.size()) {
            throw new IllegalArgumentException("size must not be larger then certificate chain!");
        }
        try {
            if (!certificateChain.isEmpty()) {
                int last = certificateChain.size() - 1;
                X500Principal issuer = null;
                for (int index = 0; index <= last; index++) {
                    X509Certificate cert = certificateChain.get(index);
                    LOGGER.debug("Current Subject DN: {}", cert.getSubjectX500Principal().getName());
                    if (issuer != null && !issuer.equals(cert.getSubjectX500Principal())) {
                        LOGGER.debug("Actual Issuer DN: {}", cert.getSubjectX500Principal().getName());
                        throw new IllegalArgumentException("Given certificates do not form a chain");
                    }
                    issuer = cert.getIssuerX500Principal();
                    LOGGER.debug("Expected Issuer DN: {}", issuer.getName());
                    if (issuer.equals(cert.getSubjectX500Principal()) && index != last) {
                        throw new IllegalArgumentException("Given certificates do not form a chain, root is not the last!");
                    }
                }
                if (size < certificateChain.size()) {
                    List<X509Certificate> temp = new ArrayList<>();
                    for (int index2 = 0; index2 < size; index2++) {
                        temp.add(certificateChain.get(index2));
                    }
                    certificateChain = temp;
                }
            }
            CertificateFactory factory = CertificateFactory.getInstance(TYPE_X509);
            return factory.generateCertPath(certificateChain);
        } catch (CertificateException e) {
            throw new IllegalArgumentException("could not create X.509 certificate factory", e);
        }
    }

    public static CertPath generateValidatableCertPath(List<X509Certificate> certificateChain, List<X500Principal> certificateAuthorities) {
        if (certificateChain == null) {
            throw new NullPointerException("Certificate chain must not be null!");
        }
        int size = certificateChain.size();
        if (size > 0) {
            int truncate = size;
            if (certificateAuthorities != null && !certificateAuthorities.isEmpty()) {
                truncate = 0;
                int index = 0;
                while (true) {
                    if (index >= size) {
                        break;
                    }
                    X509Certificate certificate = certificateChain.get(index);
                    if (!certificateAuthorities.contains(certificate.getIssuerX500Principal())) {
                        index++;
                    } else {
                        truncate = index + 1;
                        break;
                    }
                }
            }
            if (size > 1 && truncate == size) {
                int last = size - 1;
                X509Certificate cert = certificateChain.get(last);
                if (cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal())) {
                    truncate = last;
                }
            }
            size = truncate;
        }
        return generateCertPath(certificateChain, size);
    }

    public static CertPath validateCertificatePathWithIssuer(boolean truncateCertificatePath, CertPath certPath, X509Certificate[] trustedCertificates) throws GeneralSecurityException {
        String mode;
        if (trustedCertificates == null) {
            throw new CertPathValidatorException("certificates are not trusted!");
        }
        List<? extends Certificate> list = certPath.getCertificates();
        if (list.isEmpty()) {
            return certPath;
        }
        List<X509Certificate> chain = toX509CertificatesList(list);
        int size = chain.size();
        int last = size - 1;
        X509Certificate root = (X509Certificate) list.get(last);
        X509Certificate trust = null;
        boolean add = false;
        boolean truncated = false;
        if (trustedCertificates.length == 0) {
            if (last == 0) {
                if (!root.getIssuerX500Principal().equals(root.getSubjectX500Principal())) {
                    LOGGER.debug("   trust all- single certificate {}", root.getSubjectX500Principal());
                    return certPath;
                }
                last++;
            }
            mode = "last";
            trust = root;
        } else if (truncateCertificatePath) {
            mode = LinkFormat.CONTEXT;
            int index = 1;
            while (true) {
                if (index >= size) {
                    break;
                }
                X509Certificate certificate = chain.get(index);
                if (!contains(certificate, trustedCertificates)) {
                    index++;
                } else {
                    trust = certificate;
                    if (last > index) {
                        last = index;
                        truncated = true;
                        add = true;
                    }
                }
            }
            if (trust == null) {
                last = size;
                trust = searchIssuer(root, trustedCertificates);
                if (trust != null) {
                    add = !root.equals(trust);
                }
            }
            if (trust == null) {
                X509Certificate node = chain.get(0);
                if (contains(node, trustedCertificates)) {
                    if (size > 1) {
                        mode = "node's issuer";
                        last = 1;
                        truncated = true;
                        trust = chain.get(1);
                    } else {
                        LOGGER.debug("   trust node - single certificate {}", node.getSubjectX500Principal());
                        return certPath;
                    }
                }
            }
        } else {
            trust = searchIssuer(root, trustedCertificates);
            if (trust == null && contains(root, trustedCertificates)) {
                mode = "last's subject";
                trust = root;
            } else {
                mode = "last's issuer";
                add = !root.equals(trust);
            }
            last = size;
        }
        CertPath verifyCertPath = generateCertPath(chain, last);
        Set<TrustAnchor> trustAnchors = new HashSet<>();
        if (trust == null) {
            trust = trustedCertificates[0];
        }
        trustAnchors.add(new TrustAnchor(trust, null));
        if (LOGGER.isDebugEnabled()) {
            List<X509Certificate> validateChain = toX509CertificatesList(verifyCertPath.getCertificates());
            LOGGER.debug("verify: certificate path {} (orig. {})", Integer.valueOf(last), Integer.valueOf(size));
            X509Certificate top = null;
            for (X509Certificate certificate2 : validateChain) {
                LOGGER.debug("   cert : {}", certificate2.getSubjectX500Principal());
                top = certificate2;
            }
            if (top != null) {
                LOGGER.debug("   sign : {}", top.getIssuerX500Principal());
            }
            for (TrustAnchor anchor : trustAnchors) {
                LOGGER.debug("   trust: {}, {}", mode, anchor.getTrustedCert().getSubjectX500Principal());
            }
        }
        String algorithm = CertPathValidator.getDefaultType();
        CertPathValidator validator = CertPathValidator.getInstance(algorithm);
        PKIXParameters params = new PKIXParameters(trustAnchors);
        params.setRevocationEnabled(false);
        validator.validate(verifyCertPath, params);
        if (truncated || add) {
            if (add) {
                if (!truncated) {
                    chain.add(trust);
                }
                verifyCertPath = generateCertPath(chain, last + 1);
            }
            return verifyCertPath;
        }
        return certPath;
    }

    public static List<X509Certificate> toX509CertificatesList(List<? extends Certificate> certificates) {
        if (certificates == null) {
            throw new NullPointerException("Certificates list must not be null!");
        }
        List<X509Certificate> chain = new ArrayList<>(certificates.size());
        for (Certificate cert : certificates) {
            if (!(cert instanceof X509Certificate)) {
                throw new IllegalArgumentException("Given certificate is not X.509!" + cert.getClass());
            }
            chain.add((X509Certificate) cert);
        }
        return chain;
    }

    public static List<X500Principal> toSubjects(List<X509Certificate> certificates) {
        if (certificates != null && !certificates.isEmpty()) {
            List<X500Principal> subjects = new ArrayList<>(certificates.size());
            for (X509Certificate certificate : certificates) {
                X500Principal subject = certificate.getSubjectX500Principal();
                if (!subjects.contains(subject)) {
                    subjects.add(subject);
                }
            }
            return subjects;
        }
        return Collections.emptyList();
    }

    public static String getSubjectsCn(X509Certificate certificate) {
        X500Principal principal = certificate.getSubjectX500Principal();
        return Asn1DerDecoder.readCNFromDN(principal.getEncoded());
    }

    public static boolean matchLiteralIP(X509Certificate node, String literalDestination) {
        if (node == null) {
            throw new NullPointerException("Certificate must not be null!");
        }
        if (literalDestination == null) {
            throw new NullPointerException("Destination must not be null!");
        }
        if (!StringUtil.isLiteralIpAddress(literalDestination)) {
            throw new IllegalArgumentException("Destination " + literalDestination + " is no literal IP!");
        }
        try {
            Collection<List<?>> alternativeNames = node.getSubjectAlternativeNames();
            if (alternativeNames != null) {
                for (List<?> alternativeName : alternativeNames) {
                    int type = ((Integer) alternativeName.get(0)).intValue();
                    if (type == 7) {
                        String value = (String) alternativeName.get(1);
                        if (StringUtil.isLiteralIpAddress(value) && matchLiteralIP(value, literalDestination)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        } catch (IllegalArgumentException e2) {
            return false;
        } catch (CertificateParsingException e3) {
            return false;
        }
    }

    public static boolean matchLiteralIP(String subject, String literalDestination) {
        if (subject == null) {
            throw new NullPointerException("Subject must not be null!");
        }
        if (literalDestination == null) {
            throw new NullPointerException("Destination must nit be null!");
        }
        if (subject.equalsIgnoreCase(literalDestination)) {
            return true;
        }
        try {
            return InetAddress.getByName(subject).equals(InetAddress.getByName(literalDestination));
        } catch (SecurityException | UnknownHostException e) {
            return false;
        }
    }

    public static boolean matchDestination(X509Certificate node, String destination) {
        String cn;
        if (node == null) {
            throw new NullPointerException("Certificate must not be null!");
        }
        if (destination == null) {
            throw new NullPointerException("Destination must not be null!");
        }
        try {
            boolean hasSanDns = false;
            Collection<List<?>> alternativeNames = node.getSubjectAlternativeNames();
            if (alternativeNames != null) {
                for (List<?> alternativeName : alternativeNames) {
                    int type = ((Integer) alternativeName.get(0)).intValue();
                    if (type == 2) {
                        hasSanDns = true;
                        String value = (String) alternativeName.get(1);
                        if (destination.equalsIgnoreCase(value)) {
                            return true;
                        }
                    }
                }
            }
            if (!hasSanDns && (cn = getSubjectsCn(node)) != null) {
                if (destination.equalsIgnoreCase(WHITESPACE_PATTERN.matcher(cn.trim()).replaceAll(" "))) {
                    return true;
                }
                return false;
            }
            return false;
        } catch (ClassCastException e) {
            LOGGER.debug("match", e);
            return false;
        } catch (IllegalArgumentException e2) {
            LOGGER.debug("match", e2);
            return false;
        } catch (CertificateParsingException e3) {
            LOGGER.debug("match", e3);
            return false;
        }
    }

    private static X509Certificate searchIssuer(X509Certificate certificate, X509Certificate[] certificates) {
        X500Principal subject = certificate.getIssuerX500Principal();
        X509Certificate anchor = null;
        for (X509Certificate trust : certificates) {
            if (trust != null && subject.equals(trust.getSubjectX500Principal())) {
                if (anchor != null && verifySignature(certificate, anchor)) {
                    return anchor;
                }
                anchor = trust;
            }
        }
        return anchor;
    }

    private static boolean verifySignature(X509Certificate certificate, X509Certificate caCertificate) {
        try {
            caCertificate.checkValidity();
            certificate.verify(caCertificate.getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static boolean contains(X509Certificate certificate, X509Certificate[] certificates) throws CertificateEncodingException {
        for (X509Certificate trust : certificates) {
            if (certificate.equals(trust)) {
                return true;
            }
        }
        return false;
    }
}
