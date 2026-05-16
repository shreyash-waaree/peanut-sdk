package org.eclipse.californium.elements;

import java.util.concurrent.TimeUnit;
import org.eclipse.californium.elements.util.FilteredLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/EndpointContextUtil.class */
public class EndpointContextUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointContextUtil.class);
    private static final FilteredLogger WARN_FILTER = new FilteredLogger(LOGGER, 3, TimeUnit.SECONDS.toNanos(10));

    public static boolean match(String name, Definitions<Definition<?>> definitions, EndpointContext context1, EndpointContext context2) {
        boolean warn = LOGGER.isWarnEnabled();
        boolean trace = LOGGER.isTraceEnabled();
        boolean matchAll = true;
        for (Definition<?> key : definitions) {
            Object value1 = context1.get(key);
            Object value2 = context2.get(key);
            boolean match = value1 == value2 || (null != value1 && value1.equals(value2));
            if (!match && !warn) {
                return false;
            }
            if (!match) {
                WARN_FILTER.warn("{}, {}: \"{}\" != \"{}\"", name, key, value1, value2);
            } else if (trace) {
                LOGGER.trace("{}, {}: \"{}\" == \"{}\"", new Object[]{name, key, value1, value2});
            }
            matchAll = matchAll && match;
        }
        return matchAll;
    }

    public static EndpointContext getFollowUpEndpointContext(EndpointContext messageContext, EndpointContext connectionContext) {
        EndpointContext followUpEndpointContext;
        String mode = messageContext.getString(DtlsEndpointContext.KEY_HANDSHAKE_MODE);
        if (mode != null && mode.equals(DtlsEndpointContext.HANDSHAKE_MODE_NONE)) {
            followUpEndpointContext = MapBasedEndpointContext.addEntries(connectionContext, DtlsEndpointContext.ATTRIBUTE_HANDSHAKE_MODE_NONE);
        } else {
            followUpEndpointContext = connectionContext;
        }
        return followUpEndpointContext;
    }
}
