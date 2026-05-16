package org.eclipse.californium.elements.auth;

import java.security.Principal;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/auth/AbstractExtensiblePrincipal.class */
public abstract class AbstractExtensiblePrincipal<T extends Principal> implements ExtensiblePrincipal<T> {
    private final AdditionalInfo additionalInfo;

    protected AbstractExtensiblePrincipal() {
        this(null);
    }

    protected AbstractExtensiblePrincipal(AdditionalInfo additionalInformation) {
        if (additionalInformation == null) {
            this.additionalInfo = AdditionalInfo.empty();
        } else {
            this.additionalInfo = additionalInformation;
        }
    }

    @Override // org.eclipse.californium.elements.auth.ExtensiblePrincipal
    public final AdditionalInfo getExtendedInfo() {
        return this.additionalInfo;
    }
}
