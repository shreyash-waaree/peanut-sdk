package org.eclipse.californium.core.network.serialization;

import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.elements.util.DatagramWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/serialization/UdpDataSerializer.class */
public final class UdpDataSerializer extends DataSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(UdpDataSerializer.class);

    @Override // org.eclipse.californium.core.network.serialization.DataSerializer
    protected void serializeMessage(DatagramWriter writer, Message message) {
        int mid = message.getMID();
        if (mid == -1) {
            IllegalArgumentException ex = new IllegalArgumentException("MID required for UDP serialization!");
            LOGGER.warn("UDP, {}:", message, ex);
            throw ex;
        }
        MessageHeader header = new MessageHeader(1, message.getType(), message.getToken(), message.getRawCode(), mid, -1);
        serializeHeader(writer, header);
        writer.writeCurrentByte();
        serializeOptionsAndPayload(writer, message.getOptions(), message.getPayload());
    }

    @Override // org.eclipse.californium.core.network.serialization.DataSerializer
    protected void serializeHeader(DatagramWriter writer, MessageHeader header) {
        writer.write(1, 2);
        writer.write(header.getType().value, 2);
        writer.write(header.getToken().length(), 4);
        writer.write(header.getCode(), 8);
        writer.write(header.getMID(), 16);
        writer.writeBytes(header.getToken().getBytes());
    }

    @Override // org.eclipse.californium.core.network.serialization.DataSerializer
    protected void assertValidOptions(OptionSet options) {
        UdpDataParser.assertValidUdpOptions(options);
    }
}
