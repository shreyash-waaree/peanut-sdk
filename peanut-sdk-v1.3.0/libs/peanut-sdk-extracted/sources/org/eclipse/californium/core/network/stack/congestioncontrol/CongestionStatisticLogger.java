package org.eclipse.californium.core.network.stack.congestioncontrol;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.util.CounterStatisticManager;
import org.eclipse.californium.elements.util.SimpleCounterStatistic;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/congestioncontrol/CongestionStatisticLogger.class */
public class CongestionStatisticLogger extends CounterStatisticManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CongestionStatisticLogger.class);
    private final SimpleCounterStatistic sentRequests;
    private final SimpleCounterStatistic queueRequests;
    private final SimpleCounterStatistic dequeueRequests;
    private final SimpleCounterStatistic receivedResponses;

    public CongestionStatisticLogger(String tag, int interval, TimeUnit unit, ScheduledExecutorService executor) {
        super(tag, interval, unit, executor);
        this.sentRequests = new SimpleCounterStatistic("sent-requests", this.align);
        this.queueRequests = new SimpleCounterStatistic("queue-requests", this.align);
        this.dequeueRequests = new SimpleCounterStatistic("dequeue-requests", this.align);
        this.receivedResponses = new SimpleCounterStatistic("recv-responses", this.align);
        init();
    }

    private void init() {
        add(this.sentRequests);
        add(this.queueRequests);
        add(this.receivedResponses);
    }

    @Override // org.eclipse.californium.elements.util.CounterStatisticManager
    public boolean isEnabled() {
        return LOGGER.isDebugEnabled();
    }

    @Override // org.eclipse.californium.elements.util.CounterStatisticManager
    public void dump() {
        try {
            if (this.receivedResponses.isUsed() || this.sentRequests.isUsed() || this.queueRequests.isUsed()) {
                String eol = StringUtil.lineSeparator();
                String head = "   " + this.tag;
                StringBuilder log = new StringBuilder();
                log.append(this.tag).append("congestion statistic:").append(eol);
                log.append(head).append(this.sentRequests).append(eol);
                log.append(head).append(this.queueRequests).append(eol);
                log.append(head).append(this.dequeueRequests).append(eol);
                log.append(head).append(this.receivedResponses).append(eol);
                LOGGER.debug("{}", log);
            }
        } catch (Throwable e) {
            LOGGER.error("{}", this.tag, e);
        }
    }

    public void sendRequest() {
        this.sentRequests.increment();
    }

    public void queueRequest() {
        this.queueRequests.increment();
    }

    public void dequeueRequest() {
        this.dequeueRequests.increment();
    }

    public void receiveResponse(Response response) {
        if (!response.isDuplicate()) {
            this.receivedResponses.increment();
        }
    }
}
