package org.eclipse.californium.core;

import java.security.Principal;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.TlsEndpointContext;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/Utils.class */
public final class Utils {
    private Utils() {
    }

    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        return "[" + StringUtil.byteArray2Hex(bytes) + "]";
    }

    public static String prettyPrint(Request request) {
        String nl = StringUtil.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("==[ CoAP Request ]=============================================").append(nl);
        sb.append(String.format("MID    : %d%n", Integer.valueOf(request.getMID())));
        sb.append(String.format("Token  : %s%n", request.getTokenString()));
        sb.append(String.format("Type   : %s%n", request.getType()));
        CoAP.Code code = request.getCode();
        if (code == null) {
            sb.append("Method : 0.00 - PING").append(nl);
        } else {
            sb.append(String.format("Method : %s - %s%n", code.text, code.name()));
        }
        if (request.getOffloadMode() != null) {
            sb.append("(offloaded)").append(nl);
        } else {
            sb.append(String.format("Options: %s%n", request.getOptions()));
            sb.append(String.format("Payload: %d Bytes%n", Integer.valueOf(request.getPayloadSize())));
            if (request.getPayloadSize() > 0 && MediaTypeRegistry.isPrintable(request.getOptions().getContentFormat())) {
                sb.append("---------------------------------------------------------------").append(nl);
                sb.append(request.getPayloadString());
                sb.append(nl);
            }
        }
        sb.append("===============================================================");
        return sb.toString();
    }

    public static String prettyPrint(CoapResponse response) {
        return prettyPrint(response.advanced());
    }

    public static String prettyPrint(Response response) {
        String nl = StringUtil.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("==[ CoAP Response ]============================================").append(nl);
        sb.append(String.format("MID    : %d%n", Integer.valueOf(response.getMID())));
        sb.append(String.format("Token  : %s%n", response.getTokenString()));
        sb.append(String.format("Type   : %s%n", response.getType()));
        CoAP.ResponseCode code = response.getCode();
        sb.append(String.format("Status : %s - %s%n", code, code.name()));
        Long rtt = response.getApplicationRttNanos();
        if (response.getOffloadMode() != null) {
            if (rtt != null) {
                sb.append(String.format("RTT    : %d ms%n", Long.valueOf(TimeUnit.NANOSECONDS.toMillis(rtt.longValue()))));
            }
            sb.append("(offloaded)").append(nl);
        } else {
            sb.append(String.format("Options: %s%n", response.getOptions()));
            if (rtt != null) {
                sb.append(String.format("RTT    : %d ms%n", Long.valueOf(TimeUnit.NANOSECONDS.toMillis(rtt.longValue()))));
            }
            sb.append(String.format("Payload: %d Bytes%n", Integer.valueOf(response.getPayloadSize())));
            if (response.getPayloadSize() > 0 && MediaTypeRegistry.isPrintable(response.getOptions().getContentFormat())) {
                sb.append("---------------------------------------------------------------").append(nl);
                sb.append(response.getPayloadString());
                sb.append(nl);
            }
        }
        sb.append("===============================================================");
        return sb.toString();
    }

    public static String prettyPrint(EndpointContext endpointContext) {
        String nl = StringUtil.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append(">>> ").append(endpointContext);
        String cipher = endpointContext.getString(DtlsEndpointContext.KEY_CIPHER);
        if (cipher == null) {
            cipher = endpointContext.getString(TlsEndpointContext.KEY_CIPHER);
        }
        if (cipher != null) {
            sb.append(nl).append(">>> ").append(cipher);
        }
        Principal principal = endpointContext.getPeerIdentity();
        if (principal != null) {
            sb.append(nl).append(">>> ").append(principal);
        }
        String cid = endpointContext.getString(DtlsEndpointContext.KEY_READ_CONNECTION_ID);
        if (cid != null) {
            sb.append(nl).append(">>> read-cid : ").append(cid);
        }
        String cid2 = endpointContext.getString(DtlsEndpointContext.KEY_WRITE_CONNECTION_ID);
        if (cid2 != null) {
            sb.append(nl).append(">>> write-cid: ").append(cid2);
        }
        return sb.toString();
    }
}
