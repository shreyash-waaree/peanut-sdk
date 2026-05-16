package org.eclipse.californium.core.network.interceptors;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.util.CounterStatisticManager;
import org.eclipse.californium.elements.util.SimpleCounterStatistic;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/interceptors/HealthStatisticLogger.class */
public class HealthStatisticLogger extends CounterStatisticManager implements MessageInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthStatisticLogger.class);
    private final SimpleCounterStatistic sentRequests;
    private final SimpleCounterStatistic sentResponses;
    private final SimpleCounterStatistic sentRejects;
    private final SimpleCounterStatistic sentAcknowledges;
    private final SimpleCounterStatistic resentRequests;
    private final SimpleCounterStatistic resentResponses;
    private final SimpleCounterStatistic sendErrors;
    private final SimpleCounterStatistic receivedRequests;
    private final SimpleCounterStatistic receivedResponses;
    private final SimpleCounterStatistic receivedRejects;
    private final SimpleCounterStatistic receivedAcknowledges;
    private final SimpleCounterStatistic duplicateRequests;
    private final SimpleCounterStatistic duplicateResponses;
    private final SimpleCounterStatistic ignoredMessages;
    private final SimpleCounterStatistic offloadedMessages;
    private final boolean udp;

    public HealthStatisticLogger(String tag, boolean udp) {
        super(tag);
        this.sentRequests = new SimpleCounterStatistic("requests", this.align);
        this.sentResponses = new SimpleCounterStatistic("responses", this.align);
        this.sentRejects = new SimpleCounterStatistic("rejects", this.align);
        this.sentAcknowledges = new SimpleCounterStatistic("acks", this.align);
        this.resentRequests = new SimpleCounterStatistic("request retransmissions", this.align);
        this.resentResponses = new SimpleCounterStatistic("response retransmissions", this.align);
        this.sendErrors = new SimpleCounterStatistic("errors", this.align);
        this.receivedRequests = new SimpleCounterStatistic("requests", this.align);
        this.receivedResponses = new SimpleCounterStatistic("responses", this.align);
        this.receivedRejects = new SimpleCounterStatistic("rejects", this.align);
        this.receivedAcknowledges = new SimpleCounterStatistic("acks", this.align);
        this.duplicateRequests = new SimpleCounterStatistic("duplicate requests", this.align);
        this.duplicateResponses = new SimpleCounterStatistic("duplicate responses", this.align);
        this.ignoredMessages = new SimpleCounterStatistic("ignored", this.align);
        this.offloadedMessages = new SimpleCounterStatistic("offloaded", this.align);
        this.udp = udp;
        init();
    }

    public HealthStatisticLogger(String tag, boolean udp, long interval, TimeUnit unit, ScheduledExecutorService executor) {
        super(tag, interval, unit, executor);
        this.sentRequests = new SimpleCounterStatistic("requests", this.align);
        this.sentResponses = new SimpleCounterStatistic("responses", this.align);
        this.sentRejects = new SimpleCounterStatistic("rejects", this.align);
        this.sentAcknowledges = new SimpleCounterStatistic("acks", this.align);
        this.resentRequests = new SimpleCounterStatistic("request retransmissions", this.align);
        this.resentResponses = new SimpleCounterStatistic("response retransmissions", this.align);
        this.sendErrors = new SimpleCounterStatistic("errors", this.align);
        this.receivedRequests = new SimpleCounterStatistic("requests", this.align);
        this.receivedResponses = new SimpleCounterStatistic("responses", this.align);
        this.receivedRejects = new SimpleCounterStatistic("rejects", this.align);
        this.receivedAcknowledges = new SimpleCounterStatistic("acks", this.align);
        this.duplicateRequests = new SimpleCounterStatistic("duplicate requests", this.align);
        this.duplicateResponses = new SimpleCounterStatistic("duplicate responses", this.align);
        this.ignoredMessages = new SimpleCounterStatistic("ignored", this.align);
        this.offloadedMessages = new SimpleCounterStatistic("offloaded", this.align);
        this.udp = udp;
        init();
    }

    private void init() {
        add("send-", this.sentRequests);
        add("send-", this.sentResponses);
        add("send-", this.sentAcknowledges);
        add("send-", this.sentRejects);
        add("send-", this.resentRequests);
        add("send-", this.resentResponses);
        add("send-", this.sendErrors);
        add("recv-", this.receivedRequests);
        add("recv-", this.receivedResponses);
        add("recv-", this.receivedAcknowledges);
        add("recv-", this.receivedRejects);
        add("recv-", this.duplicateRequests);
        add("recv-", this.duplicateResponses);
        add("recv-", this.ignoredMessages);
    }

    @Override // org.eclipse.californium.elements.util.CounterStatisticManager
    public boolean isEnabled() {
        return LOGGER.isDebugEnabled();
    }

    @Override // org.eclipse.californium.elements.util.CounterStatisticManager
    public void dump() {
        try {
            if (this.receivedRequests.isUsed() || this.sentRequests.isUsed() || this.sendErrors.isUsed()) {
                String eol = StringUtil.lineSeparator();
                String head = "   " + this.tag;
                StringBuilder log = new StringBuilder();
                log.append(this.tag).append("endpoint statistic:").append(eol);
                log.append(this.tag).append("send statistic:").append(eol);
                log.append(head).append(this.sentRequests).append(eol);
                log.append(head).append(this.sentResponses).append(eol);
                if (this.udp) {
                    log.append(head).append(this.sentAcknowledges).append(eol);
                    log.append(head).append(this.sentRejects).append(eol);
                    log.append(head).append(this.resentRequests).append(eol);
                    log.append(head).append(this.resentResponses).append(eol);
                }
                log.append(head).append(this.sendErrors).append(eol);
                log.append(this.tag).append("receive statistic:").append(eol);
                log.append(head).append(this.receivedRequests).append(eol);
                log.append(head).append(this.receivedResponses).append(eol);
                if (this.udp) {
                    log.append(head).append(this.receivedAcknowledges).append(eol);
                    log.append(head).append(this.receivedRejects).append(eol);
                    log.append(head).append(this.duplicateRequests).append(eol);
                    log.append(head).append(this.duplicateResponses).append(eol);
                    log.append(head).append(this.offloadedMessages).append(eol);
                }
                log.append(head).append(this.ignoredMessages).append(eol);
                long sent = getSentCounters();
                long processed = getProcessedCounters();
                log.append(this.tag).append("sent ").append(sent).append(", received ").append(processed);
                LOGGER.debug("{}", log);
            }
        } catch (Throwable e) {
            LOGGER.error("{}", this.tag, e);
        }
    }

    public long getSentCounters() {
        long sent = this.sentRequests.getCounter() + this.sentResponses.getCounter() + this.sentAcknowledges.getCounter() + this.sentRejects.getCounter() + this.resentRequests.getCounter() + this.resentResponses.getCounter();
        return sent;
    }

    public long getProcessedCounters() {
        long processed = this.receivedRequests.getCounter() + this.receivedResponses.getCounter() + this.receivedAcknowledges.getCounter() + this.receivedRejects.getCounter() + this.duplicateRequests.getCounter() + this.duplicateResponses.getCounter() + this.ignoredMessages.getCounter();
        return processed;
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void sendRequest(Request request) {
        if (request.getSendError() != null) {
            this.sendErrors.increment();
        } else if (request.isDuplicate()) {
            this.resentRequests.increment();
        } else {
            this.sentRequests.increment();
        }
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void sendResponse(Response response) {
        if (response.getOffloadMode() != null) {
            this.offloadedMessages.increment();
        }
        if (response.getSendError() != null) {
            this.sendErrors.increment();
        } else if (response.isDuplicate()) {
            this.resentResponses.increment();
        } else {
            this.sentResponses.increment();
        }
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void sendEmptyMessage(EmptyMessage message) {
        if (message.getSendError() != null) {
            this.sendErrors.increment();
        } else if (message.getType() == CoAP.Type.ACK) {
            this.sentAcknowledges.increment();
        } else {
            this.sentRejects.increment();
        }
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveRequest(Request request) {
        if (request.isDuplicate()) {
            this.duplicateRequests.increment();
        } else {
            this.receivedRequests.increment();
        }
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveResponse(Response response) {
        if (response.isCanceled()) {
            this.ignoredMessages.increment();
        } else if (response.isDuplicate()) {
            this.duplicateResponses.increment();
        } else {
            this.receivedResponses.increment();
        }
    }

    @Override // org.eclipse.californium.core.network.interceptors.MessageInterceptor
    public void receiveEmptyMessage(EmptyMessage message) {
        if (message.isCanceled()) {
            this.ignoredMessages.increment();
        } else if (message.getType() == CoAP.Type.ACK) {
            this.receivedAcknowledges.increment();
        } else {
            this.receivedRejects.increment();
        }
    }
}
