package org.eclipse.californium.elements.util;

import com.keenon.sdk.http.ws.WsStatus;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/Statistic.class */
public class Statistic {
    private final long slotWidth;
    private final AtomicLong[] statistic;
    private final AtomicLong sum = new AtomicLong();
    private final AtomicBoolean invalidSum = new AtomicBoolean();
    private final AtomicLong maximum = new AtomicLong();

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/Statistic$Scale.class */
    public interface Scale {
        long scale(long j);
    }

    public Statistic(long range, long slot) {
        int size = ((int) (range / slot)) + 1;
        this.statistic = new AtomicLong[size];
        for (int index = 0; index < size; index++) {
            this.statistic[index] = new AtomicLong();
        }
        this.slotWidth = slot;
    }

    public void add(long value) {
        if (value >= 0) {
            int index = (int) (value / this.slotWidth);
            if (index < this.statistic.length) {
                this.statistic[index].incrementAndGet();
            } else {
                this.statistic[this.statistic.length - 1].incrementAndGet();
            }
            if (!this.invalidSum.get() && this.sum.addAndGet(value) < 0) {
                this.invalidSum.set(true);
            }
            long j = this.maximum.get();
            while (true) {
                long maximumValue = j;
                if (value > maximumValue && !this.maximum.compareAndSet(maximumValue, value)) {
                    j = this.maximum.get();
                } else {
                    return;
                }
            }
        }
    }

    private long getUpperLimit(int index) {
        if (this.slotWidth > 1) {
            return (((long) (index + 1)) * this.slotWidth) - 1;
        }
        return index;
    }

    public boolean available() {
        for (int index = 0; index < this.statistic.length; index++) {
            if (this.statistic[index].get() > 0) {
                return true;
            }
        }
        return false;
    }

    public String getSummaryAsText() {
        return getSummary(950, 990, 999).toString();
    }

    public Summary getSummary(int... percentiles) {
        long count = 0;
        for (int index = 0; index < this.statistic.length; index++) {
            long hits = this.statistic[index].get();
            if (hits > 0) {
                count += hits;
            }
        }
        if (count > 0) {
            long max = this.maximum.get();
            long[] values = null;
            if (percentiles != null) {
                values = new long[percentiles.length];
                if (percentiles.length > 0) {
                    Arrays.sort(percentiles);
                    int linesIndex = percentiles.length - 1;
                    if (percentiles[linesIndex] < 0 || percentiles[linesIndex] > 999) {
                        throw new IllegalArgumentException("line " + percentiles[linesIndex] + " is not in [0...999]%%");
                    }
                    long line = (count * ((long) (WsStatus.CODE.NORMAL_CLOSE - percentiles[linesIndex]))) / 1000;
                    long downCount = 0;
                    for (int index2 = this.statistic.length - 1; index2 >= 0; index2--) {
                        long hits2 = this.statistic[index2].get();
                        if (hits2 > 0) {
                            long next = downCount + hits2;
                            while (downCount <= line && next > line) {
                                long value = getUpperLimit(index2);
                                if (value > max) {
                                    value = max;
                                }
                                values[linesIndex] = value;
                                linesIndex--;
                                if (linesIndex < 0) {
                                    break;
                                }
                                if (percentiles[linesIndex] < 0 || percentiles[linesIndex] > 999) {
                                    throw new IllegalArgumentException("line " + percentiles[linesIndex] + " is not in [0...999]%%");
                                }
                                line = (count * ((long) (WsStatus.CODE.NORMAL_CLOSE - percentiles[linesIndex]))) / 1000;
                            }
                            if (linesIndex < 0) {
                                break;
                            }
                            downCount = next;
                        }
                    }
                }
            }
            return new Summary((int) count, this.invalidSum.get() ? null : Long.valueOf(this.sum.get()), max, percentiles, values);
        }
        return new Summary();
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/Statistic$Summary.class */
    public static class Summary {
        private final int count;
        private final Long overallSum;
        private final long maximum;
        private final int[] percentiles;
        private final long[] percentileValues;

        public Summary() {
            this.count = 0;
            this.overallSum = 0L;
            this.maximum = 0L;
            this.percentiles = null;
            this.percentileValues = null;
        }

        public Summary(int count, Long overallSum, long maximum, int[] percentiles, long[] values) {
            if (percentiles != null) {
                if (values == null) {
                    throw new NullPointerException("values must not be null, if percentiles are provided!");
                }
                if (percentiles.length != values.length) {
                    throw new IllegalArgumentException("Number of values must match percentiles! " + percentiles.length + " != " + values.length);
                }
            }
            this.count = count;
            this.overallSum = overallSum;
            this.maximum = maximum;
            this.percentiles = percentiles;
            this.percentileValues = values;
        }

        public Summary(Summary raw, Scale scale) {
            this.count = raw.count;
            if (raw.overallSum != null) {
                this.overallSum = Long.valueOf(scale.scale(raw.overallSum.longValue()));
            } else {
                this.overallSum = null;
            }
            this.maximum = scale.scale(raw.maximum);
            this.percentiles = raw.percentiles;
            if (raw.percentileValues != null) {
                int numOfValues = raw.percentileValues.length;
                this.percentileValues = new long[numOfValues];
                for (int index = 0; index < numOfValues; index++) {
                    this.percentileValues[index] = scale.scale(raw.percentileValues[index]);
                }
                return;
            }
            this.percentileValues = null;
        }

        public int getCount() {
            return this.count;
        }

        public double getAverage() {
            if (this.overallSum == null) {
                return -1.0d;
            }
            if (this.count == 0) {
                return 0.0d;
            }
            return this.overallSum.longValue() / ((double) this.count);
        }

        public Long getOverallSum() {
            return this.overallSum;
        }

        public long getMaximum() {
            return this.maximum;
        }

        public int getPercentileCount() {
            if (this.percentiles != null) {
                return this.percentiles.length;
            }
            return 0;
        }

        public long getPercentilePerMill(int index) {
            if (this.percentiles != null) {
                return this.percentiles[index];
            }
            return -1L;
        }

        public long getPercentileValue(int index) {
            if (this.percentileValues != null) {
                return this.percentileValues[index];
            }
            return -1L;
        }

        public String toString() {
            return toString("");
        }

        public String toString(String unit) {
            if (this.count > 0) {
                StringBuilder summary = new StringBuilder();
                summary.append(String.format("#: %d", Integer.valueOf(this.count)));
                if (this.overallSum != null) {
                    double average = getAverage();
                    if (average < 1.0d) {
                        summary.append(String.format(Locale.UK, ", sum.: %d%s", this.overallSum, unit));
                    } else {
                        summary.append(String.format(Locale.UK, ", avg.: %.2f%s", Double.valueOf(average), unit));
                    }
                }
                if (this.percentiles != null) {
                    for (int index = 0; index < this.percentiles.length; index++) {
                        int p = this.percentiles[index] / 10;
                        int pm = this.percentiles[index] % 10;
                        if (pm > 0) {
                            summary.append(String.format(", %d.%d%%: %d%s", Integer.valueOf(p), Integer.valueOf(pm), Long.valueOf(this.percentileValues[index]), unit));
                        } else {
                            summary.append(String.format(", %d%%: %d%s", Integer.valueOf(p), Long.valueOf(this.percentileValues[index]), unit));
                        }
                    }
                }
                summary.append(String.format(", max.: %d%s", Long.valueOf(this.maximum), unit));
                return summary.toString();
            }
            return "no values available!";
        }

        public Summary scale(Scale scale) {
            return new Summary(this, scale);
        }
    }
}
