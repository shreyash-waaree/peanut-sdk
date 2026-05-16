package org.eclipse.californium.core.coap;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.elements.EndpointContext;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/EmptyMessage.class */
public class EmptyMessage extends Message {
    public EmptyMessage(CoAP.Type type) {
        super(type);
        setProtectFromOffload();
    }

    public String toString() {
        String payload;
        String appendix = "";
        if (!hasEmptyToken() || getOptions().asSortedList().size() > 0 || getPayloadSize() > 0) {
            String payload2 = getPayloadString();
            if (payload2 == null) {
                payload = "no payload";
            } else {
                int len = payload2.length();
                if (payload2.indexOf("\n") != -1) {
                    payload2 = payload2.substring(0, payload2.indexOf("\n"));
                }
                if (payload2.length() > 24) {
                    payload2 = payload2.substring(0, 20);
                }
                payload = "\"" + payload2 + "\"";
                if (payload.length() != len + 2) {
                    payload = payload + ".. " + payload.length() + " bytes";
                }
            }
            appendix = " NON-EMPTY: Token=" + getTokenString() + ", " + getOptions() + ", " + payload;
        }
        return String.format("%s        MID=%5d%s", getType(), Integer.valueOf(getMID()), appendix);
    }

    @Override // org.eclipse.californium.core.coap.Message
    public int getRawCode() {
        return 0;
    }

    @Override // org.eclipse.californium.core.coap.Message
    public boolean isIntendedPayload() {
        return false;
    }

    @Override // org.eclipse.californium.core.coap.Message
    public void assertPayloadMatchsBlocksize() {
        if (getPayloadSize() > 0) {
            throw new IllegalStateException("Empty message contains " + getPayloadSize() + " bytes payload!");
        }
    }

    @Override // org.eclipse.californium.core.coap.Message
    public boolean hasBlock(BlockOption block) {
        return false;
    }

    public static EmptyMessage newACK(Message message) {
        return newACK(message, message.getSourceContext());
    }

    public static EmptyMessage newACK(Message message, EndpointContext destination) {
        EmptyMessage ack = new EmptyMessage(CoAP.Type.ACK);
        ack.setDestinationContext(destination);
        ack.setMID(message.getMID());
        return ack;
    }

    public static EmptyMessage newRST(Message message) {
        return newRST(message, message.getSourceContext());
    }

    public static EmptyMessage newRST(Message message, EndpointContext destination) {
        EmptyMessage rst = new EmptyMessage(CoAP.Type.RST);
        rst.setDestinationContext(destination);
        rst.setMID(message.getMID());
        return rst;
    }
}
