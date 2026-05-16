package org.eclipse.californium.elements.auth;

import java.io.ByteArrayInputStream;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.CertPathUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/auth/X509CertPath.class */
public class X509CertPath extends AbstractExtensiblePrincipal<X509CertPath> {
    private static final String TYPE_X509 = "X.509";
    private static final String ENCODING = "PkiPath";
    private final CertPath path;
    private final X509Certificate target;

    public X509CertPath(CertPath certPath) {
        this(certPath, null);
    }

    private X509CertPath(CertPath certPath, AdditionalInfo additionalInformation) {
        super(additionalInformation);
        if (!TYPE_X509.equals(certPath.getType())) {
            throw new IllegalArgumentException("Cert path must contain X.509 certificates only");
        }
        if (certPath.getCertificates().isEmpty()) {
            throw new IllegalArgumentException("Cert path must not be empty");
        }
        this.path = certPath;
        this.target = (X509Certificate) certPath.getCertificates().get(0);
    }

    @Override // org.eclipse.californium.elements.auth.ExtensiblePrincipal
    public X509CertPath amend(AdditionalInfo additionInfo) {
        return new X509CertPath(this.path, additionInfo);
    }

    public static X509CertPath fromBytes(byte[] encodedPath) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance(TYPE_X509);
            CertPath certPath = factory.generateCertPath(new ByteArrayInputStream(encodedPath), ENCODING);
            return new X509CertPath(certPath);
        } catch (CertificateException e) {
            throw new IllegalArgumentException("byte array does not contain X.509 certificate path");
        }
    }

    public static X509CertPath fromCertificatesChain(Certificate... certificateChain) {
        if (certificateChain == null) {
            throw new NullPointerException("Certificate chain must not be null!");
        }
        if (certificateChain.length == 0) {
            throw new IllegalArgumentException("Certificate chain must not be empty!");
        }
        List<X509Certificate> chain = CertPathUtil.toX509CertificatesList(Arrays.asList(certificateChain));
        CertPath certPath = CertPathUtil.generateCertPath(chain);
        return new X509CertPath(certPath);
    }

    public static X509CertPath fromCertificatesChain(List<X509Certificate> certificateChain) {
        if (certificateChain == null) {
            throw new NullPointerException("Certificate chain must not be null!");
        }
        if (certificateChain.isEmpty()) {
            throw new IllegalArgumentException("Certificate chain must not be empty!");
        }
        CertPath certPath = CertPathUtil.generateCertPath(certificateChain);
        return new X509CertPath(certPath);
    }

    public byte[] toByteArray() {
        try {
            return this.path.getEncoded(ENCODING);
        } catch (CertificateEncodingException e) {
            return Bytes.EMPTY;
        }
    }

    @Override // java.security.Principal
    public String getName() {
        return this.target.getSubjectX500Principal().getName();
    }

    public String getCN() {
        return CertPathUtil.getSubjectsCn(this.target);
    }

    public CertPath getPath() {
        return this.path;
    }

    public X509Certificate getTarget() {
        return this.target;
    }

    @Override // java.security.Principal
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        X509CertPath other = (X509CertPath) obj;
        return this.target.equals(other.target);
    }

    @Override // java.security.Principal
    public int hashCode() {
        return this.target.hashCode();
    }

    @Override // java.security.Principal
    public String toString() {
        return "x509 [" + getName() + "]";
    }
}
