package org.eclipse.californium.elements.auth;

import java.security.Principal;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/auth/ExtensiblePrincipal.class */
public interface ExtensiblePrincipal<T extends Principal> extends Principal {
    T amend(AdditionalInfo additionalInfo);

    AdditionalInfo getExtendedInfo();
}
