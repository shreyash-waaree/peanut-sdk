package org.eclipse.californium.elements.auth;

import java.util.HashMap;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/auth/AdditionalInfo.class */
public final class AdditionalInfo {
    private static final Map<String, Object> EMPTY_MAP = new HashMap(0);
    private final Map<String, Object> info;

    private AdditionalInfo(Map<String, Object> additionalInfo) {
        if (additionalInfo == null) {
            this.info = EMPTY_MAP;
        } else {
            this.info = new HashMap(additionalInfo);
        }
    }

    public static AdditionalInfo empty() {
        return new AdditionalInfo(null);
    }

    public static AdditionalInfo from(Map<String, Object> info) {
        return new AdditionalInfo(info);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = this.info.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
}
