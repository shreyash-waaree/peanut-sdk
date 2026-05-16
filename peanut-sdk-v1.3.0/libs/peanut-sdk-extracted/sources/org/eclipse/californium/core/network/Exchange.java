package org.eclipse.californium.core.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.NoResponseOption;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.util.ClockUtil;
import org.eclipse.californium.elements.util.SerialExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/Exchange.class */
public class Exchange {
    private static final Logger LOGGER;
    static final boolean DEBUG;
    private static final int MAX_OBSERVE_NO = 16777215;
    private static final AtomicInteger INSTANCE_COUNTER;
    private final int id;
    private final SerialExecutor executor;
    private final long nanoTimestamp;
    private final boolean keepRequestInStore;
    private final boolean notification;
    private final Object peersIdentity;
    private final Origin origin;
    private Throwable caller;
    private volatile Endpoint endpoint;
    private volatile RemoveHandler removeHandler;
    private final AtomicBoolean complete;
    private KeyMID currentKeyMID;
    private KeyToken originalKeyToken;
    private KeyToken currentKeyToken;
    private volatile long sendNanoTimestamp;
    private boolean transmissionRttStart;
    private boolean transmissionRttSet;
    private long transmissionRttTimestamp;
    private volatile Request request;
    private volatile Request currentRequest;
    private volatile Response response;
    private volatile Response currentResponse;
    private volatile boolean timedOut;
    private float timeoutScale;
    private int currentTimeout;
    private volatile int failedTransmissionCount;
    private volatile ScheduledFuture<?> retransmissionHandle;
    private volatile BlockOption block1ToAck;
    private volatile Integer notificationNumber;
    private volatile ObserveRelation relation;
    private volatile List<KeyMID> notifications;
    private final AtomicReference<EndpointContext> endpointContext;
    private volatile EndpointContextOperator endpointContextPreOperator;
    private byte[] cryptoContextId;
    static final /* synthetic */ boolean $assertionsDisabled;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/Exchange$EndpointContextOperator.class */
    public interface EndpointContextOperator {
        EndpointContext apply(EndpointContext endpointContext);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/Exchange$Origin.class */
    public enum Origin {
        LOCAL,
        REMOTE
    }

    static {
        $assertionsDisabled = !Exchange.class.desiredAssertionStatus();
        LOGGER = LoggerFactory.getLogger(Exchange.class);
        DEBUG = LOGGER.isTraceEnabled();
        INSTANCE_COUNTER = new AtomicInteger();
    }

    public Exchange(Request request, Object peersIdentity, Origin origin, Executor executor) {
        this(request, peersIdentity, origin, executor, null, false);
    }

    public Exchange(Request request, Object peersIdentity, Origin origin, Executor executor, EndpointContext ctx, boolean notification) {
        this.complete = new AtomicBoolean();
        this.failedTransmissionCount = 0;
        this.endpointContext = new AtomicReference<>();
        if (request == null) {
            throw new NullPointerException("request must not be null!");
        }
        if (executor == null) {
            throw new NullPointerException("executor must not be null");
        }
        this.id = INSTANCE_COUNTER.incrementAndGet();
        this.executor = new SerialExecutor(executor);
        this.currentRequest = request;
        this.request = request;
        this.origin = origin;
        this.peersIdentity = peersIdentity;
        this.endpointContext.set(ctx);
        this.keepRequestInStore = !notification && request.isObserve() && origin == Origin.LOCAL;
        this.notification = notification;
        this.nanoTimestamp = ClockUtil.nanoRealtime();
    }

    public String toString() {
        char originMarker = this.origin == Origin.LOCAL ? 'L' : 'R';
        if (this.complete.get()) {
            return "Exchange[" + originMarker + this.id + ", complete]";
        }
        return "Exchange[" + originMarker + this.id + "]";
    }

    public void sendAccept() {
        if (!$assertionsDisabled && this.origin != Origin.REMOTE) {
            throw new AssertionError();
        }
        sendAccept(this.currentRequest.getSourceContext());
    }

    public void sendAccept(EndpointContext context) {
        if (!$assertionsDisabled && this.origin != Origin.REMOTE) {
            throw new AssertionError();
        }
        Request current = this.currentRequest;
        if (current.getType() == CoAP.Type.CON && current.hasMID() && current.acknowledge()) {
            EmptyMessage ack = EmptyMessage.newACK(current, context);
            this.endpoint.sendEmptyMessage(this, ack);
        }
    }

