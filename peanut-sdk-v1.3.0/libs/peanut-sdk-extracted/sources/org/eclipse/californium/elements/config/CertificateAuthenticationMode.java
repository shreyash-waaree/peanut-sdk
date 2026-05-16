package org.eclipse.californium.elements.config;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/CertificateAuthenticationMode.class */
public enum CertificateAuthenticationMode {
    NONE(false),
    WANTED(true),
    NEEDED(true);

    private final boolean useCertificateRequest;

    CertificateAuthenticationMode(boolean useCertificateRequest) {
        this.useCertificateRequest = useCertificateRequest;
    }

    public boolean useCertificateRequest() {
        return this.useCertificateRequest;
    }
}
