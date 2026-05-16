package org.eclipse.californium.elements.util;

import java.util.Arrays;
import java.util.Random;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/Bytes.class */
public class Bytes {
    public static final byte[] EMPTY = new byte[0];
    private final byte[] bytes;
    private final int hash;
    private final boolean useClassInEquals;
    private String asString;

    public Bytes(byte[] bytes) {
        this(bytes, 255, false);
    }

    public Bytes(byte[] bytes, int maxLength, boolean copy) {
        this(bytes, maxLength, copy, false);
    }

    public Bytes(byte[] bytes, int maxLength, boolean copy, boolean useClassInEquals) {
        if (bytes == null) {
            throw new NullPointerException("bytes must not be null");
        }
        if (bytes.length > maxLength) {
            throw new IllegalArgumentException("bytes length must be between 0 and " + maxLength + " inclusive");
        }
        this.useClassInEquals = useClassInEquals;
        this.bytes = copy ? Arrays.copyOf(bytes, bytes.length) : bytes;
        this.hash = Arrays.hashCode(bytes);
    }

    public String toString() {
        return "BYTES=" + getAsString();
    }

    public final int hashCode() {
        return this.hash;
    }

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && (obj instanceof Bytes)) {
            Bytes other = (Bytes) obj;
            if (((this.useClassInEquals || other.useClassInEquals) && getClass() != obj.getClass()) || this.hash != other.hash) {
                return false;
            }
            return Arrays.equals(this.bytes, other.bytes);
        }
        return false;
    }

    public final byte[] getBytes() {
        return this.bytes;
    }

    public final String getAsString() {
        if (this.asString == null) {
            this.asString = StringUtil.byteArray2Hex(this.bytes);
        }
        return this.asString;
    }

    public final boolean isEmpty() {
        return this.bytes.length == 0;
    }

    public final int length() {
        return this.bytes.length;
    }

    public static byte[] createBytes(Random generator, int size) {
        byte[] byteArray = new byte[size];
        try {
            generator.nextBytes(byteArray);
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage().contains("Number of bits per request limited ") && size > 4096) {
                byte[] part = new byte[4096];
                int i = 0;
                while (true) {
                    int offset = i;
                    if (offset >= size) {
                        break;
                    }
                    generator.nextBytes(part);
                    int fill = Math.min(size - offset, part.length);
                    System.arraycopy(part, 0, byteArray, offset, fill);
                    i = offset + fill;
                }
            }
        }
        return byteArray;
    }

    public static byte[] concatenate(Bytes a, Bytes b) {
        return concatenate(a.getBytes(), b.getBytes());
    }

    public static byte[] concatenate(byte[] a, byte[] b) {
        int lengthA = a.length;
        int lengthB = b.length;
        byte[] concat = new byte[lengthA + lengthB];
        System.arraycopy(a, 0, concat, 0, lengthA);
        System.arraycopy(b, 0, concat, lengthA, lengthB);
        return concat;
    }

    public static void clear(byte[] data) {
        Arrays.fill(data, (byte) 0);
    }

    public static boolean equals(Bytes a, Bytes b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
