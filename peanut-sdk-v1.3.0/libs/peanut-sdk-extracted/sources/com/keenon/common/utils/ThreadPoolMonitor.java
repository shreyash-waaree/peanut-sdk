package com.keenon.common.utils;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/ThreadPoolMonitor.class */
public class ThreadPoolMonitor extends ThreadPoolExecutor {
    private final Logger logger;
    int ac;
    private AtomicLong totalCostTime;
    private AtomicLong totalTasks;
    private String poolName;
    private long minCostTime;
    private long maxCostTime;
    private ThreadLocal<Long> startTime;

    public ThreadPoolMonitor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, String poolName) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), poolName);
    }

    public ThreadPoolMonitor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, String poolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.logger = LoggerFactory.getLogger(getClass());
        this.ac = 0;
        this.totalCostTime = new AtomicLong();
        this.totalTasks = new AtomicLong();
        this.startTime = new ThreadLocal<>();
        this.poolName = poolName;
    }

    public static ExecutorService newFixedThreadPool(int nThreads, String poolName) {
        return new ThreadPoolMonitor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), poolName);
    }

    public static ExecutorService newCachedThreadPool(String poolName) {
        return new ThreadPoolMonitor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), poolName);
    }

    public static ExecutorService newSingleThreadExecutor(String poolName) {
        return new ThreadPoolMonitor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), poolName);
    }

    @Override // java.util.concurrent.ThreadPoolExecutor, java.util.concurrent.ExecutorService
    public void shutdown() {
        this.logger.info("{} Going to shutdown. Executed tasks: {}, Running tasks: {}, Pending tasks: {}", new Object[]{this.poolName, Long.valueOf(getCompletedTaskCount()), Integer.valueOf(getActiveCount()), Integer.valueOf(getQueue().size())});
        super.shutdown();
    }

    @Override // java.util.concurrent.ThreadPoolExecutor, java.util.concurrent.ExecutorService
    public List<Runnable> shutdownNow() {
        this.logger.info("{} Going to immediately shutdown. Executed tasks: {}, Running tasks: {}, Pending tasks: {}", new Object[]{this.poolName, Long.valueOf(getCompletedTaskCount()), Integer.valueOf(getActiveCount()), Integer.valueOf(getQueue().size())});
        return super.shutdownNow();
    }

    @Override // java.util.concurrent.ThreadPoolExecutor
    protected void beforeExecute(Thread t, Runnable r) {
        this.startTime.set(Long.valueOf(System.currentTimeMillis()));
    }

    @Override // java.util.concurrent.ThreadPoolExecutor
    protected void afterExecute(Runnable r, Throwable t) {
        long costTime = System.currentTimeMillis() - this.startTime.get().longValue();
        this.startTime.remove();
        this.maxCostTime = this.maxCostTime > costTime ? this.maxCostTime : costTime;
        if (this.totalTasks.get() == 0) {
            this.minCostTime = costTime;
        }
        this.minCostTime = this.minCostTime < costTime ? this.minCostTime : costTime;
        this.totalCostTime.addAndGet(costTime);
        this.totalTasks.incrementAndGet();
        this.ac = getActiveCount();
        if (costTime > 500) {
            this.logger.info("{}-pool-monitor MaxTimes500: Duration: {} ms, PoolSize: {}, CorePoolSize: {}, ActiveCount: {}, Completed: {}, Task: {}, Queue: {}, LargestPoolSize: {}, MaximumPoolSize: {},  KeepAliveTime: {}, isShutdown: {}, isTerminated: {}", new Object[]{this.poolName, Long.valueOf(costTime), Integer.valueOf(getPoolSize()), Integer.valueOf(getCorePoolSize()), Integer.valueOf(super.getActiveCount()), Long.valueOf(getCompletedTaskCount()), Long.valueOf(getTaskCount()), Integer.valueOf(getQueue().size()), Integer.valueOf(getLargestPoolSize()), Integer.valueOf(getMaximumPoolSize()), Long.valueOf(getKeepAliveTime(TimeUnit.MILLISECONDS)), Boolean.valueOf(isShutdown()), Boolean.valueOf(isTerminated())});
            return;
        }
        if (costTime > 200) {
            this.logger.info("{}-pool-monitor MaxTimes200: Duration: {} ms, PoolSize: {}, CorePoolSize: {}, ActiveCount: {}, Completed: {}, Task: {}, Queue: {}, LargestPoolSize: {}, MaximumPoolSize: {},  KeepAliveTime: {}, isShutdown: {}, isTerminated: {}", new Object[]{this.poolName, Long.valueOf(costTime), Integer.valueOf(getPoolSize()), Integer.valueOf(getCorePoolSize()), Integer.valueOf(super.getActiveCount()), Long.valueOf(getCompletedTaskCount()), Long.valueOf(getTaskCount()), Integer.valueOf(getQueue().size()), Integer.valueOf(getLargestPoolSize()), Integer.valueOf(getMaximumPoolSize()), Long.valueOf(getKeepAliveTime(TimeUnit.MILLISECONDS)), Boolean.valueOf(isShutdown()), Boolean.valueOf(isTerminated())});
        } else if (costTime > 50) {
            this.logger.info("{}-pool-monitor MaxTimes50: Duration: {} ms, PoolSize: {}, CorePoolSize: {}, ActiveCount: {}, Completed: {}, Task: {}, Queue: {}, LargestPoolSize: {}, MaximumPoolSize: {},  KeepAliveTime: {}, isShutdown: {}, isTerminated: {}", new Object[]{this.poolName, Long.valueOf(costTime), Integer.valueOf(getPoolSize()), Integer.valueOf(getCorePoolSize()), Integer.valueOf(super.getActiveCount()), Long.valueOf(getCompletedTaskCount()), Long.valueOf(getTaskCount()), Integer.valueOf(getQueue().size()), Integer.valueOf(getLargestPoolSize()), Integer.valueOf(getMaximumPoolSize()), Long.valueOf(getKeepAliveTime(TimeUnit.MILLISECONDS)), Boolean.valueOf(isShutdown()), Boolean.valueOf(isTerminated())});
        } else {
            this.logger.info("{}-pool-monitor: Duration: {} ms, PoolSize: {}, CorePoolSize: {}, ActiveCount: {}, Completed: {}, Task: {}, Queue: {}, LargestPoolSize: {}, MaximumPoolSize: {},  KeepAliveTime: {}, isShutdown: {}, isTerminated: {}", new Object[]{this.poolName, Long.valueOf(costTime), Integer.valueOf(getPoolSize()), Integer.valueOf(getCorePoolSize()), Integer.valueOf(super.getActiveCount()), Long.valueOf(getCompletedTaskCount()), Long.valueOf(getTaskCount()), Integer.valueOf(getQueue().size()), Integer.valueOf(getLargestPoolSize()), Integer.valueOf(getMaximumPoolSize()), Long.valueOf(getKeepAliveTime(TimeUnit.MILLISECONDS)), Boolean.valueOf(isShutdown()), Boolean.valueOf(isTerminated())});
        }
    }

    public int getAc() {
        return this.ac;
    }

    public float getAverageCostTime() {
        return this.totalCostTime.get() / this.totalTasks.get();
    }

    public long getMaxCostTime() {
        return this.maxCostTime;
    }

    public long getMinCostTime() {
        return this.minCostTime;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/ThreadPoolMonitor$EventThreadFactory.class */
    static class EventThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        EventThreadFactory(String poolName) {
            SecurityManager s = System.getSecurityManager();
            this.group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = poolName + "-pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override // java.util.concurrent.ThreadFactory
        public Thread newThread(Runnable r) {
            Thread t = new Thread(this.group, r, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != 5) {
                t.setPriority(5);
            }
            return t;
        }
    }
}
