package com.keenon.peanut.sample.util;

/**
 * Small helper around spoken transcripts. The previous build capped matching to the first
 * three words — that meant phrases like "can you please move forward" never reached the
 * intent detector. Now the whole transcript is used and the cap is effectively unlimited.
 */
public final class VoiceTranscriptUtil {

    /**
     * Kept for compatibility with existing UI strings. No longer enforces a word cap on
     * intent matching — {@link #firstWordsForIntent(String)} returns the full trimmed text.
     */
    public static final int MAX_WORDS_FOR_INTENT = Integer.MAX_VALUE;

    private VoiceTranscriptUtil() {}

    /** Returns the trimmed transcript as-is (no word cap). */
    public static String firstWordsForIntent(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
