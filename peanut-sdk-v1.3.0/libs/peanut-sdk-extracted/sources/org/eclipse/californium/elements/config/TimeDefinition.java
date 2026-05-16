package org.eclipse.californium.elements.config;

import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.LinkFormat;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/TimeDefinition.class */
public class TimeDefinition extends DocumentedDefinition<Long> {
    public TimeDefinition(String key, String documentation) {
        super(key, documentation, Long.class, null);
    }

    public TimeDefinition(String key, String documentation, long defaultValue, TimeUnit unit) {
        super(key, documentation, Long.class, Long.valueOf(TimeUnit.NANOSECONDS.convert(defaultValue, unit)));
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String getTypeName() {
        return "Time";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public String writeValue(Long value) {
        TimeUnit unit = TimeUnit.MILLISECONDS;
        if (value.longValue() != 0) {
            unit = TimeUnit.NANOSECONDS;
            if (value.longValue() % 1000 == 0) {
                unit = TimeUnit.MICROSECONDS;
                value = Long.valueOf(value.longValue() / 1000);
                if (value.longValue() % 1000 == 0) {
                    unit = TimeUnit.MILLISECONDS;
                    value = Long.valueOf(value.longValue() / 1000);
                    if (value.longValue() % 1000 == 0) {
                        unit = TimeUnit.SECONDS;
                        value = Long.valueOf(value.longValue() / 1000);
                        if (value.longValue() % 60 == 0) {
                            unit = TimeUnit.MINUTES;
                            value = Long.valueOf(value.longValue() / 60);
                            if (value.longValue() % 60 == 0) {
                                unit = TimeUnit.HOURS;
                                value = Long.valueOf(value.longValue() / 60);
                                if (value.longValue() % 24 == 0) {
                                    unit = TimeUnit.DAYS;
                                    value = Long.valueOf(value.longValue() / 24);
                                }
                            }
                        }
                    }
                }
            }
        }
        return value + "[" + getTimeUnitAsText(unit) + "]";
    }

    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Long checkValue(Long value) throws ValueException {
        if (value != null && value.longValue() < 0) {
            throw new ValueException("Time " + value + " must be not less than 0!");
        }
        return value;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Can't rename method to resolve collision */
    @Override // org.eclipse.californium.elements.config.DocumentedDefinition
    public Long parseValue(String value) throws ValueException {
        TimeUnit unit;
        TimeUnit valueUnit = TimeUnit.MILLISECONDS;
        String num = value;
        int pos = value.indexOf(91);
        if (pos >= 0) {
            int end = value.indexOf(93);
            if (pos < end) {
                num = value.substring(0, pos).trim();
                String textUnit = value.substring(pos + 1, end).trim();
                valueUnit = getTimeUnit(textUnit);
                if (valueUnit == null) {
                    throw new ValueException(textUnit + " unknown unit!");
                }
            } else {
                throw new ValueException(value + " doesn't match value[unit]!");
            }
        } else {
            char last = value.charAt(value.length() - 1);
            if (!Character.isDigit(last) && (unit = getTimeUnit(value)) != null) {
                valueUnit = unit;
                num = value.substring(0, value.length() - getTimeUnitAsText(unit).length()).trim();
            }
        }
        long time = Long.parseLong(num);
        return Long.valueOf(TimeUnit.NANOSECONDS.convert(time, valueUnit));
    }

    /* JADX INFO: renamed from: org.eclipse.californium.elements.config.TimeDefinition$1, reason: invalid class name */
    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/config/TimeDefinition$1.class */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$java$util$concurrent$TimeUnit = new int[TimeUnit.values().length];

        static {
            try {
                $SwitchMap$java$util$concurrent$TimeUnit[TimeUnit.NANOSECONDS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$java$util$concurrent$TimeUnit[TimeUnit.MICROSECONDS.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$java$util$concurrent$TimeUnit[TimeUnit.MILLISECONDS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$java$util$concurrent$TimeUnit[TimeUnit.SECONDS.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$java$util$concurrent$TimeUnit[TimeUnit.MINUTES.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$java$util$concurrent$TimeUnit[TimeUnit.HOURS.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$java$util$concurrent$TimeUnit[TimeUnit.DAYS.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    public static String getTimeUnitAsText(TimeUnit unit) {
        switch (AnonymousClass1.$SwitchMap$java$util$concurrent$TimeUnit[unit.ordinal()]) {
            case 1:
                return "ns";
            case 2:
                return "ys";
            case 3:
                return "ms";
            case 4:
                return "s";
            case 5:
                return "min";
            case 6:
                return "h";
            case 7:
                return LinkFormat.SECTOR;
            default:
                return "";
        }
    }

    public static TimeUnit getTimeUnit(String timeUnitText) {
        String matchUnitText = "";
        TimeUnit matchingUnit = null;
        for (TimeUnit unit : TimeUnit.values()) {
            String text = getTimeUnitAsText(unit);
            if (!text.isEmpty()) {
                if (text.equals(timeUnitText)) {
                    return unit;
                }
                if (timeUnitText.endsWith(text) && text.length() > matchUnitText.length()) {
                    matchingUnit = unit;
                    matchUnitText = text;
                }
            }
        }
        return matchingUnit;
    }
}
