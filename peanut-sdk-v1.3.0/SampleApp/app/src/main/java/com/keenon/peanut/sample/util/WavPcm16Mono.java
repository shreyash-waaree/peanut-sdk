package com.keenon.peanut.sample.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Builds a minimal RIFF WAVE file from PCM 16-bit little-endian mono samples.
 */
public final class WavPcm16Mono {

    private WavPcm16Mono() {}

    public static byte[] buildWav(byte[] pcm16LeMono, int sampleRate) {
        if (pcm16LeMono == null) {
            pcm16LeMono = new byte[0];
        }
        int dataLen = pcm16LeMono.length;
        int riffChunkSize = 36 + dataLen;
        ByteBuffer buf = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
        buf.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(riffChunkSize);
        buf.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        buf.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(16);
        buf.putShort((short) 1);
        buf.putShort((short) 1);
        buf.putInt(sampleRate);
        int byteRate = sampleRate * 2;
        buf.putInt(byteRate);
        buf.putShort((short) 2);
        buf.putShort((short) 16);
        buf.put("data".getBytes(StandardCharsets.US_ASCII));
        buf.putInt(dataLen);
        buf.put(pcm16LeMono);
        return buf.array();
    }
}
