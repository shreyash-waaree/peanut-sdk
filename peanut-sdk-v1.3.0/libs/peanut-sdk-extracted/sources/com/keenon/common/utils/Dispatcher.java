package com.keenon.common.utils;

import com.keenon.common.constant.PeanutConstants;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/Dispatcher.class */
public final class Dispatcher {
    private static final String TAG = "[Dispatcher]";
    private static final int MAXIMUM_CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = Integer.MAX_VALUE;
    private static final int MAXIMUM_REQUESTS_SIZE = 1;
    private final Deque<NodeCall> readyAsyncCalls = new ArrayDeque();
    private final Deque<NodeCall> runningAsyncCalls = new ArrayDeque();
    private int maxRequests = 1;
    private Runnable idleCallback;
    private ExecutorService executorService;

    public Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
        if (executorService == null) {
            this.executorService = executorService();
        }
    }

    private static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return new ThreadFactory() { // from class: com.keenon.common.utils.Dispatcher.1
            @Override // java.util.concurrent.ThreadFactory
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable, name);
                result.setDaemon(daemon);
                return result;
            }
        };
    }

    private synchronized ExecutorService executorService() {
        if (this.executorService == null) {
            this.executorService = new ThreadPoolExecutor(1, MAXIMUM_POOL_SIZE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), threadFactory("Dispatcher#", false));
        }
        return this.executorService;
    }

    public synchronized int getMaxRequests() {
        return this.maxRequests;
    }

    public synchronized void setMaxRequests(int maxRequests) {
        if (maxRequests < 1) {
            maxRequests = 1;
        }
        this.maxRequests = maxRequests;
        promoteCalls();
    }

    public synchronized void setIdleCallback(Runnable idleCallback) {
        this.idleCallback = idleCallback;
    }

    public synchronized void enqueue(NodeCall call) {
        if (this.runningAsyncCalls.size() < this.maxRequests) {
            executorService().execute(call);
            this.runningAsyncCalls.add(call);
        } else {
            this.readyAsyncCalls.add(call);
        }
    }

    public synchronized void cancelAll() {
        if (this.readyAsyncCalls != null) {
            this.readyAsyncCalls.clear();
        }
        if (this.runningAsyncCalls != null) {
            this.runningAsyncCalls.clear();
        }
    }

    private void promoteCalls() {
        if (this.runningAsyncCalls.size() < this.maxRequests && !this.readyAsyncCalls.isEmpty()) {
            Iterator<NodeCall> it = this.readyAsyncCalls.iterator();
            while (it.hasNext()) {
                try {
                    NodeCall call = it.next();
                    it.remove();
                    executorService().execute(call);
                    this.runningAsyncCalls.add(call);
                } catch (Exception e) {
                    LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
                }
                if (this.runningAsyncCalls.size() >= this.maxRequests) {
                    return;
                }
            }
        }
    }

    private void promoteNextNodeCall(NodeCall finishNodeCall) {
        if (this.runningAsyncCalls.size() < this.maxRequests && !this.readyAsyncCalls.isEmpty()) {
            String nextNodeName = finishNodeCall.nextNodeName;
            Iterator<NodeCall> it = this.readyAsyncCalls.iterator();
            while (it.hasNext()) {
                try {
                    NodeCall call = it.next();
                    if (call != null && android.text.TextUtils.equals(call.nodeName, nextNodeName)) {
                        it.remove();
                        executorService().execute(call);
                        this.runningAsyncCalls.add(call);
                        if (this.runningAsyncCalls.size() >= this.maxRequests) {
                            return;
                        }
                    }
                } catch (Exception e) {
                    LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
                }
            }
        }
    }

    public void executeNextNodeCall(NodeCall call) {
        finished(call);
    }

    void finished(NodeCall call) {
        finished(this.runningAsyncCalls, call, true);
    }

    private void finished(Deque<NodeCall> calls, NodeCall call, boolean promoteCalls) {
        int runningCallsCount;
        Runnable idleCallback;
        synchronized (this) {
            calls.remove(call);
            if (call != null && call.isEnableNextNode) {
                LogUtils.d(PeanutConstants.TAG_UTIL, "[Dispatcher][执行规定Next节点任务:" + call.nextNodeName + "]");
                promoteNextNodeCall(call);
            } else if (promoteCalls) {
                LogUtils.d(PeanutConstants.TAG_UTIL, "[Dispatcher][执行下个节点任务]");
                promoteCalls();
            }
            runningCallsCount = runningCallsCount();
            idleCallback = this.idleCallback;
        }
        if (runningCallsCount == 0 && idleCallback != null) {
            LogUtils.d(PeanutConstants.TAG_UTIL, "[Dispatcher][执行idle节点任务]");
            idleCallback.run();
        }
    }

    public synchronized List<Runnable> queuedCalls() {
        List<Runnable> result = new ArrayList<>();
        for (Runnable asyncCall : this.readyAsyncCalls) {
            result.add(asyncCall);
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized List<NodeCall> runningCalls() {
        List<NodeCall> result = new ArrayList<>();
        for (NodeCall asyncCall : this.runningAsyncCalls) {
            result.add(asyncCall);
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized int queuedCallsCount() {
        return this.readyAsyncCalls.size();
    }

    public synchronized int runningCallsCount() {
        return this.runningAsyncCalls.size();
    }

    public void release() {
        this.idleCallback = null;
        if (this.readyAsyncCalls != null) {
            this.readyAsyncCalls.clear();
        }
        if (this.runningAsyncCalls != null) {
            this.runningAsyncCalls.clear();
        }
        try {
            if (this.executorService != null) {
                this.executorService.shutdown();
                this.executorService = null;
            }
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/Dispatcher$NodeCall.class */
    public static class NodeCall implements Runnable {
        public final String nodeName;
        public final String nextNodeName;
        final Dispatcher dispatcher;
        public boolean isEnableNextNode = false;
        public boolean isEndCallExecuteFinish;

        public NodeCall(String nodeName, String nextNodeName, boolean isCallEndExecuteFinish, Dispatcher dispatcher) {
            this.isEndCallExecuteFinish = true;
            this.dispatcher = dispatcher;
            this.nodeName = nodeName;
            this.nextNodeName = nextNodeName;
            this.isEndCallExecuteFinish = isCallEndExecuteFinish;
        }

        @Override // java.lang.Runnable
        public final void run() {
            long startTime = 0;
            String oldName = Thread.currentThread().getName();
            Thread.currentThread().setName(this.nodeName);
            try {
                try {
                    startTime = System.currentTimeMillis();
                    boolean isInterceptExecute = interceptExecute();
                    if (!isInterceptExecute) {
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[Dispatcher][节点任务执行:" + this.nodeName + " " + isInterceptExecute + "]");
                        execute();
                    } else {
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[Dispatcher][节点任务中止:" + this.nodeName + " " + isInterceptExecute + "]");
                    }
                    Thread.currentThread().setName(oldName);
                    long expireTime = System.currentTimeMillis() - startTime;
                    LogUtils.d(PeanutConstants.TAG_UTIL, "[Dispatcher][节点任务结束:" + this.nodeName + ", 耗时:" + expireTime + "ms]");
                    if (this.dispatcher != null && this.isEndCallExecuteFinish) {
                        this.dispatcher.finished(this);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.currentThread().setName(oldName);
                    long expireTime2 = System.currentTimeMillis() - startTime;
                    LogUtils.d(PeanutConstants.TAG_UTIL, "[Dispatcher][节点任务结束:" + this.nodeName + ", 耗时:" + expireTime2 + "ms]");
                    if (this.dispatcher != null && this.isEndCallExecuteFinish) {
                        this.dispatcher.finished(this);
                    }
                }
            } catch (Throwable th) {
                Thread.currentThread().setName(oldName);
                long expireTime3 = System.currentTimeMillis() - startTime;
                LogUtils.d(PeanutConstants.TAG_UTIL, "[Dispatcher][节点任务结束:" + this.nodeName + ", 耗时:" + expireTime3 + "ms]");
                if (this.dispatcher != null && this.isEndCallExecuteFinish) {
                    this.dispatcher.finished(this);
                }
                throw th;
            }
        }

        protected void execute() {
        }

        protected boolean interceptExecute() {
            return false;
        }
    }
}
