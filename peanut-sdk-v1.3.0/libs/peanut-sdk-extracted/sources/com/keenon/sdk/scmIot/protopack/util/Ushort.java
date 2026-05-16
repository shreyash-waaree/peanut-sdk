package com.keenon.sdk.scmIot.protopack.util;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/util/Ushort.class */
public class Ushort extends Number implements Comparable<Ushort> {
    private static final long serialVersionUID = 1;
    private int value;

    public Ushort(short value) {
        if (value < 0) {
            int h = (value >>> 8) & 255;
            String s = Integer.toBinaryString(((h << 8) | value) & 65535);
            this.value = Integer.valueOf(s, 2).intValue();
            return;
        }
        this.value = value;
    }

    public Ushort(int value) {
        this.value = value;
    }

    public Ushort(String value) {
        this.value = Integer.valueOf(value).intValue();
    }

    public static Ushort toUInt(int value) {
        return new Ushort(value);
    }

    public String toString() {
        return Integer.toString(this.value);
    }

    public int hashCode() {
        int result = (31 * 1) + (this.value ^ (this.value >>> 32));
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Ushort other = (Ushort) obj;
        if (this.value != other.value) {
            return false;
        }
        return true;
    }

    @Override // java.lang.Comparable
    public int compareTo(Ushort o) {
        return this.value - o.intValue();
    }

    @Override // java.lang.Number
    public int intValue() {
        return this.value;
    }

    @Override // java.lang.Number
    public long longValue() {
        return this.value;
    }

    @Override // java.lang.Number
    public float floatValue() {
        return this.value;
    }

    @Override // java.lang.Number
    public double doubleValue() {
        return this.value;
    }

    @Override // java.lang.Number
    public short shortValue() {
        return (short) this.value;
    }
}
