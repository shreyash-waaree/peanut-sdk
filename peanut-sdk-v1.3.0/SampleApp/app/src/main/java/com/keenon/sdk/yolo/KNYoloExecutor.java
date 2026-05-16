package com.keenon.sdk.yolo;

import android.os.Build;
import android.util.Log;

import com.keenon.sdk.base.model.KNBox;

/**
 * Shim at the exact package path that libyolov5.so / libgyolov5.so was compiled against.
 * JNI symbols in those libraries are:
 *   Java_com_keenon_sdk_yolo_KNYoloExecutor_init
 *   Java_com_keenon_sdk_yolo_KNYoloExecutor_initModel
 *   Java_com_keenon_sdk_yolo_KNYoloExecutor_detectData
 *   Java_com_keenon_sdk_yolo_KNYoloExecutor_destroyModel
 *
 * This class must live at com.keenon.sdk.yolo so FindClass succeeds at runtime.
 */
public class KNYoloExecutor {

    private static final String TAG = "KNYoloExecutor";

    static {
        try {
            System.loadLibrary("opencv_java4");
            Log.i(TAG, "libopencv_java4.so loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "libopencv_java4.so FAILED: " + e.getMessage());
        }
        try {
            System.loadLibrary("ncnn");
            Log.i(TAG, "libncnn.so loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "libncnn.so FAILED: " + e.getMessage());
        }
        try {
            // API > 23 uses libgyolov5.so, older uses libyolov5.so
            if (Build.VERSION.SDK_INT > 23) {
                System.loadLibrary("gyolov5");
                Log.i(TAG, "libgyolov5.so loaded");
            } else {
                System.loadLibrary("yolov5");
                Log.i(TAG, "libyolov5.so loaded");
            }
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "libyolov5.so/libgyolov5.so FAILED: " + e.getMessage());
        }
    }

    // ---- Exact native signatures from libyolov5.so ----

    /** Must be called once before initModel. size = model index (0 for canpan_detection). */
    public native void init(int size);

    /** Load model from file paths. type = model slot index. */
    public native void initModel(int type, String paramPath, String binPath);

    /** Update model in-place (swap files without reinit). */
    public native void updateModel(int type, String paramPath, String binPath);

    /** Destroy the loaded model and free memory. */
    public native void destroyModel();

    /**
     * Detect on a raw JPEG byte array.
     * Native code returns {@link KNBox} instances (same as production Keenon SDK) — not {@code float[]}.
     *
     * @param type       model slot index
     * @param jpegData   JPEG byte array of the frame
     * @param dataSize   jpegData.length
     * @param threshold  confidence threshold (e.g. 0.40)
     * @param paramPath  full path to .param file
     * @param binPath    full path to .bin file
     * @param inputTag   e.g. "data" or "images"
     * @param inputId    model input id (0 for most models)
     * @param extractTag output layer tag (e.g. "output")
     * @param srcWidth   original frame width
     * @param srcHeight  original frame height
     * @param modelWidth model input width
     * @param modelHeight model input height
     */
    public native KNBox[] detectData(int type, byte[] jpegData, int dataSize,
                                     double threshold,
                                     String paramPath, String binPath,
                                     String inputTag, int inputId, String extractTag,
                                     int srcWidth, int srcHeight,
                                     int modelWidth, int modelHeight);
}
