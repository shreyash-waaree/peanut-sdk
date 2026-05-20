package com.keenon.peanut.supermarket.yolo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;

/**
 * Prepares JPEG bytes and JNI size hints for {@code KNYoloExecutor.detectData} on rk3288.
 *
 * <p>Decompiled {@code libyolov5.so} ({@code YoloV4::detect}): when {@code srcW == modelW}
 * native code skips {@code IMGUtil::jpeg2Rgb888} and calls {@code ncnn::Mat::from_pixels}
 * on the buffer as raw RGB. Our buffers are JPEG — that path SIGSEGVs. Always pass
 * {@link #JNI_SRC_W} ({@value #JNI_SRC_W}) so {@code srcW != modelW} and JPEG is decoded first.</p>
 */
public final class NcnnModelJpeg {

    private static final String TAG = "NcnnModelJpeg";
    public static final int MODEL_W = 640;
    public static final int MODEL_H = 480;

    /** Passed as detectData src width — must differ from {@link #MODEL_W} to force JPEG decode. */
    public static final int JNI_SRC_W = MODEL_W + 1;
    public static final int JNI_SRC_H = MODEL_H;
    private static final int MAX_JPEG_BYTES = 2 * 1024 * 1024;
    private static final int JPEG_QUALITY = 85;

    private NcnnModelJpeg() {}

    public static boolean isLikelyJpeg(byte[] b) {
        return b != null
                && b.length >= 4
                && b[0] == (byte) 0xFF
                && b[1] == (byte) 0xD8
                && b[b.length - 2] == (byte) 0xFF
                && b[b.length - 1] == (byte) 0xD9;
    }

    public static boolean isJpegDecodable(byte[] jpeg) {
        if (jpeg == null || jpeg.length < 4) return false;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opts);
        return opts.outWidth > 0 && opts.outHeight > 0;
    }

    /**
     * Re-encode at exactly {@link #MODEL_W}×{@link #MODEL_H} for native detect.
     */
    public static byte[] forModelInput(byte[] jpeg) {
        if (jpeg == null || jpeg.length < 4 || jpeg.length > MAX_JPEG_BYTES) return null;
        if (!isLikelyJpeg(jpeg) && !isJpegDecodable(jpeg)) return null;
        Bitmap decoded = null;
        Bitmap scaled = null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, opts);
            if (decoded == null) return null;
            final int w = decoded.getWidth();
            final int h = decoded.getHeight();
            if (w <= 0 || h <= 0) return null;
            if (w == MODEL_W && h == MODEL_H) {
                scaled = decoded;
                decoded = null;
            } else {
                scaled = Bitmap.createScaledBitmap(decoded, MODEL_W, MODEL_H, true);
                if (scaled == null) return null;
            }
            ByteArrayOutputStream out =
                    new ByteArrayOutputStream(Math.max(32768, jpeg.length / 2 + 1024));
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) return null;
            byte[] result = out.toByteArray();
            if (!isLikelyJpeg(result)) return null;
            return result;
        } catch (Throwable t) {
            Log.w(TAG, "forModelInput failed", t);
            return null;
        } finally {
            if (decoded != null && !decoded.isRecycled()) decoded.recycle();
            if (scaled != null && scaled != decoded && !scaled.isRecycled()) scaled.recycle();
        }
    }
}
