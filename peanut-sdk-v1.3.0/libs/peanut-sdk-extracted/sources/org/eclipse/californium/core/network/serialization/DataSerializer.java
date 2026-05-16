package org.eclipse.californium.core.network.serialization;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.MessageCallback;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.util.DatagramWriter;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/serialization/DataSerializer.class */
public abstract class DataSerializer {
    protected abstract void serializeHeader(DatagramWriter datagramWriter, MessageHeader messageHeader);

    public final byte[] getByteArray(Message message) {
        if (message == null) {
            throw new NullPointerException("message must not be null!");
        }
        assertValidOptions(message.getOptions());
        message.assertPayloadMatchsBlocksize();
        if (message.getRawCode() == 0) {
            if (message.getType() == CoAP.Type.NON) {
                throw new IllegalArgumentException("NON message must not use code 0 (empty message)!");
            }
            if (!message.getToken().isEmpty()) {
                throw new IllegalArgumentException("Empty messages must not use a token!");
            }
            if (message.getPayloadSize() > 0) {
                throw new IllegalArgumentException("Empty messages must not contain payload!");
            }
            DatagramWriter messageWriter = new DatagramWriter(4);
            serializeEmpytMessage(messageWriter, message);
            return messageWriter.toByteArray();
        }
        DatagramWriter messageWriter2 = new DatagramWriter();
        serializeMessage(messageWriter2, message);
        return messageWriter2.toByteArray();
    }

    public final RawData serializeRequest(Request request) {
        return serializeRequest(request, null);
    }

    public final RawData serializeRequest(Request request, MessageCallback outboundCallback) {
        if (request == null) {
            throw new NullPointerException("request must not be null!");
        }
        if (request.getBytes() == null) {
            request.setBytes(getByteArray(request));
        }
        return RawData.outbound(request.getBytes(), request.getEffectiveDestinationContext(), outboundCallback, request.isMulticast());
    }

    public final RawData serializeResponse(Response response) {
        return serializeResponse(response, null);
    }

    public final RawData serializeResponse(Response response, MessageCallback outboundCallback) {
        if (response == null) {
            throw new NullPointerException("response must not be null!");
        }
        if (response.getBytes() == null) {
            response.setBytes(getByteArray(response));
        }
        return RawData.outbound(response.getBytes(), response.getEffectiveDestinationContext(), outboundCallback, false);
    }

    public final RawData serializeEmptyMessage(EmptyMessage emptyMessage) {
        return serializeEmptyMessage(emptyMessage, null);
    }

    public final RawData serializeEmptyMessage(EmptyMessage emptyMessage, MessageCallback outboundCallback) {
        if (emptyMessage == null) {
            throw new NullPointerException("empty-message must not be null!");
        }
        if (emptyMessage.getBytes() == null) {
            emptyMessage.setBytes(getByteArray(emptyMessage));
        }
        return RawData.outbound(emptyMessage.getBytes(), emptyMessage.getEffectiveDestinationContext(), outboundCallback, false);
    }

    protected void serializeEmpytMessage(DatagramWriter writer, Message message) {
        MessageHeader header = new MessageHeader(1, message.getType(), message.getToken(), 0, message.getMID(), 0);
        serializeHeader(writer, header);
        writer.writeCurrentByte();
    }

    protected void serializeMessage(DatagramWriter writer, Message message) {
        DatagramWriter optionsAndPayloadWriter = new DatagramWriter();
        serializeOptionsAndPayload(optionsAndPayloadWriter, message.getOptions(), message.getPayload());
        optionsAndPayloadWriter.writeCurrentByte();
        MessageHeader header = new MessageHeader(1, message.getType(), message.getToken(), message.getRawCode(), message.getMID(), optionsAndPayloadWriter.size());
        serializeHeader(writer, header);
        writer.writeCurrentByte();
        writer.write(optionsAndPayloadWriter);
    }

    protected void assertValidOptions(OptionSet options) {
    }

    public static void serializeOptionsAndPayload(DatagramWriter writer, OptionSet optionSet, byte[] payload) {
        if (writer == null) {
            throw new NullPointerException("writer must not be null!");
        }
        if (optionSet == null) {
            throw new NullPointerException("option-set must not be null!");
        }
        int lastOptionNumber = 0;
        for (Option option : optionSet.asSortedList()) {
            byte[] value = option.getValue();
            int optionNumber = option.getNumber();
            int optionDelta = optionNumber - lastOptionNumber;
            int optionDeltaNibble = getOptionNibble(optionDelta);
            writer.write(optionDeltaNibble, 4);
            int optionLength = value.length;
            int optionLengthNibble = getOptionNibble(optionLength);
            writer.write(optionLengthNibble, 4);
            if (optionDeltaNibble == 13) {
                writer.write(optionDelta - 13, 8);
            } else if (optionDeltaNibble == 14) {
                writer.write(optionDelta - 269, 16);
            }
            if (optionLengthNibble == 13) {
                writer.write(optionLength - 13, 8);
            } else if (optionLengthNibble == 14) {
                writer.write(optionLength - 269, 16);
            }
            writer.writeBytes(value);
            lastOptionNumber = optionNumber;
        }
        if (payload != null && payload.length > 0) {
            writer.writeByte((byte) -1);
            writer.writeBytes(payload);
        }
    }

    private static int getOptionNibble(int optionValue) {
        if (optionValue <= 12) {
            return optionValue;
        }
        if (optionValue <= 268) {
            return 13;
        }
        if (optionValue <= 65804) {
            return 14;
        }
        throw new IllegalArgumentException("Unsupported option delta " + optionValue);
    }
}
