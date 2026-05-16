package org.eclipse.californium.elements.util;

import com.keenon.common.constant.PeanutConstants;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/ExecutorsUtil.class */
public class ExecutorsUtil {
    private static final int SPLIT_THRESHOLD = 1;
    private static final Boolean REMOVE_ON_CANCEL;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorsUtil.class);
    private static final Runnable WARMUP = new Runnable() { // from class: org.eclipse.californium.elements.util.ExecutorsUtil.1
        @Override // java.lang.Runnable
        public void run() {
            ExecutorsUtil.LOGGER.trace("warmup ...");
        }
    };
    public static final ThreadGroup TIMER_THREAD_GROUP = new ThreadGroup("Timer");
    private static final Boolean DEFAULT_REMOVE_ON_CANCEL = true;

    static {
        Boolean remove = StringUtil.getConfigurationBoolean("EXECUTER_REMOVE_ON_CANCEL");
        if (remove == null) {
            remove = DEFAULT_REMOVE_ON_CANCEL;
        }
        if (remove != null) {
            try {
                ScheduledThreadPoolExecutor.class.getMethod("setRemoveOnCancelPolicy", Boolean.TYPE);
            } catch (NoSuchMethodException e) {
                remove = null;
            }
        }
        REMOVE_ON_CANCEL = remove;
    }

    public static ScheduledExecutorService newScheduledThreadPool(int poolSize, ThreadFactory threadFactory) {
        if (poolSize <= 1) {
            LOGGER.trace("create scheduled thread pool of {} threads", Integer.valueOf(poolSize));
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(poolSize, threadFactory);
            setRemoveOnCancelPolicy(executor);
            executor.execute(WARMUP);
            return executor;
        }
        LOGGER.trace("create special thread pool of {} threads", Integer.valueOf(poolSize));
        SplitScheduledThreadPoolExecutor executor2 = new SplitScheduledThreadPoolExecutor(poolSize, threadFactory);
        executor2.execute(WARMUP);
        executor2.schedule(WARMUP, 0L, TimeUnit.NANOSECONDS);
        return executor2;
    }

    public static ExecutorService newFixedThreadPool(int poolSize, ThreadFactory threadFactory) {
        LOGGER.trace("create thread pool of {} threads", Integer.valueOf(poolSize));
        ExecutorService executor = Executors.newFixedThreadPool(poolSize, threadFactory);
        executor.execute(WARMUP);
        return executor;
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        LOGGER.trace("create scheduled single thread pool");
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        setRemoveOnCancelPolicy(executor);
        executor.execute(WARMUP);
        return executor;
    }

    public static ScheduledThreadPoolExecutor newDefaultSecondaryScheduler(String namePrefix) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory(namePrefix));
        setRemoveOnCancelPolicy(executor);
        executor.execute(WARMUP);
        executor.prestartAllCoreThreads();
        return executor;
    }

    public static void shutdownExecutorGracefully(long timeMaxToWaitInMs, ExecutorService... executors) {
        if (executors.length == 0) {
            return;
        }
        long start = ClockUtil.nanoRealtime();
        for (ExecutorService executorService : executors) {
            executorService.shutdown();
        }
        long time = TimeUnit.NANOSECONDS.toMillis(ClockUtil.nanoRealtime() - start);
        if (time > timeMaxToWaitInMs) {
            LOGGER.warn("shutdown {} ms exceeded the maximum {} ms", Long.valueOf(time), Long.valueOf(timeMaxToWaitInMs));
        }
        try {
            try {
                long timeToWait = (timeMaxToWaitInMs / ((long) executors.length)) / 2;
                for (ExecutorService executor : executors) {
                    if (!executor.awaitTermination(timeToWait, TimeUnit.MILLISECONDS)) {
                        List<Runnable> runningTasks = executor.shutdownNow();
                        if (runningTasks.size() > 0) {
                            LOGGER.debug("ignoring remaining {} scheduled task(s)", Integer.valueOf(runningTasks.size()));
                        }
                        executor.awaitTermination(timeToWait, TimeUnit.MILLISECONDS);
                    }
                }
                long time2 = TimeUnit.NANOSECONDS.toMillis(ClockUtil.nanoRealtime() - start);
                if (time2 > timeMaxToWaitInMs) {
                    LOGGER.warn("await termination {} ms exceeded the maximum {} ms", Long.valueOf(time2), Long.valueOf(timeMaxToWaitInMs));
                }
            } catch (InterruptedException e) {
                for (ExecutorService executorService2 : executors) {
                    executorService2.shutdownNow();
                }
                Thread.currentThread().interrupt();
                long time3 = TimeUnit.NANOSECONDS.toMillis(ClockUtil.nanoRealtime() - start);
                if (time3 > timeMaxToWaitInMs) {
                    LOGGER.warn("await termination {} ms exceeded the maximum {} ms", Long.valueOf(time3), Long.valueOf(timeMaxToWaitInMs));
                }
            }
        } catch (Throwable th) {
            long time4 = TimeUnit.NANOSECONDS.toMillis(ClockUtil.nanoRealtime() - start);
            if (time4 > timeMaxToWaitInMs) {
                LOGGER.warn("await termination {} ms exceeded the maximum {} ms", Long.valueOf(time4), Long.valueOf(timeMaxToWaitInMs));
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    @NotForAndroid
    public static void setRemoveOnCancelPolicy(ScheduledExecutorService executor) {
        if (REMOVE_ON_CANCEL != null && (executor instanceof ScheduledThreadPoolExecutor)) {
            ((ScheduledThreadPoolExecutor) executor).setRemoveOnCancelPolicy(REMOVE_ON_CANCEL.booleanValue());
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/ExecutorsUtil$SplitScheduledThreadPoolExecutor.class */
    private static class SplitScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
        private final long SCHEDULE_EXECUTOR_LOGGING_QUEUE_SIZE_DIFF_DEFAULT = 10000;
        private final ExecutorService directExecutor;
        private AtomicLong scheduleQueueSize;
        private final long scheduleLoggingQueueSizeDiff;

        public SplitScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
            super(corePoolSize < 1 ? corePoolSize : 1, threadFactory);
            this.SCHEDULE_EXECUTOR_LOGGING_QUEUE_SIZE_DIFF_DEFAULT = PeanutConstants.REQUEST_TIMEOUT;
            this.scheduleQueueSize = new AtomicLong();
            setMaximumPoolSize(corePoolSize < 1 ? corePoolSize : 1);
            Long diff = StringUtil.getConfigurationLong("EXECUTER_LOGGING_QUEUE_SIZE_DIFF");
            this.scheduleLoggingQueueSizeDiff = diff == null ? PeanutConstants.REQUEST_TIMEOUT : diff.longValue();
            ExecutorsUtil.setRemoveOnCancelPolicy(this);
            if (corePoolSize > 1) {
                this.directExecutor = ExecutorsUtil.newFixedThreadPool(corePoolSize - 1, threadFactory);
            } else {
                this.directExecutor = null;
            }
            Logger logger = ExecutorsUtil.LOGGER;
            Object[] objArr = new Object[3];
            objArr[0] = ExecutorsUtil.REMOVE_ON_CANCEL;
            objArr[1] = Boolean.valueOf(this.directExecutor != null);
            objArr[2] = Long.valueOf(this.scheduleLoggingQueueSizeDiff);
            logger.debug("remove on cancel: {}, split: {}, log-diff: {}", objArr);
        }

        @Override // java.util.concurrent.ScheduledThreadPoolExecutor, java.util.concurrent.ThreadPoolExecutor, java.util.concurrent.Executor
        public void execute(Runnable command) {
            if (this.directExecutor == null) {
                super.execute(command);
                return;
            }
            long lastSize = this.scheduleQueueSize.get();
            long size = getQueue().size();
            long diff = Math.abs(lastSize - size);
            if (diff > this.scheduleLoggingQueueSizeDiff && this.scheduleQueueSize.compareAndSet(lastSize, size)) {
                ExecutorsUtil.LOGGER.debug("Job queue {}", Long.valueOf(size));
                purge();
            }
            this.directExecutor.execute(command);
        }

        @Override // java.util.concurrent.ScheduledThreadPoolExecutor, java.util.concurrent.AbstractExecutorService, java.util.concurrent.ExecutorService
        public Future<?> submit(Runnable task) {
            if (this.directExecutor == null) {
                return super.submit(task);
            }
            return this.directExecutor.submit(task);
        }

        @Override // java.util.concurrent.ScheduledThreadPoolExecutor, java.util.concurrent.AbstractExecutorService, java.util.concurrent.ExecutorService
        public <T> Future<T> submit(Runnable task, T result) {
            if (this.directExecutor == null) {
                return super.submit(task, result);
            }
            return this.directExecutor.submit(task, result);
        }

        @Override // java.util.concurrent.ScheduledThreadPoolExecutor, java.util.concurrent.AbstractExecutorService, java.util.concurrent.ExecutorService
        public <T> Future<T> submit(Callable<T> task) {
            if (this.directExecutor == null) {
                return super.submit(task);
            }
            return this.directExecutor.submit(task);
        }

        @Override // java.util.concurrent.ScheduledThreadPoolExecutor, java.util.concurrent.ThreadPoolExecutor, java.util.concurrent.ExecutorService
        public void shutdown() {
            if (this.directExecutor != null) {
                this.directExecutor.shutdown();
            }
            super.shutdown();
        }

        @Override // java.util.concurrent.ScheduledThreadPoolExecutor, java.util.concurrent.ThreadPoolExecutor, java.util.concurrent.ExecutorService
        public List<Runnable> shutdownNow() {
            List<Runnable> result = super.shutdownNow();
            if (this.directExecutor != null) {
                result.addAll(this.directExecutor.shutdownNow());
            }
            return result;
        }

        @Override // java.util.concurrent.ThreadPoolExecutor, java.util.concurrent.ExecutorService
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            if (this.directExecutor != null) {
                if (!this.directExecutor.awaitTermination(timeout / 2, unit)) {
                    return false;
                }
                return super.awaitTermination(timeout / 2, unit);
            }
            return super.awaitTermination(timeout, unit);
        }
    }
}
