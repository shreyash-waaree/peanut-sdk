package org.eclipse.californium.elements.util;

import java.util.concurrent.TimeUnit;
import org.eclipse.californium.elements.util.Statistic;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/TimeStatistic.class */
public class TimeStatistic {
    private static final Statistic.Scale NANOS_TO_MILLIS = new Statistic.Scale() { // from class: org.eclipse.californium.elements.util.TimeStatistic.1
        @Override // org.eclipse.californium.elements.util.Statistic.Scale
        public long scale(long value) {
            return TimeUnit.NANOSECONDS.toMillis(value);
        }
    };
    private final Statistic statistic;

    public TimeStatistic(long timeRange, long timeSlot, TimeUnit unit) {
        long range = unit.toNanos(timeRange);
        long slot = unit.toNanos(timeSlot);
        this.statistic = new Statistic(range, slot);
    }

    public void add(long time, TimeUnit unit) {
        if (time >= 0) {
            long nanos = unit.toNanos(time);
            this.statistic.add(nanos);
        }
    }

    public boolean available() {
        return this.statistic.available();
    }

    public String getSummaryAsText() {
        return getSummary(950, 990, 999).toString(" ms");
    }

    public Statistic.Summary getSummary(int... percentiles) {
        return new Statistic.Summary(this.statistic.getSummary(percentiles), NANOS_TO_MILLIS);
    }
}
