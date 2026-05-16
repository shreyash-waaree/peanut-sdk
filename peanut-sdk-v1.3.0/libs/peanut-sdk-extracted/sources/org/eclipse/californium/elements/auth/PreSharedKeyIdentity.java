package org.eclipse.californium.elements.auth;

import java.util.Objects;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/auth/PreSharedKeyIdentity.class */
public final class PreSharedKeyIdentity extends AbstractExtensiblePrincipal<PreSharedKeyIdentity> {
    private final boolean scopedIdentity;
    private final String virtualHost;
    private final String identity;
    private final String name;

    public PreSharedKeyIdentity(String identity) {
        this(false, null, identity, null);
    }

    public PreSharedKeyIdentity(String virtualHost, String identity) {
        this(true, virtualHost, identity, null);
    }

    private PreSharedKeyIdentity(boolean sni, String virtualHost, String identity, AdditionalInfo additionalInformation) {
        super(additionalInformation);
        if (identity == null) {
            throw new NullPointerException("Identity must not be null");
        }
        this.scopedIdentity = sni;
        if (sni) {
            StringBuilder b = new StringBuilder();
            if (virtualHost == null) {
                this.virtualHost = null;
            } else if (StringUtil.isValidHostName(virtualHost)) {
                this.virtualHost = virtualHost.toLowerCase();
                b.append(this.virtualHost);
            } else {
                throw new IllegalArgumentException("virtual host is not a valid hostname");
            }
            b.append(":");
            b.append(identity);
            this.name = b.toString();
        } else {
            if (virtualHost != null) {
                throw new IllegalArgumentException("virtual host is not supported, if sni is disabled");
            }
            this.virtualHost = null;
            this.name = identity;
        }
        this.identity = identity;
    }

    private PreSharedKeyIdentity(boolean scopedIdentity, String virtualHost, String identity, String name, AdditionalInfo additionalInfo) {
        super(additionalInfo);
        this.scopedIdentity = scopedIdentity;
        this.virtualHost = virtualHost;
        this.identity = identity;
        this.name = name;
    }

    @Override // org.eclipse.californium.elements.auth.ExtensiblePrincipal
    public PreSharedKeyIdentity amend(AdditionalInfo additionInfo) {
        return new PreSharedKeyIdentity(this.scopedIdentity, this.virtualHost, this.identity, this.name, additionInfo);
    }

    public boolean isScopedIdentity() {
        return this.scopedIdentity;
    }

    public String getVirtualHost() {
        return this.virtualHost;
    }

    public String getIdentity() {
        return this.identity;
    }

    @Override // java.security.Principal
    public String getName() {
        return this.name;
    }

    @Override // java.security.Principal
    public String toString() {
        if (this.scopedIdentity) {
            return "PreSharedKey Identity [virtual host: " + this.virtualHost + ", identity: " + this.identity + "]";
        }
        return "PreSharedKey Identity [identity: " + this.identity + "]";
    }

    @Override // java.security.Principal
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override // java.security.Principal
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PreSharedKeyIdentity other = (PreSharedKeyIdentity) obj;
        return Objects.equals(this.name, other.name);
    }
}
