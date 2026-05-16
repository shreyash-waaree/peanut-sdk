package org.eclipse.californium.elements.util;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/FilteredLogger.class */
public class FilteredLogger {
    private static final boolean ENABLE;
    private final Logger logger;
    private final long nanosPerPeriod;
    private final long maxPerPeriod;
    private long counter;
    private long startNanos = ClockUtil.nanoRealtime();

    static {
        ENABLE = !Boolean.FALSE.equals(StringUtil.getConfigurationBoolean("COAP_LOGGING_FILTER"));
    }

    public FilteredLogger(Logger logger, long maxPerPeriod, long nanosPerPeriod) {
        this.logger = logger;
        this.maxPerPeriod = maxPerPeriod;
        this.nanosPerPeriod = nanosPerPeriod;
    }

    public void warn(String fmt, Object... args) {
        if (this.logger.isWarnEnabled()) {
            log(Level.WARN, fmt, args);
        }
    }

    public void info(String fmt, Object... args) {
        if (this.logger.isInfoEnabled()) {
            log(Level.INFO, fmt, args);
        }
    }

    public void debug(String fmt, Object... args) {
        if (this.logger.isDebugEnabled()) {
            log(Level.DEBUG, fmt, args);
        }
    }

    public void trace(String fmt, Object... args) {
        if (this.logger.isTraceEnabled()) {
            log(Level.TRACE, fmt, args);
        }
    }

    private void log(Level level, String fmt, Object... args) {
        boolean info;
        if (ENABLE) {
            long now = ClockUtil.nanoRealtime();
            long time = (this.nanosPerPeriod + this.startNanos) - now;
            synchronized (this) {
                info = this.counter < this.maxPerPeriod;
                if (time > 0) {
                    this.counter++;
                } else {
                    this.startNanos = now;
                    if (!info) {
                        int length = args.length;
                        args = Arrays.copyOf(args, length + 1);
                        args[length] = Long.valueOf(this.counter);
                        fmt = fmt + " ({} additional errors.)";
                        info = true;
                    }
                    this.counter = 0L;
                }
            }
        } else {
            info = true;
        }
        if (info) {
            switch (AnonymousClass1.$SwitchMap$org$slf4j$event$Level[level.ordinal()]) {
                case 1:
                    this.logger.error(fmt, args);
                    return;
                case 2:
                    this.logger.warn(fmt, args);
                    return;
                case 3:
                    this.logger.info(fmt, args);
                    return;
                case 4:
                    this.logger.debug(fmt, args);
                    return;
                case 5:
                    this.logger.trace(fmt, args);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: renamed from: org.eclipse.californium.elements.util.FilteredLogger$1, reason: invalid class name */
    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/util/FilteredLogger$1.class */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$org$slf4j$event$Level = new int[Level.values().length];

        static {
            try {
                $SwitchMap$org$slf4j$event$Level[Level.ERROR.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$slf4j$event$Level[Level.WARN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$org$slf4j$event$Level[Level.INFO.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$org$slf4j$event$Level[Level.DEBUG.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$org$slf4j$event$Level[Level.TRACE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }
}
