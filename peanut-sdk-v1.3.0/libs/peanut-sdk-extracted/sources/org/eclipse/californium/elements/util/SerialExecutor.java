package org.eclipse.californium.elements.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SerialExecutor.class */
public class SerialExecutor extends AbstractExecutorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialExecutor.class);
    private final Executor executor;
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private final AtomicReference<ExecutionListener> listener = new AtomicReference<>();
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition terminated = this.lock.newCondition();
    private Runnable currentlyExecutedJob;
    private boolean shutdown;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SerialExecutor$ExecutionListener.class */
    public interface ExecutionListener {
        void beforeExecution();

        void afterExecution();
    }

    public SerialExecutor(Executor executor) {
        if (executor == null) {
            this.shutdown = true;
        }
        this.executor = executor;
    }

    @Override // java.util.concurrent.Executor
    public void execute(Runnable command) {
        this.lock.lock();
        try {
            if (this.shutdown) {
                throw new RejectedExecutionException("SerialExecutor already shutdown!");
            }
            this.tasks.offer(command);
            if (this.currentlyExecutedJob == null) {
                scheduleNextJob();
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void assertOwner() {
        Thread me = Thread.currentThread();
        if (this.owner.get() != me) {
            Thread thread = this.owner.get();
            if (thread == null) {
                throw new ConcurrentModificationException(this + " is not owned!");
            }
            throw new ConcurrentModificationException(this + " owned by " + thread.getName() + "!");
        }
    }

    public boolean checkOwner() {
        return this.owner.get() == Thread.currentThread();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setOwner() {
        Thread thread = this.owner.get();
        if (!this.owner.compareAndSet(null, Thread.currentThread())) {
            if (thread == null) {
                throw new ConcurrentModificationException(this + " was already owned!");
            }
            throw new ConcurrentModificationException(this + " already owned by " + thread.getName() + "!");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearOwner() {
        if (!this.owner.compareAndSet(Thread.currentThread(), null)) {
            Thread thread = this.owner.get();
            if (thread == null) {
                throw new ConcurrentModificationException(this + " is not owned, clear failed!");
            }
            throw new ConcurrentModificationException(this + " owned by " + thread.getName() + ", clear failed!");
        }
    }

    @Override // java.util.concurrent.ExecutorService
    public void shutdown() {
        this.lock.lock();
        try {
            this.shutdown = true;
        } finally {
            this.lock.unlock();
        }
    }

    @Override // java.util.concurrent.ExecutorService
    public List<Runnable> shutdownNow() {
        this.lock.lock();
        try {
            List<Runnable> pending = new ArrayList<>(this.tasks.size());
            shutdownNow(pending);
            return pending;
        } finally {
            this.lock.unlock();
        }
    }

    public int shutdownNow(Collection<Runnable> jobs) {
        this.lock.lock();
        try {
            shutdown();
            return this.tasks.drainTo(jobs);
        } finally {
            this.lock.unlock();
        }
    }

    @Override // java.util.concurrent.ExecutorService
    public boolean isShutdown() {
        this.lock.lock();
        try {
            return this.shutdown;
        } finally {
            this.lock.unlock();
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:8:0x0019  */
    @Override // java.util.concurrent.ExecutorService
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean isTerminated() {
        /*
            r2 = this;
            r0 = r2
            java.util.concurrent.locks.ReentrantLock r0 = r0.lock
            r0.lock()
            r0 = r2
            boolean r0 = r0.shutdown     // Catch: java.lang.Throwable -> L24
            if (r0 == 0) goto L19
            r0 = r2
            java.lang.Runnable r0 = r0.currentlyExecutedJob     // Catch: java.lang.Throwable -> L24
            if (r0 != 0) goto L19
            r0 = 1
            goto L1a
        L19:
            r0 = 0
        L1a:
            r3 = r0
            r0 = r2
            java.util.concurrent.locks.ReentrantLock r0 = r0.lock
            r0.unlock()
            r0 = r3
            return r0
        L24:
            r4 = move-exception
            r0 = r2
            java.util.concurrent.locks.ReentrantLock r0 = r0.lock
            r0.unlock()
            r0 = r4
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: org.eclipse.californium.elements.util.SerialExecutor.isTerminated():boolean");
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x0045  */
    @Override // java.util.concurrent.ExecutorService
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean awaitTermination(long r6, java.util.concurrent.TimeUnit r8) throws java.lang.InterruptedException {
        /*
            r5 = this;
            r0 = r5
            java.util.concurrent.locks.ReentrantLock r0 = r0.lock
            r0.lock()
            r0 = r8
            r1 = r6
            long r0 = r0.toNanos(r1)     // Catch: java.lang.Throwable -> L52
            r9 = r0
        Le:
            r0 = r5
            boolean r0 = r0.shutdown     // Catch: java.lang.Throwable -> L52
            if (r0 == 0) goto L1c
            r0 = r5
            java.lang.Runnable r0 = r0.currentlyExecutedJob     // Catch: java.lang.Throwable -> L52
            if (r0 == 0) goto L33
        L1c:
            r0 = r5
            java.util.concurrent.locks.Condition r0 = r0.terminated     // Catch: java.lang.Throwable -> L52
            r1 = r9
            long r0 = r0.awaitNanos(r1)     // Catch: java.lang.Throwable -> L52
            r9 = r0
            r0 = r9
            r1 = 0
            int r0 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1))
            if (r0 > 0) goto Le
            goto L33
        L33:
            r0 = r5
            boolean r0 = r0.shutdown     // Catch: java.lang.Throwable -> L52
            if (r0 == 0) goto L45
            r0 = r5
            java.lang.Runnable r0 = r0.currentlyExecutedJob     // Catch: java.lang.Throwable -> L52
            if (r0 != 0) goto L45
            r0 = 1
            goto L46
        L45:
            r0 = 0
        L46:
            r11 = r0
            r0 = r5
            java.util.concurrent.locks.ReentrantLock r0 = r0.lock
            r0.unlock()
            r0 = r11
            return r0
        L52:
            r12 = move-exception
            r0 = r5
            java.util.concurrent.locks.ReentrantLock r0 = r0.lock
            r0.unlock()
            r0 = r12
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: org.eclipse.californium.elements.util.SerialExecutor.awaitTermination(long, java.util.concurrent.TimeUnit):boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void scheduleNextJob() {
        this.lock.lock();
        try {
            this.currentlyExecutedJob = this.tasks.poll();
            if (this.currentlyExecutedJob != null) {
                final Runnable command = this.currentlyExecutedJob;
                this.executor.execute(new Runnable() { // from class: org.eclipse.californium.elements.util.SerialExecutor.1
                    /* JADX WARN: Finally extract failed */
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            try {
                                SerialExecutor.this.setOwner();
                                ExecutionListener current = (ExecutionListener) SerialExecutor.this.listener.get();
                                if (current != null) {
                                    try {
                                        try {
                                            current.beforeExecution();
                                        } catch (Throwable th) {
                                            if (current != null) {
                                                try {
                                                    current.afterExecution();
                                                } catch (Throwable t) {
                                                    SerialExecutor.LOGGER.error("unexpected error occurred:", t);
                                                    SerialExecutor.this.clearOwner();
                                                    throw th;
                                                }
                                            }
                                            SerialExecutor.this.clearOwner();
                                            throw th;
                                        }
                                    } catch (Throwable t2) {
                                        SerialExecutor.LOGGER.error("unexpected error occurred:", t2);
                                        if (current == null) {
                                            SerialExecutor.this.clearOwner();
                                        } else {
                                            try {
                                                current.afterExecution();
                                            } catch (Throwable t3) {
                                                SerialExecutor.LOGGER.error("unexpected error occurred:", t3);
                                                SerialExecutor.this.clearOwner();
                                                SerialExecutor.this.scheduleNextJob();
                                            }
                                            SerialExecutor.this.clearOwner();
                                        }
                                    }
                                }
                                command.run();
                                if (current == null) {
                                    SerialExecutor.this.clearOwner();
                                } else {
                                    try {
                                        current.afterExecution();
                                    } catch (Throwable t4) {
                                        SerialExecutor.LOGGER.error("unexpected error occurred:", t4);
                                    }
                                    SerialExecutor.this.clearOwner();
                                }
                                SerialExecutor.this.scheduleNextJob();
                            } catch (Throwable th2) {
                                SerialExecutor.this.scheduleNextJob();
                                throw th2;
                            }
                        } catch (RejectedExecutionException ex) {
                            SerialExecutor.LOGGER.debug("shutdown?", ex);
                        }
                    }
                });
            } else if (this.shutdown) {
                this.terminated.signalAll();
            }
        } finally {
            this.lock.unlock();
        }
    }

    public ExecutionListener setExecutionListener(ExecutionListener listener) {
        return this.listener.getAndSet(listener);
    }
}
