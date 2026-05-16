package org.eclipse.californium.core.observe;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.ClockUtil;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/ObserveRelation.class */
public class ObserveRelation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObserveRelation.class);
    private final long checkIntervalTime;
    private final int checkIntervalCount;
    private final ObservingEndpoint endpoint;
    private final Resource resource;
    private final Exchange exchange;
    private Response recentControlNotification;
    private Response nextControlNotification;
    private final String key;
    private volatile boolean established;
    private volatile boolean canceled;
    private long interestCheckTimer = ClockUtil.nanoRealtime();
    private int interestCheckCounter = 1;

    public ObserveRelation(ObservingEndpoint endpoint, Resource resource, Exchange exchange) {
        if (endpoint == null) {
            throw new NullPointerException();
        }
        if (resource == null) {
            throw new NullPointerException();
        }
        if (exchange == null) {
            throw new NullPointerException();
        }
        this.endpoint = endpoint;
        this.resource = resource;
        this.exchange = exchange;
        Configuration config = exchange.getEndpoint().getConfig();
        this.checkIntervalTime = config.get(CoapConfig.NOTIFICATION_CHECK_INTERVAL_TIME, TimeUnit.NANOSECONDS).longValue();
        this.checkIntervalCount = ((Integer) config.get(CoapConfig.NOTIFICATION_CHECK_INTERVAL_COUNT)).intValue();
        this.key = StringUtil.toString(getSource()) + SslContextUtil.PARAMETER_SEPARATOR + exchange.getRequest().getTokenString();
        LOGGER.debug("Observe-relation, checks every {}ns or {} notifications.", Long.valueOf(this.checkIntervalTime), Integer.valueOf(this.checkIntervalCount));
    }

    public boolean isEstablished() {
        return this.established;
    }

    public void setEstablished() {
        boolean fail;
        synchronized (this) {
            fail = this.canceled;
            if (!fail) {
                this.established = true;
            }
        }
        if (fail) {
            throw new IllegalStateException(String.format("Could not establish observe relation %s with %s, already canceled (%s)!", getKey(), this.resource.getURI(), this.exchange));
        }
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public void cleanup() {
        if (isEstablished()) {
            cancel();
        }
    }

    public void cancel() {
        cancel(true);
    }

    public void cancelAll() {
        this.endpoint.cancelAll();
    }

    public void notifyObservers() {
        this.resource.handleRequest(this.exchange);
    }

    public Resource getResource() {
        return this.resource;
    }

    public Exchange getExchange() {
        return this.exchange;
    }

    public InetSocketAddress getSource() {
        return this.endpoint.getAddress();
    }

    public boolean check() {
        boolean check;
        boolean check2;
        long now = ClockUtil.nanoRealtime();
        synchronized (this) {
            int i = this.interestCheckCounter + 1;
            this.interestCheckCounter = i;
            check = i >= this.checkIntervalCount;
            if (check) {
                this.interestCheckTimer = now;
                this.interestCheckCounter = 0;
            }
        }
        if (check) {
            LOGGER.trace("Observe-relation check, {} notifications reached.", Integer.valueOf(this.checkIntervalCount));
            return check;
        }
        synchronized (this) {
            check2 = (now - this.interestCheckTimer) - this.checkIntervalTime > 0;
            if (check2) {
                this.interestCheckTimer = now;
                this.interestCheckCounter = 0;
            }
        }
        if (check2) {
            LOGGER.trace("Observe-relation check, {}s interval reached.", Long.valueOf(TimeUnit.NANOSECONDS.toSeconds(this.checkIntervalTime)));
        }
        return check2;
    }

    public boolean isPostponedNotification(Response response) {
        if (isInTransit(this.recentControlNotification)) {
            LOGGER.trace("in transit {}", this.recentControlNotification);
            if (this.nextControlNotification != null) {
                if (!this.nextControlNotification.isNotification()) {
                    return true;
                }
                this.nextControlNotification.onTransferComplete();
            }
            this.nextControlNotification = response;
            return true;
        }
        this.recentControlNotification = response;
        this.nextControlNotification = null;
        send(response);
        return false;
    }

    public Response getNextNotification(Response response, boolean acknowledged) {
        Response next = null;
        if (this.recentControlNotification == response) {
            next = this.nextControlNotification;
            if (next != null) {
                this.recentControlNotification = next;
                this.nextControlNotification = null;
                send(next);
            } else if (acknowledged) {
                this.recentControlNotification = null;
                this.nextControlNotification = null;
            }
        }
        return next;
    }

    public void send(Response response) {
        if (!response.isNotification()) {
            cancel(false);
        }
    }

    public String getKey() {
        return this.key;
    }

    private void cancel(boolean complete) {
        boolean fail = false;
        boolean cancel = false;
        synchronized (this) {
            if (!this.canceled) {
                fail = !this.established;
                if (!fail) {
                    this.canceled = true;
                    this.established = false;
                    cancel = true;
                }
            }
        }
        if (fail) {
            throw new IllegalStateException(String.format("Observe relation %s with %s not established (%s)!", getKey(), this.resource.getURI(), this.exchange));
        }
        if (cancel) {
            LOGGER.debug("Canceling observe relation {} with {} ({})", new Object[]{getKey(), this.resource.getURI(), this.exchange});
            this.resource.removeObserveRelation(this);
            this.endpoint.removeObserveRelation(this);
            if (complete) {
                this.exchange.executeComplete();
            }
        }
    }

    private static boolean isInTransit(Response response) {
        return (response == null || !response.isConfirmable() || response.isAcknowledged() || response.isTimedOut() || response.isRejected()) ? false : true;
    }
}
