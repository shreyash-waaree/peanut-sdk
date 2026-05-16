package com.keenon.common.utils;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/PeanutMainThreadExecutor.class */
public class PeanutMainThreadExecutor implements Executor {
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override // java.util.concurrent.Executor
    public void execute(Runnable r) {
        this.handler.post(r);
    }

    public void release() {
        if (this.handler != null) {
            this.handler.removeCallbacksAndMessages(null);
        }
    }
}
