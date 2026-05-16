package org.eclipse.californium.elements.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.californium.elements.util.SimpleCounterStatistic;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/CounterStatisticManager.class */
public abstract class CounterStatisticManager {
    protected final SimpleCounterStatistic.AlignGroup align;
    private final Map<String, SimpleCounterStatistic> statistics;
    protected final String tag;
    private final ScheduledExecutorService executor;
    private final long interval;
    private final TimeUnit unit;
    private ScheduledFuture<?> taskHandle;
    private AtomicBoolean running;

    public abstract boolean isEnabled();

    public abstract void dump();

    protected CounterStatisticManager(String tag) {
        this.align = new SimpleCounterStatistic.AlignGroup();
        this.statistics = new HashMap();
        this.running = new AtomicBoolean();
        this.tag = StringUtil.normalizeLoggingTag(tag);
        this.interval = 0L;
        this.unit = null;
        this.executor = null;
    }

    protected CounterStatisticManager(String tag, long interval, TimeUnit unit, ScheduledExecutorService executor) {
        this.align = new SimpleCounterStatistic.AlignGroup();
        this.statistics = new HashMap();
        this.running = new AtomicBoolean();
        if (executor == null) {
            throw new NullPointerException("executor must not be null!");
        }
        this.tag = StringUtil.normalizeLoggingTag(tag);
        if (isEnabled()) {
            this.interval = interval;
            this.unit = unit;
            this.executor = interval > 0 ? executor : null;
        } else {
            this.interval = 0L;
            this.unit = null;
            this.executor = null;
        }
    }

    protected void add(String head, SimpleCounterStatistic statistic) {
        this.statistics.put(head + statistic.getName(), statistic);
    }

    protected void add(SimpleCounterStatistic statistic) {
        this.statistics.put(statistic.getName(), statistic);
    }

    protected void addByKey(String key, SimpleCounterStatistic statistic) {
        this.statistics.put(key, statistic);
    }

    protected SimpleCounterStatistic get(String name) {
        return this.statistics.get(name);
    }

    public synchronized void start() {
        if (this.executor != null && this.taskHandle == null) {
            this.running.set(true);
            this.taskHandle = this.executor.scheduleAtFixedRate(new Runnable() { // from class: org.eclipse.californium.elements.util.CounterStatisticManager.1
                @Override // java.lang.Runnable
                public void run() {
                    if (CounterStatisticManager.this.running.get()) {
                        CounterStatisticManager.this.dump();
                    }
                }
            }, this.interval, this.interval, this.unit);
        }
    }

    public synchronized boolean stop() {
        if (this.taskHandle != null) {
            this.running.set(false);
            this.taskHandle.cancel(false);
            this.taskHandle = null;
            return true;
        }
        return false;
    }

    public void reset() {
        for (SimpleCounterStatistic statistic : this.statistics.values()) {
            statistic.reset();
        }
    }

    public long getCounter(String name) {
        return get(name).getCounter();
    }

    public String getTag() {
        return this.tag;
    }
}
