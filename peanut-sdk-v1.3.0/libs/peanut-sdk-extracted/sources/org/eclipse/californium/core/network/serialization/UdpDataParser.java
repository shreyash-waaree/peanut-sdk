package org.eclipse.californium.core.network.serialization;

import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAPMessageFormatException;
import org.eclipse.californium.core.coap.MessageFormatException;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.elements.util.DatagramReader;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/serialization/UdpDataParser.class */
public final class UdpDataParser extends DataParser {
    @Override // org.eclipse.californium.core.network.serialization.DataParser
    protected MessageHeader parseHeader(DatagramReader reader) {
        if (!reader.bytesAvailable(4)) {
            throw new MessageFormatException("UDP Message too short! " + (reader.bitsLeft() / 8) + " must be at least 4 bytes!");
        }
        int version = reader.read(2);
        assertCorrectVersion(version);
        int type = reader.read(2);
        int tokenLength = reader.read(4);
        if (tokenLength > 8) {
            throw new MessageFormatException("UDP Message has invalid token length (> 8) " + tokenLength);
        }
        int code = reader.read(8);
        int mid = reader.read(16);
        if (!reader.bytesAvailable(tokenLength)) {
            throw new CoAPMessageFormatException("UDP Message too short for token! " + (reader.bitsLeft() / 8) + " must be at least " + tokenLength + " bytes!", null, mid, code, CoAP.Type.CON.value == type);
        }
        Token token = Token.fromProvider(reader.readBytes(tokenLength));
        return new MessageHeader(version, CoAP.Type.valueOf(type), token, code, mid, 0);
    }

    @Override // org.eclipse.californium.core.network.serialization.DataParser
    protected void assertValidOptions(OptionSet options) {
        assertValidUdpOptions(options);
    }

    private void assertCorrectVersion(int version) {
        if (version != 1) {
            throw new MessageFormatException("UDP Message has invalid version: " + version);
        }
    }

    public static void assertValidUdpOptions(OptionSet options) {
        BlockOption block = options.getBlock1();
        if (block != null && block.isBERT()) {
            throw new IllegalArgumentException("Block1 BERT used for UDP!");
        }
        BlockOption block2 = options.getBlock2();
        if (block2 != null && block2.isBERT()) {
            throw new IllegalArgumentException("Block2 BERT used for UDP!");
        }
    }
}
