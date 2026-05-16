package org.eclipse.californium.elements.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/NamedThreadFactory.class */
public class NamedThreadFactory implements ThreadFactory {
    public static final ThreadGroup COAP_THREAD_GROUP = new ThreadGroup("Californium");
    public static final ThreadGroup SCANDIUM_THREAD_GROUP = new ThreadGroup("Scandium");
    private final ThreadGroup group;
    private final AtomicInteger index;
    private final String prefix;

    static {
        COAP_THREAD_GROUP.setDaemon(false);
        SCANDIUM_THREAD_GROUP.setDaemon(false);
    }

    public NamedThreadFactory(String threadPrefix) {
        this(threadPrefix, null);
    }

    public NamedThreadFactory(String threadPrefix, ThreadGroup threadGroup) {
        this.index = new AtomicInteger(1);
        this.group = null == threadGroup ? COAP_THREAD_GROUP : threadGroup;
        this.prefix = threadPrefix;
    }

    @Override // java.util.concurrent.ThreadFactory
    public final Thread newThread(Runnable runnable) {
        Thread ret = new Thread(this.group, runnable, this.prefix + this.index.getAndIncrement(), 0L);
        ret.setDaemon(createDaemonThreads());
        if (ret.getPriority() != 5) {
            ret.setPriority(5);
        }
        return ret;
    }

    protected boolean createDaemonThreads() {
        return false;
    }
}
