package org.eclipse.californium.core.coap;

import java.util.Arrays;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.StringUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/Option.class */
public class Option implements Comparable<Option> {
    private final int number;
    private byte[] value;

    public Option() {
        this.number = 0;
        setValue(Bytes.EMPTY);
    }

    public Option(int number) {
        this.number = number;
    }

    public Option(int number, String str) {
        this.number = number;
        setStringValue(str);
    }

    public Option(int number, int val) {
        this.number = number;
        setIntegerValue(val);
    }

    public Option(int number, long val) {
        this.number = number;
        setLongValue(val);
    }

    public Option(int number, byte[] opaque) {
        this.number = number;
        setValue(opaque);
    }

    public int getLength() {
        return getValue().length;
    }

    public int getNumber() {
        return this.number;
    }

    public byte[] getValue() {
        if (this.value == null) {
            String name = OptionNumberRegistry.toString(this.number);
            throw new IllegalStateException(name + " option value must be set before!");
        }
        return this.value;
    }

    public String getStringValue() {
        return new String(getValue(), CoAP.UTF8_CHARSET);
    }

    public int getIntegerValue() {
        int ret = 0;
        byte[] value = getValue();
        for (int i = 0; i < value.length; i++) {
            ret += (value[(value.length - i) - 1] & 255) << (i * 8);
        }
        return ret;
    }

    public long getLongValue() {
        long ret = 0;
        byte[] value = getValue();
        for (int i = 0; i < value.length; i++) {
            ret += ((long) (value[(value.length - i) - 1] & 255)) << (i * 8);
        }
        return ret;
    }

    public void setValue(byte[] value) {
        if (value == null) {
            String name = OptionNumberRegistry.toString(this.number);
            throw new NullPointerException(name + " option value must not be null!");
        }
        OptionNumberRegistry.assertValueLength(this.number, value.length);
        this.value = value;
    }

    public void setStringValue(String str) {
        setValue(str == null ? null : str.getBytes(CoAP.UTF8_CHARSET));
    }

    public void setIntegerValue(int val) {
        int length = ((32 - Integer.numberOfLeadingZeros(val)) + 7) / 8;
        byte[] value = new byte[length];
        for (int i = 0; i < length; i++) {
            value[(length - i) - 1] = (byte) (val >> (i * 8));
        }
        setValue(value);
    }

    public void setLongValue(long val) {
        int length = ((64 - Long.numberOfLeadingZeros(val)) + 7) / 8;
        byte[] value = new byte[length];
        for (int i = 0; i < length; i++) {
            value[(length - i) - 1] = (byte) (val >> (i * 8));
        }
        setValue(value);
    }

    public boolean isCritical() {
        return (this.number & 1) != 0;
    }

    public boolean isUnSafe() {
        return (this.number & 2) != 0;
    }

    public boolean isNoCacheKey() {
        return (this.number & 30) == 28;
    }

    @Override // java.lang.Comparable
    public int compareTo(Option o) {
        return this.number - o.number;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Option)) {
            return false;
        }
        Option op = (Option) o;
        return this.number == op.number && Arrays.equals(this.value, op.value);
    }

    public int hashCode() {
        return (this.number * 31) + Arrays.hashCode(this.value);
    }

    public String toString() {
        return OptionNumberRegistry.toString(this.number) + ": " + toValueString();
    }

    public String toValueString() {
        if (this.value == null) {
            return "not available";
        }
        switch (OptionNumberRegistry.getFormatByNr(this.number)) {
            case INTEGER:
                if (this.number == 27 || this.number == 23) {
                    return "\"" + new BlockOption(this.value) + "\"";
                }
                int iValue = getIntegerValue();
                if (this.number == 17 || this.number == 12) {
                    return "\"" + MediaTypeRegistry.toString(iValue) + "\"";
                }
                if (this.number == 258) {
                    return "\"" + new NoResponseOption(iValue) + "\"";
                }
                return Long.toString(getLongValue());
            case STRING:
                return "\"" + getStringValue() + "\"";
            case EMPTY:
                return "";
            default:
                return "0x" + StringUtil.byteArray2Hex(this.value);
        }
    }

    Option setValueUnchecked(byte[] value) {
        this.value = value;
        return this;
    }
}