    public void sendReject() {
        if (!$assertionsDisabled && this.origin != Origin.REMOTE) {
            throw new AssertionError();
        }
        sendReject(this.currentRequest.getSourceContext());
    }

    public void sendReject(EndpointContext context) {
        if (!$assertionsDisabled && this.origin != Origin.REMOTE) {
            throw new AssertionError();
        }
        Request current = this.currentRequest;
        if (current.hasMID() && !current.isRejected()) {
            current.setRejected(true);
            if (!current.isMulticast()) {
                EmptyMessage rst = EmptyMessage.newRST(current, context);
                this.endpoint.sendEmptyMessage(this, rst);
            }
        }
    }

    public void sendResponse(Response response) {
        Request current = this.currentRequest;
        if (current.getOptions().hasNoResponse()) {
            NoResponseOption noResponse = current.getOptions().getNoResponse();
            if (noResponse.suppress(response.getCode()) && !current.acknowledge()) {
                return;
            }
        } else if (current.isMulticast() && response.isError()) {
            return;
        }
        if (response.getDestinationContext() == null) {
            response.setDestinationContext(this.currentRequest.getSourceContext());
        }
        this.endpoint.sendResponse(this, response);
    }

    public Origin getOrigin() {
        return this.origin;
    }

    public boolean isOfLocalOrigin() {
        return this.origin == Origin.LOCAL;
    }

    public boolean keepsRequestInStore() {
        return this.keepRequestInStore;
    }

    public boolean isNotification() {
        return this.notification;
    }

    public Request getRequest() {
        return this.request;
    }

    public void setRequest(Request newRequest) {
        Token token;
        assertOwner();
        if (this.request != newRequest) {
            if (this.keepRequestInStore && (token = this.request.getToken()) != null && !token.equals(newRequest.getToken())) {
                throw new IllegalArgumentException(this + " token missmatch (" + token + "!=" + newRequest.getToken() + ")!");
            }
            this.request = newRequest;
        }
    }

    public Request getCurrentRequest() {
        return this.currentRequest;
    }

    public void setCurrentRequest(Request newCurrentRequest) {
        assertOwner();
        if (this.currentRequest != newCurrentRequest) {
            setRetransmissionHandle(null);
            this.failedTransmissionCount = 0;
            LOGGER.debug("{} replace {} by {}", new Object[]{this, this.currentRequest, newCurrentRequest});
            this.currentRequest = newCurrentRequest;
        }
    }

    public Response getResponse() {
        return this.response;
    }

    public void setResponse(Response response) {
        assertOwner();
        this.response = response;
    }

    public Response getCurrentResponse() {
        return this.currentResponse;
    }

    public void setCurrentResponse(Response newCurrentResponse) {
        assertOwner();
        if (this.currentResponse != newCurrentResponse) {
            if (!isOfLocalOrigin() && this.currentKeyMID != null && this.currentResponse != null && this.currentResponse.getType() == CoAP.Type.NON && this.currentResponse.isNotification()) {
                LOGGER.info("{} store NON notification: {}", this, this.currentKeyMID);
                this.notifications.add(this.currentKeyMID);
                this.currentKeyMID = null;
            }
            this.currentResponse = newCurrentResponse;
        }
    }

    public KeyMID getKeyMID() {
        return this.currentKeyMID;
    }

    public void setKeyMID(KeyMID keyMID) {
        assertOwner();
        if (!keyMID.equals(this.currentKeyMID)) {
            RemoveHandler handler = this.removeHandler;
            if (handler != null && this.currentKeyMID != null) {
                handler.remove(this, null, this.currentKeyMID);
            }
            this.currentKeyMID = keyMID;
        }
    }

    public void setKeyToken(KeyToken keyToken) {
        assertOwner();
        if (!isOfLocalOrigin()) {
            throw new IllegalStateException("Token is only supported for local exchanges!");
        }
        if (!keyToken.equals(this.currentKeyToken)) {
            RemoveHandler handler = this.removeHandler;
            if (handler != null && this.currentKeyToken != null && !this.currentKeyToken.equals(this.originalKeyToken)) {
                handler.remove(this, this.currentKeyToken, null);
            }
            this.currentKeyToken = keyToken;
            if (this.keepRequestInStore && this.originalKeyToken == null) {
                this.originalKeyToken = keyToken;
            }
        }
    }

    public KeyToken getKeyToken() {
        return this.currentKeyToken;
    }

    public BlockOption getBlock1ToAck() {
        return this.block1ToAck;
    }

    public void setBlock1ToAck(BlockOption block1ToAck) {
        this.block1ToAck = block1ToAck;
    }

