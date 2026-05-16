package org.eclipse.californium.elements.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.elements.Definition;
import org.eclipse.californium.elements.Definitions;
import org.eclipse.californium.elements.MapBasedEndpointContext;
import org.eclipse.californium.elements.exception.VersionMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SerializationUtil.class */
public class SerializationUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationUtil.class);
    public static final int NO_VERSION = 0;
    private static final int ADDRESS_VERSION = 1;
    private static final int ADDRESS_LITERAL = 1;
    private static final int ADDRESS_NAME = 2;
    private static final int ATTRIBUTES_VERSION = 1;
    private static final int ATTRIBUTES_STRING = 1;
    private static final int ATTRIBUTES_BYTES = 2;
    private static final int ATTRIBUTES_INTEGER = 3;
    private static final int ATTRIBUTES_LONG = 4;
    private static final int ATTRIBUTES_BOOLEAN = 5;
    private static final int ATTRIBUTES_INET_SOCKET_ADDRESS = 6;
    private static final int NANOTIME_SNYC_MARK_VERSION = 1;

    public static void writeNoItem(OutputStream out) throws IOException {
        out.write(0);
    }

    public static void writeNoItem(DatagramWriter writer) {
        writer.writeByte((byte) 0);
    }

    public static int writeStartItem(DatagramWriter writer, int version, int numBits) {
        if (version == 0) {
            throw new IllegalArgumentException("version must not be 0!");
        }
        writer.writeByte((byte) version);
        return writer.space(numBits);
    }

    public static void writeFinishedItem(DatagramWriter writer, int position, int numBits) {
        writer.writeSize(position, numBits);
    }

    public static int readStartItem(DataStreamReader reader, int version, int numBits) {
        if (version == 0) {
            throw new IllegalArgumentException("Version must not be 0!");
        }
        int read = reader.readNextByte() & 255;
        if (read == 0) {
            return -1;
        }
        if (read != version) {
            throw new VersionMismatchException("Version mismatch! " + version + " is required, not " + read + "!", read);
        }
        return reader.read(numBits);
    }

    public static int readStartItem(DataStreamReader reader, SupportedVersionsMatcher versions, int numBits) {
        if (versions == null) {
            throw new NullPointerException("Version must not be null!");
        }
        int read = reader.readNextByte() & 255;
        if (read == 0) {
            return -1;
        }
        if (!versions.supports(read)) {
            throw new VersionMismatchException("Version mismatch! " + versions + " are required, not " + read + "!", read);
        }
        return reader.read(numBits);
    }

    public static void write(DatagramWriter writer, String value, int numBits) {
        writer.writeVarBytes(value == null ? null : value.getBytes(StandardCharsets.UTF_8), numBits);
    }

    public static String readString(DataStreamReader reader, int numBits) {
        byte[] data = reader.readVarBytes(numBits);
        if (data != null) {
            return new String(data, StandardCharsets.UTF_8);
        }
        return null;
    }

    public static boolean verifyString(DataStreamReader reader, String expectedValue, int numBits) {
        if (expectedValue == null) {
            throw new NullPointerException("Expected value must not be null!");
        }
        byte[] data = reader.readVarBytes(numBits);
        if (data == null) {
            return false;
        }
        byte[] mark = expectedValue.getBytes(StandardCharsets.UTF_8);
        if (Arrays.equals(mark, data)) {
            return true;
        }
        String read = StringUtil.toDisplayString(data, 16);
        if (!read.startsWith("\"") && !read.startsWith("<")) {
            expectedValue = StringUtil.byteArray2HexString(mark, ' ', 16);
        }
        throw new IllegalArgumentException("Mismatch, read " + read + ", expected " + expectedValue + ".");
    }

    public static void write(DatagramWriter writer, InetSocketAddress address) {
        if (address == null) {
            writeNoItem(writer);
            return;
        }
        int position = writeStartItem(writer, 1, 8);
        writer.write(address.getPort(), 16);
        if (address.isUnresolved()) {
            writer.writeByte((byte) 2);
            writer.writeBytes(address.getHostName().getBytes(StandardCharsets.US_ASCII));
        } else {
            writer.writeByte((byte) 1);
            writer.writeBytes(address.getAddress().getAddress());
        }
        writeFinishedItem(writer, position, 8);
    }

    public static InetSocketAddress readAddress(DataStreamReader reader) {
        int length = readStartItem(reader, 1, 8);
        if (length <= 0) {
            return null;
        }
        DatagramReader rangeReader = reader.createRangeReader(length);
        int port = rangeReader.read(16);
        int type = rangeReader.readNextByte() & 255;
        byte[] address = rangeReader.readBytesLeft();
        switch (type) {
            case 1:
                try {
                    return new InetSocketAddress(InetAddress.getByAddress(address), port);
                } catch (UnknownHostException e) {
                    return null;
                }
            case 2:
                return new InetSocketAddress(new String(address, StandardCharsets.US_ASCII), port);
            default:
                return null;
        }
    }

    public static void write(DatagramWriter writer, Map<Definition<?>, Object> entries) {
        if (entries == null) {
            writeNoItem(writer);
            return;
        }
        int position = writeStartItem(writer, 1, 16);
        for (Map.Entry<Definition<?>, Object> entry : entries.entrySet()) {
            write(writer, entry.getKey().getKey(), 8);
            Object value = entry.getValue();
            if (value instanceof String) {
                writer.writeByte((byte) 1);
                write(writer, (String) value, 8);
            } else if (value instanceof Bytes) {
                writer.writeByte((byte) 2);
                writer.writeVarBytes((Bytes) value, 8);
            } else if (value instanceof Integer) {
                writer.writeByte((byte) 3);
                writer.write(((Integer) value).intValue(), 32);
            } else if (value instanceof Long) {
                writer.writeByte((byte) 4);
                writer.writeLong(((Long) value).longValue(), 64);
            } else if (value instanceof Boolean) {
                writer.writeByte((byte) 5);
                writer.writeByte(((Boolean) value).booleanValue() ? (byte) 1 : (byte) 0);
            } else if (value instanceof InetSocketAddress) {
                writer.writeByte((byte) 6);
                write(writer, (InetSocketAddress) value);
            }
        }
        writeFinishedItem(writer, position, 16);
    }

    /* JADX WARN: Failed to find 'out' block for switch in B:14:0x0066. Please report as an issue. */
    public static <T extends Definition<?>> MapBasedEndpointContext.Attributes readEndpointContexAttributes(DataStreamReader reader, Definitions<T> definitions) {
        int length = readStartItem(reader, 1, 16);
        if (length < 0) {
            return null;
        }
        DatagramReader rangeReader = reader.createRangeReader(length);
        MapBasedEndpointContext.Attributes attributes = new MapBasedEndpointContext.Attributes();
        while (rangeReader.bytesAvailable()) {
            String key = readString(rangeReader, 8);
            Definition<?> definition = definitions.get(key);
            if (definition == null) {
                throw new IllegalArgumentException("'" + key + "' is not in definitions!");
            }
            try {
                int type = rangeReader.readNextByte() & 255;
                switch (type) {
                    case 1:
                        String stringValue = readString(rangeReader, 8);
                        attributes.add(definition, stringValue);
                        break;
                    case 2:
                        byte[] data = rangeReader.readVarBytes(8);
                        attributes.add(definition, new Bytes(data));
                        break;
                    case 3:
                        int intValue = rangeReader.read(32);
                        attributes.add(definition, Integer.valueOf(intValue));
                        break;
                    case 4:
                        long longValue = rangeReader.readLong(64);
                        attributes.add(definition, Long.valueOf(longValue));
                        break;
                    case 5:
                        byte booleanValue = rangeReader.readNextByte();
                        attributes.add(definition, booleanValue == 1 ? Boolean.TRUE : Boolean.FALSE);
                        break;
                    case 6:
                        InetSocketAddress address = readAddress(rangeReader);
                        attributes.add(definition, address);
                        break;
                }
            } catch (ClassCastException ex) {
                LOGGER.warn("Read attribute {}:", key, ex);
            } catch (IllegalArgumentException ex2) {
                LOGGER.warn("Read attribute {}:", key, ex2);
            }
        }
        return attributes;
    }

    public static void writeNanotimeSynchronizationMark(DatagramWriter writer) {
        int position = writeStartItem(writer, 1, 8);
        long millis = System.currentTimeMillis();
        long nanos = ClockUtil.nanoRealtime();
        writer.writeLong(millis, 64);
        writer.writeLong(nanos, 64);
        writeFinishedItem(writer, position, 8);
    }

    public static long readNanotimeSynchronizationMark(DataStreamReader reader) {
        int length = readStartItem(reader, 1, 8);
        if (length <= 0) {
            return 0L;
        }
        DatagramReader rangeReader = reader.createRangeReader(length);
        long millis = rangeReader.readLong(64);
        long nanos = rangeReader.readLong(64);
        rangeReader.assertFinished("times");
        long startMillis = System.currentTimeMillis();
        long startNanos = ClockUtil.nanoRealtime();
        long deltaSystemtime = Math.max(TimeUnit.MILLISECONDS.toNanos(startMillis - millis), 0L);
        long deltaUptime = startNanos - nanos;
        long delta = deltaUptime - deltaSystemtime;
        return delta;
    }

    public static void skipItems(InputStream in, int numBits) {
        DataStreamReader reader = new DataStreamReader(in);
        while ((reader.readNextByte() & 255) != 0) {
            int len = reader.read(numBits);
            reader.skip(len);
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SerializationUtil$SupportedVersions.class */
    public static class SupportedVersions {
        private final int[] versions;

        public SupportedVersions(int... versions) {
            this(true, versions);
        }

        protected SupportedVersions(boolean copy, int... versions) {
            if (versions == null) {
                throw new NullPointerException("Versions must not be null!");
            }
            if (versions.length == 0) {
                throw new IllegalArgumentException("Versions must not be empty!");
            }
            this.versions = copy ? Arrays.copyOf(versions, versions.length) : versions;
            if (supports(0)) {
                throw new IllegalArgumentException("Versions must not contain NO_VERSION!");
            }
        }

        public boolean supports(int readVersion) {
            for (int version : this.versions) {
                if (readVersion == version) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            return Arrays.toString(this.versions);
        }

        public SupportedVersionsMatcher matcher() {
            return new SupportedVersionsMatcher(this.versions);
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SerializationUtil$SupportedVersionsMatcher.class */
    public static class SupportedVersionsMatcher extends SupportedVersions {
        private int readVersion;

        private SupportedVersionsMatcher(int... versions) {
            super(false, versions);
            this.readVersion = 0;
        }

        @Override // org.eclipse.californium.elements.util.SerializationUtil.SupportedVersions
        public boolean supports(int readVersion) {
            if (super.supports(readVersion)) {
                this.readVersion = readVersion;
                return true;
            }
            this.readVersion = 0;
            return false;
        }

        public int getReadVersion() {
            return this.readVersion;
        }
    }
}
