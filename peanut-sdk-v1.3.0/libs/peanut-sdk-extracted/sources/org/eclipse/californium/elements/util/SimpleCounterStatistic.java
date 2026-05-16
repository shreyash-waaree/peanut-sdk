package org.eclipse.californium.elements.util;

import java.util.concurrent.atomic.AtomicLong;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SimpleCounterStatistic.class */
public class SimpleCounterStatistic {
    private final String name;
    private final int align;
    private final AlignGroup group;
    private final AtomicLong currentCounter;
    private final AtomicLong overallCounter;

    public SimpleCounterStatistic(String name) {
        this.currentCounter = new AtomicLong();
        this.overallCounter = new AtomicLong();
        this.name = name;
        this.align = 0;
        this.group = null;
    }

    public SimpleCounterStatistic(String name, int align) {
        this.currentCounter = new AtomicLong();
        this.overallCounter = new AtomicLong();
        this.name = name;
        this.align = align;
        this.group = null;
    }

    public SimpleCounterStatistic(String name, AlignGroup group) {
        this.currentCounter = new AtomicLong();
        this.overallCounter = new AtomicLong();
        this.name = name;
        this.align = 0;
        this.group = group.add(this);
    }

    public String dump(int align) {
        long current;
        long overall;
        synchronized (this.overallCounter) {
            current = this.currentCounter.getAndSet(0L);
            overall = this.overallCounter.addAndGet(current);
        }
        return format(align, this.name, current) + String.format(" (%8d overall).", Long.valueOf(overall));
    }

    public String getName() {
        return this.name;
    }

    public long increment() {
        return this.currentCounter.incrementAndGet();
    }

    public long increment(int delta) {
        return this.currentCounter.addAndGet(delta);
    }

    public long getCounter() {
        long j;
        synchronized (this.overallCounter) {
            j = this.overallCounter.get() + this.currentCounter.get();
        }
        return j;
    }

    public long reset() {
        long andSet;
        synchronized (this.overallCounter) {
            long current = this.currentCounter.getAndSet(0L);
            this.overallCounter.addAndGet(current);
            andSet = this.overallCounter.getAndSet(0L);
        }
        return andSet;
    }

    public boolean isUsed() {
        boolean z;
        synchronized (this.overallCounter) {
            z = this.currentCounter.get() > 0 || this.overallCounter.get() > 0;
        }
        return z;
    }

    public String toString() {
        int align = this.group == null ? this.align : this.group.getAlign();
        return dump(align);
    }

    public static String format(int align, String name, long value) {
        return align == 0 ? String.format("%s: %8d", name, Long.valueOf(value)) : String.format("%" + align + "s: %8d", name, Long.valueOf(value));
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/SimpleCounterStatistic$AlignGroup.class */
    public static class AlignGroup {
        int align;

        public AlignGroup add(SimpleCounterStatistic statistic) {
            return add(statistic.getName());
        }

        public AlignGroup add(String name) {
            int align = name.length();
            if (align > this.align) {
                this.align = align;
            }
            return this;
        }

        public int getAlign() {
            return -(this.align + 1);
        }
    }
}