    public Endpoint getEndpoint() {
        return this.endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Object getPeersIdentity() {
        return this.peersIdentity;
    }

    public boolean isTimedOut() {
        return this.timedOut;
    }

    public void setTimedOut(Message message) {
        assertOwner();
        LOGGER.debug("{} timed out {}!", this, message);
        if (!isComplete()) {
            setComplete();
            this.timedOut = true;
            message.setTimedOut(true);
            if (this.request != null && this.request != message && this.currentRequest == message) {
                this.request.setTimedOut(true);
            }
        }
    }

    public int getFailedTransmissionCount() {
        return this.failedTransmissionCount;
    }

    public int incrementFailedTransmissionCount() {
        assertOwner();
        int i = this.failedTransmissionCount + 1;
        this.failedTransmissionCount = i;
        return i;
    }

    public float getTimeoutScale() {
        return this.timeoutScale;
    }

    public void setTimeoutScale(float scale) {
        if (scale < 1.0f) {
            throw new IllegalArgumentException("Timeout scale factor must be at least 1.0, not " + scale);
        }
        this.timeoutScale = scale;
    }

    public int getCurrentTimeout() {
        return this.currentTimeout;
    }

    public void setCurrentTimeout(int currentTimeout) {
        if (currentTimeout <= 1) {
            throw new IllegalArgumentException("Timeout  must be larger than 1 ms, not " + currentTimeout);
        }
        this.currentTimeout = currentTimeout;
    }

    public boolean isTransmissionPending() {
        return this.retransmissionHandle != null;
    }

    public void setRetransmissionHandle(ScheduledFuture<?> newRetransmissionHandle) {
        assertOwner();
        if (!this.complete.get() || newRetransmissionHandle == null) {
            ScheduledFuture<?> previous = this.retransmissionHandle;
            this.retransmissionHandle = newRetransmissionHandle;
            if (previous != null) {
                previous.cancel(false);
            }
        }
    }

    public void retransmitResponse() {
        assertOwner();
        if (this.origin == Origin.REMOTE) {
            this.caller = null;
            this.complete.set(false);
            return;
        }
        throw new IllegalStateException(this + " retransmit on local exchange not allowed!");
    }

    public void setNotificationNumber(int notificationNo) {
        if (notificationNo < 0 || notificationNo > MAX_OBSERVE_NO) {
            throw new IllegalArgumentException(this + " illegal observe number");
        }
        this.notificationNumber = Integer.valueOf(notificationNo);
    }

    public Integer getNotificationNumber() {
        return this.notificationNumber;
    }

    public void setRemoveHandler(RemoveHandler removeHandler) {
        this.removeHandler = removeHandler;
    }

    public boolean hasRemoveHandler() {
        return this.removeHandler != null;
    }

    public boolean isComplete() {
        return this.complete.get();
    }

    public Throwable getCaller() {
        return this.caller;
    }

    public boolean setComplete() {
        assertOwner();
        if (this.complete.compareAndSet(false, true)) {
            if (DEBUG) {
                this.caller = new Throwable(toString());
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("{}!", this, this.caller);
                } else {
                    LOGGER.debug("{}!", this);
                }
            } else {
                LOGGER.debug("{}!", this);
            }
            setRetransmissionHandle(null);
            RemoveHandler handler = this.removeHandler;
            if (handler != null) {
                if (this.origin == Origin.LOCAL) {
                    if (this.currentKeyToken != null || this.currentKeyMID != null) {
                        handler.remove(this, this.currentKeyToken, this.currentKeyMID);
                    }
                    if (this.currentKeyToken != this.originalKeyToken) {
                        handler.remove(this, this.originalKeyToken, null);
                    }
                    if (LOGGER.isDebugEnabled()) {
                        Request currrentRequest = getCurrentRequest();
                        Request request = getRequest();
                        if (request == currrentRequest) {
                            LOGGER.debug("local {} completed {}!", this, request);
                            return true;
                        }
                        LOGGER.debug("local {} completed {} -/- {}!", new Object[]{this, request, currrentRequest});
                        return true;
                    }
                    return true;
                }
                Response currentResponse = getCurrentResponse();
                if (currentResponse == null) {
                    LOGGER.debug("remote {} rejected (without response)!", this);
                    return true;
                }
                if (this.currentKeyMID != null) {
                    handler.remove(this, null, this.currentKeyMID);
                }
                removeNotifications();
                Response response = getResponse();
                if (response == currentResponse || response == null) {
                    LOGGER.debug("Remote {} completed {}!", this, currentResponse);
                    return true;
                }
                LOGGER.debug("Remote {} completed {} -/- {}!", new Object[]{this, response, currentResponse});
                return true;
            }
            return true;
        }
        throw new ExchangeCompleteException(this + " already complete!", this.caller);
    }

