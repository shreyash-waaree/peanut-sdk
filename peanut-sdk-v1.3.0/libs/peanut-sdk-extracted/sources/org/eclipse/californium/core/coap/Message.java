package org.eclipse.californium.core.coap;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.stack.ReliabilityLayerParameters;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.ClockUtil;
import org.eclipse.californium.elements.util.NetworkInterfacesUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/Message.class */
public abstract class Message {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Message.class);
    public static final int NONE = -1;
    public static final int MAX_MID = 65535;
    private CoAP.Type type;
    private OptionSet options;
    private boolean unintendedPayload;
    private int maxResourceBodySize;
    private volatile ReliabilityLayerParameters parameters;
    private volatile EndpointContext destinationContext;
    private volatile EndpointContext effectiveDestinationContext;
    private volatile EndpointContext sourceContext;
    private InetSocketAddress localAddress;
    private volatile boolean sent;
    private volatile boolean rejected;
    private volatile boolean canceled;
    private volatile boolean timedOut;
    private volatile boolean duplicate;
    private volatile boolean transferComplete;
    private volatile Throwable sendError;
    private volatile byte[] bytes;
    private volatile OffloadMode offload;
    private volatile boolean protectFromOffload;
    private volatile long nanoTimestamp;
    private volatile int mid = -1;
    private volatile Token token = null;
    private byte[] payload = Bytes.EMPTY;
    private final AtomicBoolean acknowledged = new AtomicBoolean();
    private final AtomicReference<List<MessageObserver>> messageObservers = new AtomicReference<>();
    private volatile List<MessageObserver> unmodifiableMessageObserversFacade = null;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/coap/Message$OffloadMode.class */
    public enum OffloadMode {
        PAYLOAD,
        FULL
    }

    public abstract int getRawCode();

    public abstract void assertPayloadMatchsBlocksize();

    public abstract boolean hasBlock(BlockOption blockOption);

    protected Message() {
    }

    protected String toTracingString(String code) {
        OffloadMode offload;
        OptionSet options;
        String status = getStatusTracingString();
        String payload = getPayloadTracingString();
        synchronized (this.acknowledged) {
            offload = this.offload;
            options = this.options;
        }
        if (offload == OffloadMode.FULL) {
            return String.format("%s-%-6s MID=%5d, Token=%s %s(offloaded!)", getType(), code, Integer.valueOf(getMID()), getTokenString(), status);
        }
        if (offload == OffloadMode.PAYLOAD) {
            return String.format("%s-%-6s MID=%5d, Token=%s, OptionSet=%s, %s(offloaded!)", getType(), code, Integer.valueOf(getMID()), getTokenString(), options, status);
        }
        return String.format("%s-%-6s MID=%5d, Token=%s, OptionSet=%s, %s%s", getType(), code, Integer.valueOf(getMID()), getTokenString(), options, status, payload);
    }

    public Message(CoAP.Type type) {
        this.type = type;
    }

    public CoAP.Type getType() {
        return this.type;
    }

    public Message setType(CoAP.Type type) {
        this.type = type;
        return this;
    }

    public boolean isConfirmable() {
        return getType() == CoAP.Type.CON;
    }

    public Message setConfirmable(boolean con) {
        setType(con ? CoAP.Type.CON : CoAP.Type.NON);
        return this;
    }

    public boolean isIntendedPayload() {
        return true;
    }

    public void setUnintendedPayload() {
        if (isIntendedPayload()) {
            throw new IllegalStateException("Message is already intended to have payload!");
        }
        this.unintendedPayload = true;
    }

    public boolean isUnintendedPayload() {
        return this.unintendedPayload;
    }

    public void setReliabilityLayerParameters(ReliabilityLayerParameters parameter) {
        this.parameters = parameter;
    }

    public ReliabilityLayerParameters getReliabilityLayerParameters() {
        return this.parameters;
    }

    public int getMID() {
        return this.mid;
    }

    public boolean hasMID() {
        return this.mid != -1;
    }

    public Message setMID(int mid) {
        if (mid > 65535 || mid < -1) {
            throw new IllegalArgumentException("The MID must be an unsigned 16-bit number but was " + mid);
        }
        if (this.bytes != null) {
            throw new IllegalStateException("already serialized!");
        }
        this.mid = mid;
        return this;
    }

    public void removeMID() {
        setMID(-1);
    }

    public boolean hasEmptyToken() {
        return this.token == null || this.token.isEmpty();
    }

    public Token getToken() {
        return this.token;
    }

    public byte[] getTokenBytes() {
        if (this.token == null) {
            return null;
        }
        return this.token.getBytes();
    }

    public String getTokenString() {
        return this.token == null ? "null" : this.token.getAsString();
    }

    public Message setToken(byte[] tokenBytes) {
        Token token = null;
        if (tokenBytes != null) {
            token = new Token(tokenBytes);
        }
        return setToken(token);
    }

    public Message setToken(Token token) {
        this.token = token;
        if (this.bytes != null) {
            throw new IllegalStateException("already serialized!");
        }
        return this;
    }

    public OptionSet getOptions() {
        OptionSet optionSet;
        synchronized (this.acknowledged) {
            if (this.offload == OffloadMode.FULL) {
                throw new IllegalStateException("message " + this.offload + " offloaded! " + this);
            }
            if (this.options == null) {
                this.options = new OptionSet();
            }
            optionSet = this.options;
        }
        return optionSet;
    }

    public Message setOptions(OptionSet options) {
        this.options = new OptionSet(options);
        return this;
    }

    public int getMaxResourceBodySize() {
        return this.maxResourceBodySize;
    }

    public void setMaxResourceBodySize(int maxResourceBodySize) {
        this.maxResourceBodySize = maxResourceBodySize;
    }

    public int getPayloadSize() {
        return this.payload.length;
    }

    public byte[] getPayload() {
        if (this.offload != null) {
            throw new IllegalStateException("message " + this.offload + " offloaded!");
        }
        return this.payload;
    }

    public String getPayloadString() {
        if (this.offload != null) {
            throw new IllegalStateException("message " + this.offload + " offloaded!");
        }
        if (this.payload.length == 0) {
            return "";
        }
        return new String(this.payload, CoAP.UTF8_CHARSET);
    }

    protected String getPayloadTracingString() {
        return StringUtil.toDisplayString(this.payload, 32);
    }

    public Message setPayload(String payload) {
        if (payload == null || payload.isEmpty()) {
            this.payload = Bytes.EMPTY;
        } else {
            setPayload(payload.getBytes(CoAP.UTF8_CHARSET));
        }
        return this;
    }

    public Message setPayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            this.payload = Bytes.EMPTY;
        } else {
            if (!isIntendedPayload() && !isUnintendedPayload()) {
                throw new IllegalArgumentException("Message must not have payload!");
            }
            this.payload = payload;
        }
        return this;
    }

    public EndpointContext getDestinationContext() {
        return this.destinationContext;
    }

    public EndpointContext getEffectiveDestinationContext() {
        return this.effectiveDestinationContext;
    }

    public EndpointContext getSourceContext() {
        return this.sourceContext;
    }

    public Message setDestinationContext(EndpointContext peerContext) {
        if (peerContext != null) {
            InetAddress address = peerContext.getPeerAddress().getAddress();
            if (NetworkInterfacesUtil.isBroadcastAddress(address)) {
                throw new IllegalArgumentException("Broadcast destination " + StringUtil.toString(address) + " only supported for request!");
            }
            if (NetworkInterfacesUtil.isMultiAddress(address)) {
                throw new IllegalArgumentException("Multicast destination " + StringUtil.toString(address) + " only supported for request!");
            }
        }
        this.destinationContext = peerContext;
        this.effectiveDestinationContext = peerContext;
        return this;
    }

    public void setEffectiveDestinationContext(EndpointContext peerContext) {
        this.effectiveDestinationContext = peerContext;
    }

    protected void setRequestDestinationContext(EndpointContext peerContext) {
        this.destinationContext = peerContext;
        this.effectiveDestinationContext = peerContext;
    }

    public Message setSourceContext(EndpointContext peerContext) {
        this.sourceContext = peerContext;
        return this;
    }

    public void setLocalAddress(InetSocketAddress local) {
        this.localAddress = local;
    }

    public InetSocketAddress getLocalAddress() {
        return this.localAddress;
    }

    public boolean isAcknowledged() {
        return this.acknowledged.get();
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged.set(acknowledged);
    }

    public boolean acknowledge() {
        if (isConfirmable() && this.acknowledged.compareAndSet(false, true)) {
            for (MessageObserver handler : getMessageObservers()) {
                handler.onAcknowledgement();
            }
            return true;
        }
        return false;
    }

    public boolean isRejected() {
        return this.rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
        if (rejected) {
            for (MessageObserver handler : getMessageObservers()) {
                handler.onReject();
            }
        }
    }

    public boolean isTimedOut() {
        return this.timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
        if (timedOut) {
            for (MessageObserver observer : getMessageObservers()) {
                observer.onTimeout();
            }
        }
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
        if (canceled) {
            for (MessageObserver handler : getMessageObservers()) {
                handler.onCancel();
            }
        }
    }

    public void setReadyToSend() {
        for (MessageObserver handler : getMessageObservers()) {
            handler.onReadyToSend();
        }
    }

    public void onConnecting() {
        for (MessageObserver handler : getMessageObservers()) {
            handler.onConnecting();
        }
    }

    public void onDtlsRetransmission(int flight) {
        for (MessageObserver handler : getMessageObservers()) {
            handler.onDtlsRetransmission(flight);
        }
    }

    public boolean isSent() {
        return this.sent;
    }

    public void setSent(boolean sent) {
        boolean retransmission = this.sent;
        this.sent = sent;
        if (sent) {
            for (MessageObserver handler : getMessageObservers()) {
                handler.onSent(retransmission);
            }
        }
    }

    public Throwable getSendError() {
        return this.sendError;
    }

    public void setSendError(Throwable sendError) {
        this.sendError = sendError;
        if (sendError != null) {
            for (MessageObserver handler : getMessageObservers()) {
                handler.onSendError(sendError);
            }
        }
    }

    public void onContextEstablished(EndpointContext endpointContext) {
        if (endpointContext != null) {
            for (MessageObserver handler : getMessageObservers()) {
                handler.onContextEstablished(endpointContext);
            }
        }
    }

    public void onTransferComplete() {
        if (!this.transferComplete) {
            this.transferComplete = true;
            LOGGER.trace("Message transfer completed {}", this);
            for (MessageObserver handler : getMessageObservers()) {
                handler.onTransferComplete();
            }
        }
    }

    public boolean waitForSent(long timeout) throws InterruptedException {
        boolean z;
        long expiresNano = ClockUtil.nanoRealtime() + TimeUnit.MILLISECONDS.toNanos(timeout);
        long leftTimeout = timeout;
        synchronized (this) {
            while (!this.sent && !isCanceled() && !isTimedOut() && getSendError() == null) {
                wait(leftTimeout);
                if (timeout > 0) {
                    long leftNanos = expiresNano - ClockUtil.nanoRealtime();
                    if (leftNanos <= 0) {
                        break;
                    }
                    leftTimeout = TimeUnit.NANOSECONDS.toMillis(leftNanos) + 1;
                }
            }
            z = this.sent;
        }
        return z;
    }

    public boolean isDuplicate() {
        return this.duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    protected String getStatusTracingString() {
        if (this.canceled) {
            return "canceled ";
        }
        if (this.sendError != null) {
            return this.sendError.getMessage() + " ";
        }
        if (this.rejected) {
            return "rejected ";
        }
        if (this.acknowledged.get()) {
            return "acked ";
        }
        if (this.timedOut) {
            return "timeout ";
        }
        return "";
    }

    public byte[] getBytes() {
        if (this.offload == OffloadMode.FULL) {
            throw new IllegalStateException("message offloaded!");
        }
        return this.bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    protected boolean hasBlock(BlockOption block, BlockOption messageOffset) {
        int offset = block.getOffset();
        if (messageOffset != null) {
            offset -= messageOffset.getOffset();
        }
        return 0 <= offset && offset <= getPayloadSize();
    }

    public long getNanoTimestamp() {
        return this.nanoTimestamp;
    }

    public void setNanoTimestamp(long timestamp) {
        this.nanoTimestamp = timestamp;
    }

    public void cancel() {
        setCanceled(true);
    }

    public void retransmitting() {
        for (MessageObserver observer : getMessageObservers()) {
            try {
                observer.onRetransmission();
            } catch (Exception e) {
                LOGGER.error("Faulty MessageObserver for retransmitting events", e);
            }
        }
    }

    public void offload(OffloadMode mode) {
        if (!this.protectFromOffload) {
            synchronized (this.acknowledged) {
                this.offload = mode;
                if (mode != null) {
                    this.payload = Bytes.EMPTY;
                    if (mode == OffloadMode.FULL) {
                        this.bytes = null;
                        if (this.options != null) {
                            this.options.clear();
                            this.options = null;
                        }
                    }
                }
            }
        }
    }

    public OffloadMode getOffloadMode() {
        return this.offload;
    }

    public void setProtectFromOffload() {
        this.protectFromOffload = true;
    }

    public List<MessageObserver> getMessageObservers() {
        if (null == this.unmodifiableMessageObserversFacade) {
            return Collections.emptyList();
        }
        return this.unmodifiableMessageObserversFacade;
    }

    public void addMessageObserver(MessageObserver observer) {
        if (observer == null) {
            throw new NullPointerException();
        }
        ensureMessageObserverList().add(observer);
    }

    public void addMessageObserver(int index, MessageObserver observer) {
        if (observer == null) {
            throw new NullPointerException();
        }
        ensureMessageObserverList().add(index, observer);
    }

    public void addMessageObservers(List<MessageObserver> observers) {
        if (observers == null) {
            throw new NullPointerException();
        }
        if (!observers.isEmpty()) {
            ensureMessageObserverList().addAll(observers);
        }
    }

    public void removeMessageObserver(MessageObserver observer) {
        if (observer == null) {
            throw new NullPointerException();
        }
        List<MessageObserver> list = this.messageObservers.get();
        if (list != null) {
            list.remove(observer);
        }
    }

    private List<MessageObserver> ensureMessageObserverList() {
        List<MessageObserver> list = this.messageObservers.get();
        if (null == list) {
            boolean created = this.messageObservers.compareAndSet(null, new CopyOnWriteArrayList());
            list = this.messageObservers.get();
            if (created) {
                this.unmodifiableMessageObserversFacade = Collections.unmodifiableList(list);
            }
        }
        return list;
    }
}
