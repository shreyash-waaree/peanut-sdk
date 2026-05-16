package com.keenon.sdk.scmIot.protopack.util;

import java.math.BigInteger;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/util/Ulong.class */
public class Ulong extends Number implements Comparable<Ulong> {
    private static final long serialVersionUID = 1;
    private BigInteger value;

    public Ulong(long i) {
        if (i < 0) {
            String s = Long.toBinaryString(i);
            this.value = new BigInteger(s, 2);
        } else {
            this.value = new BigInteger(i + "");
        }
    }

    public Ulong(String l) {
        this.value = new BigInteger(l);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Ulong other = (Ulong) obj;
        if (this.value != other.value) {
            return false;
        }
        return true;
    }

    public String toString() {
        return this.value.toString();
    }

    @Override // java.lang.Number
    public int intValue() {
        return this.value.intValue();
    }

    @Override // java.lang.Number
    public long longValue() {
        return this.value.longValue();
    }

    @Override // java.lang.Number
    public float floatValue() {
        return this.value.floatValue();
    }

    @Override // java.lang.Number
    public double doubleValue() {
        return this.value.doubleValue();
    }

    @Override // java.lang.Comparable
    public int compareTo(Ulong o) {
        return this.value.divide(o.value).intValue();
    }
}
