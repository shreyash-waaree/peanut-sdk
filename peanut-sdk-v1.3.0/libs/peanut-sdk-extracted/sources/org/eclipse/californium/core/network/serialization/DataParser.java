package org.eclipse.californium.core.network.serialization;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAPMessageFormatException;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.MessageFormatException;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.DatagramReader;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/serialization/DataParser.class */
public abstract class DataParser {
    protected abstract MessageHeader parseHeader(DatagramReader datagramReader);

    public final Message parseMessage(RawData raw) {
        if (raw == null) {
            throw new NullPointerException("raw-data must not be null!");
        }
        if (raw.getConnectorAddress() == null) {
            throw new NullPointerException("raw-data connector's address must not be null!");
        }
        Message message = parseMessage(raw.getBytes());
        message.setSourceContext(raw.getEndpointContext());
        if (message instanceof Request) {
            ((Request) message).setLocalAddress(raw.getConnectorAddress(), raw.isMulticast());
        } else {
            message.setLocalAddress(raw.getConnectorAddress());
        }
        message.setNanoTimestamp(raw.getReceiveNanoTimestamp());
        return message;
    }

    public final Message parseMessage(byte[] msg) {
        String errorMsg = "illegal message code";
        DatagramReader reader = new DatagramReader(msg);
        MessageHeader header = parseHeader(reader);
        try {
            Message message = null;
            if (CoAP.isRequest(header.getCode())) {
                message = parseMessage(reader, header, new Request(CoAP.Code.valueOf(header.getCode())));
            } else if (CoAP.isResponse(header.getCode())) {
                message = parseMessage(reader, header, new Response(CoAP.ResponseCode.valueOf(header.getCode())));
            } else if (CoAP.isEmptyMessage(header.getCode())) {
                message = parseMessage(reader, header, new EmptyMessage(header.getType()));
            }
            if (message != null) {
                message.setBytes(msg);
                return message;
            }
        } catch (CoAPMessageFormatException e) {
            throw e;
        } catch (MessageFormatException e2) {
            errorMsg = e2.getMessage();
        }
        throw new CoAPMessageFormatException(errorMsg, header.getToken(), header.getMID(), header.getCode(), CoAP.Type.CON == header.getType());
    }

    protected Message parseMessage(DatagramReader reader, MessageHeader header, Message target) {
        target.setMID(header.getMID());
        target.setType(header.getType());
        target.setToken(header.getToken());
        parseOptionsAndPayload(reader, target);
        return target;
    }

    protected void assertValidOptions(OptionSet options) {
    }

    public void parseOptionsAndPayload(DatagramReader reader, Message message) {
        if (reader == null) {
            throw new NullPointerException("reader must not be null!");
        }
        if (message == null) {
            throw new NullPointerException("message must not be null!");
        }
        int currentOptionNumber = 0;
        byte nextByte = 0;
        while (reader.bytesAvailable()) {
            nextByte = reader.readNextByte();
            if (nextByte != -1) {
                try {
                    int optionDeltaNibble = (240 & nextByte) >> 4;
                    currentOptionNumber += determineValueFromNibble(reader, optionDeltaNibble);
                    int optionLengthNibble = 15 & nextByte;
                    int optionLength = determineValueFromNibble(reader, optionLengthNibble);
                    if (reader.bytesAvailable(optionLength)) {
                        Option option = new Option(currentOptionNumber);
                        option.setValue(reader.readBytes(optionLength));
                        if (currentOptionNumber == 12) {
                            int format = option.getIntegerValue();
                            message.getOptions().setContentFormat(format);
                            if (!message.getOptions().hasContentFormat()) {
                                throw new IllegalArgumentException("Content Format option must be between 0 and 65535 (2 bytes) inclusive");
                            }
                        } else {
                            message.getOptions().addOption(option);
                        }
                    } else {
                        String msg = String.format("Message contains option of length %d with only fewer bytes left in the message", Integer.valueOf(optionLength));
                        throw new IllegalArgumentException(msg);
                    }
                } catch (IllegalArgumentException ex) {
                    throw new CoAPMessageFormatException(ex.getMessage(), message.getToken(), message.getMID(), message.getRawCode(), message.isConfirmable());
                }
            }
        }
        try {
            assertValidOptions(message.getOptions());
            if (nextByte == -1) {
                if (!reader.bytesAvailable()) {
                    throw new CoAPMessageFormatException("Found payload marker (0xFF) but message contains no payload", message.getToken(), message.getMID(), message.getRawCode(), message.isConfirmable());
                }
                if (!message.isIntendedPayload()) {
                    message.setUnintendedPayload();
                }
                message.setPayload(reader.readBytesLeft());
                message.assertPayloadMatchsBlocksize();
                return;
            }
            message.setPayload(Bytes.EMPTY);
        } catch (IllegalArgumentException ex2) {
            throw new CoAPMessageFormatException(ex2.getMessage(), message.getToken(), message.getMID(), message.getRawCode(), message.isConfirmable(), CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    private static int determineValueFromNibble(DatagramReader reader, int delta) {
        if (delta <= 12) {
            return delta;
        }
        if (delta == 13) {
            return reader.read(8) + 13;
        }
        if (delta == 14) {
            return reader.read(16) + 269;
        }
        throw new IllegalArgumentException("Message contains illegal option delta/length: " + delta);
    }
}
