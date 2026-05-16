package org.eclipse.californium.elements.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import org.eclipse.californium.elements.util.Asn1DerDecoder;
import org.eclipse.californium.elements.util.Base64;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/auth/RawPublicKeyIdentity.class */
public class RawPublicKeyIdentity extends AbstractExtensiblePrincipal<RawPublicKeyIdentity> {
    private static final int BASE_64_ENCODING_OPTIONS = 81;
    private String niUri;
    private final PublicKey publicKey;

    public RawPublicKeyIdentity(PublicKey key) {
        this(key, (AdditionalInfo) null);
    }

    private RawPublicKeyIdentity(PublicKey key, AdditionalInfo additionalInformation) {
        super(additionalInformation);
        if (key == null) {
            throw new NullPointerException("Public key must not be null");
        }
        this.publicKey = key;
        createNamedInformationUri(this.publicKey.getEncoded());
    }

    public RawPublicKeyIdentity(byte[] subjectInfo) throws GeneralSecurityException {
        this(subjectInfo, null, null);
    }

    public RawPublicKeyIdentity(byte[] subjectInfo, String keyAlgorithm) throws GeneralSecurityException {
        this(subjectInfo, keyAlgorithm, null);
    }

    private RawPublicKeyIdentity(byte[] subjectInfo, String keyAlgorithm, AdditionalInfo additionalInformation) throws GeneralSecurityException {
        super(additionalInformation);
        if (subjectInfo == null) {
            throw new NullPointerException("SubjectPublicKeyInfo must not be null");
        }
        try {
            String specKeyAlgorithm = Asn1DerDecoder.readSubjectPublicKeyAlgorithm(subjectInfo);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(subjectInfo);
            if (keyAlgorithm != null) {
                if (specKeyAlgorithm == null) {
                    specKeyAlgorithm = keyAlgorithm;
                } else if (!Asn1DerDecoder.equalKeyAlgorithmSynonyms(specKeyAlgorithm, keyAlgorithm)) {
                    throw new GeneralSecurityException(String.format("Provided key algorithm %s doesn't match %s!", keyAlgorithm, specKeyAlgorithm));
                }
            } else if (specKeyAlgorithm == null) {
                throw new GeneralSecurityException("Key algorithm could not be determined!");
            }
            KeyFactory factory = Asn1DerDecoder.getKeyFactory(specKeyAlgorithm);
            try {
                this.publicKey = factory.generatePublic(spec);
                createNamedInformationUri(subjectInfo);
            } catch (RuntimeException ex) {
                throw new GeneralSecurityException(ex.getMessage());
            }
        } catch (IllegalArgumentException ex2) {
            throw new GeneralSecurityException(ex2.getMessage());
        }
    }

    @Override // org.eclipse.californium.elements.auth.ExtensiblePrincipal
    public RawPublicKeyIdentity amend(AdditionalInfo additionalInfo) {
        return new RawPublicKeyIdentity(this.publicKey, additionalInfo);
    }

    private void createNamedInformationUri(byte[] subjectPublicKeyInfo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(subjectPublicKeyInfo);
            byte[] digest = md.digest();
            String base64urlDigest = Base64.encodeBytes(digest, 81);
            StringBuilder b = new StringBuilder("ni:///sha-256;").append(base64urlDigest);
            this.niUri = b.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
        }
    }

    @Override // java.security.Principal
    public final String getName() {
        return this.niUri;
    }

    public final PublicKey getKey() {
        return this.publicKey;
    }

    public final byte[] getSubjectInfo() {
        return this.publicKey.getEncoded();
    }

    @Override // java.security.Principal
    public String toString() {
        return "RawPublicKey Identity [" + this.niUri + "]";
    }

    @Override // java.security.Principal
    public int hashCode() {
        if (this.publicKey == null) {
            return 0;
        }
        return Arrays.hashCode(getSubjectInfo());
    }

    @Override // java.security.Principal
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RawPublicKeyIdentity other = (RawPublicKeyIdentity) obj;
        if (this.publicKey == null) {
            return other.publicKey == null;
        }
        return Arrays.equals(getSubjectInfo(), other.getSubjectInfo());
    }
}
