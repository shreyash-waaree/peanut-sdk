package com.keenon.common.utils;

import com.keenon.common.constant.PeanutConstants;
import java.lang.Thread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/PeanutThreadFactory.class */
public class PeanutThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final String namePrefix;

    public PeanutThreadFactory(String threadName) {
        SecurityManager s = System.getSecurityManager();
        this.group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = threadName + " No." + poolNumber.getAndIncrement() + ", thread No.";
    }

    @Override // java.util.concurrent.ThreadFactory
    public Thread newThread(Runnable runnable) {
        String threadName = this.namePrefix + this.threadNumber.getAndIncrement();
        LogUtils.v(PeanutConstants.TAG_UTIL, "PeanutThread: Thread production, name is [" + threadName + "]");
        Thread thread = new Thread(this.group, runnable, threadName, 0L);
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != 5) {
            thread.setPriority(5);
        }
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() { // from class: com.keenon.common.utils.PeanutThreadFactory.1
            @Override // java.lang.Thread.UncaughtExceptionHandler
            public void uncaughtException(Thread thread2, Throwable ex) {
                LogUtils.e(PeanutConstants.TAG_UTIL, "PeanutThread: Running task appeared exception! Thread [" + thread2.getName() + "], because [" + ex.getMessage() + "]");
            }
        });
        return thread;
    }
}