    public boolean executeComplete() {
        if (this.complete.get()) {
            return false;
        }
        if (checkOwner()) {
            setComplete();
            return true;
        }
        execute(new Runnable() { // from class: org.eclipse.californium.core.network.Exchange.1
            @Override // java.lang.Runnable
            public void run() {
                if (!Exchange.this.complete.get()) {
                    Exchange.this.setComplete();
                }
            }
        });
        return true;
    }

    public long getNanoTimestamp() {
        return this.nanoTimestamp;
    }

    public long getSendNanoTimestamp() {
        return this.sendNanoTimestamp;
    }

    public void setSendNanoTimestamp(long nanoTimestamp) {
        this.sendNanoTimestamp = nanoTimestamp;
    }

    public void startTransmissionRtt() {
        this.transmissionRttStart = true;
        this.transmissionRttSet = false;
        this.transmissionRttTimestamp = ClockUtil.nanoRealtime();
    }

    public long calculateTransmissionRtt() {
        if (!this.transmissionRttSet && !this.transmissionRttStart) {
            throw new IllegalStateException("startTransmissionRtt must be called before!");
        }
        if (!this.transmissionRttSet) {
            this.transmissionRttSet = true;
            this.transmissionRttStart = false;
            this.transmissionRttTimestamp = ClockUtil.nanoRealtime() - this.transmissionRttTimestamp;
            if (this.transmissionRttTimestamp == 0) {
                this.transmissionRttTimestamp = 1L;
            }
        }
        return this.transmissionRttTimestamp;
    }

    public long calculateApplicationRtt() {
        return ClockUtil.nanoRealtime() - this.nanoTimestamp;
    }

    public ObserveRelation getRelation() {
        return this.relation;
    }

    public void setRelation(ObserveRelation relation) {
        assertOwner();
        if (relation == null) {
            throw new NullPointerException("Observer relation must not be null!");
        }
        if (this.relation != null || this.notifications != null) {
            throw new IllegalStateException("Observer relation already set!");
        }
        this.relation = relation;
        this.notifications = new ArrayList();
    }

    public void removeNotifications() {
        assertOwner();
        RemoveHandler handler = this.removeHandler;
        if (this.notifications != null && !this.notifications.isEmpty()) {
            for (KeyMID keyMid : this.notifications) {
                LOGGER.info("{} removing NON notification: {}", this, keyMid);
                if (handler != null) {
                    handler.remove(this, null, keyMid);
                }
            }
            this.notifications.clear();
            LOGGER.debug("{} removing all remaining NON-notifications of observe relation with {}", this, this.relation.getSource());
        }
    }

    public void setEndpointContext(EndpointContext ctx) {
        EndpointContextOperator operator = this.endpointContextPreOperator;
        if (operator != null) {
            ctx = operator.apply(ctx);
        }
        if (this.endpointContext.compareAndSet(null, ctx)) {
            getCurrentRequest().onContextEstablished(ctx);
        } else {
            this.endpointContext.set(ctx);
        }
    }

    public void resetEndpointContext() {
        this.endpointContext.set(null);
    }

    public EndpointContext getEndpointContext() {
        return this.endpointContext.get();
    }

    public void setEndpointContextPreOperator(EndpointContextOperator operator) {
        this.endpointContextPreOperator = operator;
    }

    public void execute(Runnable command) {
        try {
            if (checkOwner()) {
                command.run();
            } else {
                this.executor.execute(command);
            }
        } catch (RejectedExecutionException e) {
            LOGGER.debug("{} execute:", this, e);
        } catch (Throwable t) {
            LOGGER.error("{} execute:", this, t);
        }
    }

    public void assertIncomplete(Object message) {
        assertOwner();
        if (this.complete.get()) {
            throw new ExchangeCompleteException(this + " is already complete! " + message, this.caller);
        }
    }

    private void assertOwner() {
        this.executor.assertOwner();
    }

    public boolean checkOwner() {
        return this.executor.checkOwner();
    }

    public void setCryptographicContextID(byte[] cryptoContextId) {
        this.cryptoContextId = cryptoContextId;
    }

    public byte[] getCryptographicContextID() {
        return this.cryptoContextId;
    }
}
