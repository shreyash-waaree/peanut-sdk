package org.eclipse.californium.elements.util;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/ClockUtil.class */
public class ClockUtil {
    private static volatile Realtime handler = new Realtime() { // from class: org.eclipse.californium.elements.util.ClockUtil.1
        @Override // org.eclipse.californium.elements.util.ClockUtil.Realtime
        public long nanoRealtime() {
            return System.nanoTime();
        }
    };

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/ClockUtil$Realtime.class */
    public interface Realtime {
        long nanoRealtime();
    }

    public static void setRealtimeHandler(Realtime systemHandler) {
        if (systemHandler == null) {
            throw new NullPointerException("realtime system handler must not be null!");
        }
        handler = systemHandler;
    }

    public static long nanoRealtime() {
        return handler.nanoRealtime();
    }
}
