package org.eclipse.californium.core.network.serialization;

import org.eclipse.californium.elements.util.DatagramWriter;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/serialization/TcpDataSerializer.class */
public final class TcpDataSerializer extends DataSerializer {
    @Override // org.eclipse.californium.core.network.serialization.DataSerializer
    protected void serializeHeader(DatagramWriter writer, MessageHeader header) {
        if (header.getBodyLength() < 13) {
            writer.write(header.getBodyLength(), 4);
            writer.write(header.getToken().length(), 4);
        } else if (header.getBodyLength() < 269) {
            writer.write(13, 4);
            writer.write(header.getToken().length(), 4);
            writer.write(header.getBodyLength() - 13, 8);
        } else if (header.getBodyLength() < 65805) {
            writer.write(14, 4);
            writer.write(header.getToken().length(), 4);
            writer.write(header.getBodyLength() - 269, 16);
        } else {
            writer.write(15, 4);
            writer.write(header.getToken().length(), 4);
            writer.write(header.getBodyLength() - 65805, 32);
        }
        writer.write(header.getCode(), 8);
        writer.writeBytes(header.getToken().getBytes());
    }
}
