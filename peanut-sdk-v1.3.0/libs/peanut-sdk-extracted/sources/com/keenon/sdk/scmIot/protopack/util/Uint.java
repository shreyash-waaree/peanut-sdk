package com.keenon.sdk.scmIot.protopack.util;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/util/Uint.class */
public class Uint extends Number implements Comparable<Uint> {
    private static final long serialVersionUID = 1;
    private long value;

    public Uint(int value) {
        if (value < 0) {
            String s = Integer.toBinaryString(value);
            this.value = Long.valueOf(s, 2).longValue();
        } else {
            this.value = value;
        }
    }

    public Uint(long value) {
        this.value = value;
    }

    public Uint(String value) {
        this.value = Long.valueOf(value).longValue();
    }

    public static Uint toUInt(int value) {
        return new Uint(value);
    }

    public static void main(String[] args) {
        Uint u = new Uint(-5);
        System.err.println(u);
    }

    public String toString() {
        return Long.toString(this.value);
    }

    public int hashCode() {
        int result = (31 * 1) + ((int) (this.value ^ (this.value >>> 32)));
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Uint other = (Uint) obj;
        if (this.value != other.value) {
            return false;
        }
        return true;
    }

    @Override // java.lang.Comparable
    public int compareTo(Uint o) {
        return (int) (this.value - o.longValue());
    }

    @Override // java.lang.Number
    public int intValue() {
        return (int) this.value;
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
}
