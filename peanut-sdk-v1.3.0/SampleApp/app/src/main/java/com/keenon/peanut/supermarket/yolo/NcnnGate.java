package com.keenon.peanut.supermarket.yolo;

/**
 * Global lock for all {@code libyolov5.so} / NCNN JNI calls. The native manager is not
 * re-entrant and uses process-wide state — only one detect/init may run at a time
 * across COUNT ({@link com.keenon.peanut.supermarket.vision.TrayPlateCounter}),
 * YOLO ({@link YoloDetectorHelper}), hand ({@link HandDetectorHelper}), and body
 * ({@link BodyDetectorHelper}).
 */
public final class NcnnGate {

    public static final Object LOCK = new Object();

    private NcnnGate() {}
}
