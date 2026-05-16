package org.eclipse.californium.core.network.stack;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.stack.CongestionControlLayer;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/RemoteEndpoint.class */
public abstract class RemoteEndpoint {
    private static final int RTOARRAYSIZE = 3;
    private final InetSocketAddress remoteAddress;
    private final int nstart;
    private final boolean usesBlindEstimator;
    private final Set<Exchange> inFlight;
    private final Queue<Exchange> requestQueue;
    private final Queue<Exchange> responseQueue;
    private final Queue<CongestionControlLayer.PostponedExchange> notifyQueue;
    private boolean processingNotifies;
    private boolean initializedRto;
    private long[] overallRTO = new long[3];
    private int currentOverallIndex;
    private volatile long currentRTO;
    protected long meanOverallRTO;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/RemoteEndpoint$RtoType.class */
    public enum RtoType {
        STRONG,
        WEAK,
        NONE
    }

    public abstract void processRttMeasurement(RtoType rtoType, long j);

    public RemoteEndpoint(InetSocketAddress remoteAddress, int ackTimeout, int nstart, boolean usesBlindEstimator) {
        this.remoteAddress = remoteAddress;
        this.nstart = nstart;
        this.usesBlindEstimator = usesBlindEstimator;
        for (int i = 0; i < 3; i++) {
            this.overallRTO[i] = ackTimeout;
        }
        this.currentRTO = ackTimeout;
        this.meanOverallRTO = ackTimeout;
        this.currentOverallIndex = 0;
        this.inFlight = new HashSet();
        this.requestQueue = new LinkedList();
        this.responseQueue = new LinkedList();
        this.notifyQueue = new LinkedList();
    }

    public InetSocketAddress getRemoteAddress() {
        return this.remoteAddress;
    }

    public Queue<Exchange> getRequestQueue() {
        return this.requestQueue;
    }

    public Queue<Exchange> getResponseQueue() {
        return this.responseQueue;
    }

    public Queue<CongestionControlLayer.PostponedExchange> getNotifyQueue() {
        return this.notifyQueue;
    }

    public void setCurrentRTO(long currentRTO) {
        this.currentRTO = currentRTO;
    }

    public long getCurrentRTO() {
        return this.currentRTO;
    }

    public synchronized boolean startProcessingNotifies() {
        if (this.processingNotifies) {
            return false;
        }
        this.processingNotifies = true;
        return true;
    }

    public synchronized boolean stopProcessingNotifies() {
        if (this.processingNotifies) {
            this.processingNotifies = false;
            return true;
        }
        return false;
    }

    public synchronized boolean initialRto() {
        if (this.initializedRto) {
            return false;
        }
        this.initializedRto = true;
        return true;
    }

    public long getRTO() {
        long rto = this.currentRTO;
        int size = getNumberOfOngoingExchanges();
        if (this.usesBlindEstimator && size > 1 && !this.initializedRto) {
            rto *= (long) size;
        }
        return Math.min(rto, 32000L);
    }

    public synchronized void updateRTO(long newRTO) {
        long[] jArr = this.overallRTO;
        int i = this.currentOverallIndex;
        this.currentOverallIndex = i + 1;
        jArr[i] = newRTO;
        if (this.currentOverallIndex >= this.overallRTO.length) {
            this.currentOverallIndex = 0;
        }
        long meanRTO = 0;
        for (int i2 = 0; i2 < 3; i2++) {
            meanRTO += this.overallRTO[i2];
        }
        this.meanOverallRTO = meanRTO / 3;
        setCurrentRTO(newRTO);
    }

    public synchronized boolean registerExchange(Exchange exchange) {
        if (this.inFlight.contains(exchange)) {
            return true;
        }
        if (this.inFlight.size() < this.nstart) {
            this.inFlight.add(exchange);
            return true;
        }
        return false;
    }

    public synchronized boolean inFlightExchange(Exchange exchange) {
        return this.inFlight.contains(exchange);
    }

    public synchronized boolean removeExchange(Exchange exchange) {
        if (this.inFlight.remove(exchange)) {
            return true;
        }
        return false;
    }

    public synchronized int getNumberOfOngoingExchanges() {
        return this.inFlight.size();
    }

    public void checkAging() {
    }
}
