package org.eclipse.californium.core.network.serialization;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MessageFormatException;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.elements.util.DatagramReader;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/serialization/TcpDataParser.class */
public final class TcpDataParser extends DataParser {
    @Override // org.eclipse.californium.core.network.serialization.DataParser
    protected MessageHeader parseHeader(DatagramReader reader) {
        if (!reader.bytesAvailable(1)) {
            throw new MessageFormatException("TCP Message too short! " + (reader.bitsLeft() / 8) + " must be at least 1 byte!");
        }
        int len = reader.read(4);
        int tokenLength = reader.read(4);
        int lengthSize = 0;
        if (tokenLength > 8) {
            throw new MessageFormatException("TCP Message has invalid token length (> 8) " + tokenLength);
        }
        if (len == 13) {
            lengthSize = 1;
        } else if (len == 14) {
            lengthSize = 2;
        } else if (len == 15) {
            lengthSize = 4;
        }
        int size = lengthSize + 1 + tokenLength;
        if (!reader.bytesAvailable(size)) {
            throw new MessageFormatException("TCP Message too short! " + (reader.bitsLeft() / 8) + " must be at least " + size + " bytes!");
        }
        reader.readBytes(lengthSize);
        int code = reader.read(8);
        Token token = Token.fromProvider(reader.readBytes(tokenLength));
        return new MessageHeader(1, CoAP.Type.CON, token, code, -1, 0);
    }
}
