package org.eclipse.californium.core.coap;

import org.eclipse.californium.elements.util.Bytes;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/BlockOption.class */
public final class BlockOption {
    public static final int BERT_SZX = 7;
    private final int szx;
    private final boolean m;
    private final int num;

    public BlockOption(int szx, boolean m, int num) {
        if (szx < 0 || 7 < szx) {
            throw new IllegalArgumentException("Block option's szx " + szx + " must be between 0 and 7 inclusive");
        }
        if (num < 0 || 1048575 < num) {
            throw new IllegalArgumentException("Block option's num " + num + " must be between 0 and 524288 inclusive");
        }
        this.szx = szx;
        this.m = m;
        this.num = num;
    }

    public BlockOption(byte[] value) {
        if (value == null) {
            throw new NullPointerException();
        }
        if (value.length > 3) {
            throw new IllegalArgumentException("Block option's length " + value.length + " must be at most 3 bytes inclusive");
        }
        if (value.length == 0) {
            this.szx = 0;
            this.m = false;
            this.num = 0;
            return;
        }
        byte end = value[value.length - 1];
        this.szx = end & 7;
        this.m = ((end >> 3) & 1) == 1;
        int tempNum = (end & 255) >> 4;
        for (int i = 1; i < value.length; i++) {
            tempNum += (value[(value.length - i) - 1] & 255) << ((i * 8) - 4);
        }
        this.num = tempNum;
    }

    public boolean isBERT() {
        return this.szx == 7;
    }

    public void assertPayloadSize(int payloadSize) {
        int size;
        if (this.szx < 7 && payloadSize > 0 && payloadSize > (size = getSize())) {
            throw new IllegalStateException("Message with " + payloadSize + " bytes payload exceeds the blocksize of " + size + " bytes!");
        }
    }

    public int getSzx() {
        return this.szx;
    }

    public int getSize() {
        return szx2Size(this.szx);
    }

    public boolean isM() {
        return this.m;
    }

    public int getNum() {
        return this.num;
    }

    public byte[] getValue() {
        int last = this.szx | (this.m ? 8 : 0);
        if (this.num == 0 && !this.m && this.szx == 0) {
            return Bytes.EMPTY;
        }
        if (this.num < 16) {
            return new byte[]{(byte) (last | (this.num << 4))};
        }
        if (this.num < 4096) {
            return new byte[]{(byte) (this.num >> 4), (byte) (last | (this.num << 4))};
        }
        return new byte[]{(byte) (this.num >> 12), (byte) (this.num >> 4), (byte) (last | (this.num << 4))};
    }

    public int getOffset() {
        return this.num * szx2Size(this.szx);
    }

    public Option toOption(int number) {
        if (number != 27 && number != 23) {
            throw new IllegalArgumentException("Block Option must be either block1(27) or block2(23), not " + number + "!");
        }
        return new Option(number, getValue());
    }

    public String toString() {
        return String.format("(szx=%d/%d, m=%b, num=%d)", Integer.valueOf(this.szx), Integer.valueOf(szx2Size(this.szx)), Boolean.valueOf(this.m), Integer.valueOf(this.num));
    }

    public boolean equals(Object o) {
        if (!(o instanceof BlockOption)) {
            return false;
        }
        BlockOption block = (BlockOption) o;
        return this.szx == block.szx && this.num == block.num && this.m == block.m;
    }

    public int hashCode() {
        int result = this.szx;
        return (31 * ((31 * result) + (this.m ? 1 : 0))) + this.num;
    }

    public static int size2Szx(int blockSize) {
        if (blockSize >= 1024) {
            return 6;
        }
        if (blockSize <= 16) {
            return 0;
        }
        int maxOneBit = Integer.highestOneBit(blockSize);
        return Integer.numberOfTrailingZeros(maxOneBit) - 4;
    }

    public static int szx2Size(int szx) {
        if (szx <= 0) {
            return 16;
        }
        if (szx >= 6) {
            return 1024;
        }
        return 1 << (szx + 4);
    }
}
