package com.keenon.peanut.formbackend.speech;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Minimal RIFF WAVE parser for PCM 16-bit mono (what the Android client sends).
 */
public final class WavPcmReader {

    private WavPcmReader() {}

    public static final class Result {
        public final int sampleRateHertz;
        public final byte[] pcmLittleEndian;

        public Result(int sampleRateHertz, byte[] pcmLittleEndian) {
            this.sampleRateHertz = sampleRateHertz;
            this.pcmLittleEndian = pcmLittleEndian;
        }
    }

    /**
     * @throws IllegalArgumentException if format is not supported
     */
    public static Result parse(byte[] wav) {
        if (wav == null || wav.length < 44) {
            throw new IllegalArgumentException("WAV too short");
        }
        if (!"RIFF".equals(ascii(wav, 0, 4)) || !"WAVE".equals(ascii(wav, 8, 4))) {
            throw new IllegalArgumentException("Not a RIFF WAVE file");
        }
        int offset = 12;
        Integer sampleRate = null;
        Short channels = null;
        Short bitsPerSample = null;
        byte[] pcm = null;

        while (offset + 8 <= wav.length) {
            String chunkId = ascii(wav, offset, 4);
            int chunkSize = readLeInt(wav, offset + 4);
            int dataStart = offset + 8;
            int padded = chunkSize + (chunkSize % 2);
            if ("fmt ".equals(chunkId) && dataStart + chunkSize <= wav.length) {
                int audioFormat = readLeShort(wav, dataStart) & 0xffff;
                channels = readLeShort(wav, dataStart + 2);
                sampleRate = readLeInt(wav, dataStart + 4);
                bitsPerSample = readLeShort(wav, dataStart + 14);
                if (audioFormat != 1) {
                    throw new IllegalArgumentException("Only PCM (format 1) supported, got " + audioFormat);
                }
            } else if ("data".equals(chunkId) && dataStart + chunkSize <= wav.length) {
                pcm = Arrays.copyOfRange(wav, dataStart, dataStart + chunkSize);
            }
            offset = dataStart + padded;
        }

        if (sampleRate == null || channels == null || bitsPerSample == null || pcm == null) {
            throw new IllegalArgumentException("Missing fmt or data chunk in WAV");
        }
        if (channels != 1) {
            throw new IllegalArgumentException("Only mono supported, channels=" + channels);
        }
        if (bitsPerSample != 16) {
            throw new IllegalArgumentException("Only 16-bit PCM supported, bits=" + bitsPerSample);
        }
        return new Result(sampleRate, pcm);
    }

    private static String ascii(byte[] b, int off, int len) {
        return new String(b, off, len, StandardCharsets.US_ASCII);
    }

    private static int readLeInt(byte[] b, int off) {
        return ByteBuffer.wrap(b, off, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static short readLeShort(byte[] b, int off) {
        return ByteBuffer.wrap(b, off, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }
}
