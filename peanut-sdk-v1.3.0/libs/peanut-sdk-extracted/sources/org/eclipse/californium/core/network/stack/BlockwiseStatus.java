package org.eclipse.californium.core.network.stack;

import java.nio.ByteBuffer;
import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.EndpointContextUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/BlockwiseStatus.class */
public abstract class BlockwiseStatus {
    protected final Message firstMessage;
    private final RemoveHandler removeHandler;
    private final KeyUri keyUri;
    private final ByteBuffer buf;
    private final int contentFormat;
    private final int maxTcpBertBulkBlocks;
    private Exchange exchange;
    private EndpointContext followUpEndpointContext;
    private int currentNum;
    private int currentSzx;
    private boolean complete;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/BlockwiseStatus$RemoveHandler.class */
    public interface RemoveHandler {
        void remove(BlockwiseStatus blockwiseStatus);
    }

    protected BlockwiseStatus(KeyUri keyUri, RemoveHandler removeHandler, Exchange exchange, Message first, int maxSize, int maxTcpBertBulkBlocks) {
        if (keyUri == null) {
            throw new NullPointerException("Key URI must not be null!");
        }
        if (removeHandler == null) {
            throw new NullPointerException("Remove handler must not be null!");
        }
        if (first == null) {
            throw new NullPointerException("First message must not be null!");
        }
        if (maxSize == 0) {
            throw new IllegalArgumentException("max. size must not be 0!");
        }
        this.keyUri = keyUri;
        this.removeHandler = removeHandler;
        this.firstMessage = first;
        this.firstMessage.setProtectFromOffload();
        this.exchange = exchange;
        this.contentFormat = first.getOptions().getContentFormat();
        this.buf = ByteBuffer.allocate(maxSize);
        this.maxTcpBertBulkBlocks = maxTcpBertBulkBlocks;
        if (maxTcpBertBulkBlocks > 1) {
            this.currentSzx = 7;
        }
    }

    public KeyUri getKeyUri() {
        return this.keyUri;
    }

    public synchronized boolean isStarting() {
        return this.currentNum == 0;
    }

    protected synchronized Exchange getExchange(boolean reset) {
        Exchange result = this.exchange;
        if (reset) {
            this.exchange = null;
            this.followUpEndpointContext = null;
        }
        return result;
    }

    protected final int getCurrentOffset() {
        return this.currentNum * BlockOption.szx2Size(this.currentSzx);
    }

    protected final int getCurrentNum() {
        return this.currentNum;
    }

    protected final void setCurrentNum(int currentNum) {
        this.currentNum = currentNum;
    }

    protected final int getCurrentSzx() {
        return this.currentSzx;
    }

    protected final int getCurrentSize() {
        return BlockOption.szx2Size(this.currentSzx);
    }

    protected final int getCurrentPayloadSize() {
        int size = getCurrentSize();
        if (this.currentSzx == 7) {
            size *= this.maxTcpBertBulkBlocks;
        }
        return size;
    }

    protected final void setCurrentSzx(int currentSzx) {
        this.currentSzx = currentSzx;
    }

    public final boolean hasContentFormat(int format) {
        return this.contentFormat == format;
    }

    public final synchronized boolean isComplete() {
        return this.complete;
    }

    protected final void setComplete(boolean complete) {
        this.complete = complete;
    }

    public final synchronized boolean complete() {
        boolean complete = !this.complete;
        if (complete) {
            this.complete = true;
        }
        return complete;
    }

    public synchronized void restart() {
        this.buf.position(0);
    }

    protected int getCurrentPosition() {
        return this.buf.position();
    }

    protected final void flipBlocksBuffer() {
        this.buf.flip();
    }

    protected final byte[] getBlock(int position, int length) {
        this.buf.position(position);
        int len = Math.min(length, this.buf.remaining());
        byte[] payload = new byte[len];
        this.buf.get(payload, 0, len);
        return payload;
    }

    protected final void addBlock(byte[] block) throws BlockwiseTransferException {
        if (block != null && block.length > 0) {
            if (this.buf.remaining() < block.length) {
                String msg = String.format("response %d exceeds the left buffer %d", Integer.valueOf(block.length), Integer.valueOf(this.buf.remaining()));
                throw new BlockwiseTransferException(msg, CoAP.ResponseCode.REQUEST_ENTITY_TOO_LARGE);
            }
            this.buf.put(block);
        }
    }

    public final synchronized int getBufferSize() {
        return this.buf.capacity();
    }

    private final byte[] getBody() {
        this.buf.flip();
        byte[] body = new byte[this.buf.remaining()];
        this.buf.get(body).clear();
        return body;
    }

    public synchronized EndpointContext getFollowUpEndpointContext(EndpointContext blockContext) {
        if (this.followUpEndpointContext == null || !this.followUpEndpointContext.getPeerAddress().equals(blockContext.getPeerAddress())) {
            if (this.exchange != null) {
                Request request = this.exchange.getRequest();
                EndpointContext messageContext = request.getDestinationContext();
                this.followUpEndpointContext = EndpointContextUtil.getFollowUpEndpointContext(messageContext, blockContext);
            } else {
                this.followUpEndpointContext = blockContext;
            }
        }
        return this.followUpEndpointContext;
    }

    public synchronized String toString() {
        return String.format("[%s: currentNum=%d, currentSzx=%d, bufferSize=%d, complete=%b]", this.keyUri, Integer.valueOf(this.currentNum), Integer.valueOf(this.currentSzx), Integer.valueOf(getBufferSize()), Boolean.valueOf(this.complete));
    }

    public final synchronized void assembleReceivedMessage(Message message) {
        if (message == null) {
            throw new NullPointerException("message must not be null");
        }
        if (this.firstMessage == null) {
            throw new IllegalStateException("first message is not set");
        }
        if (this.firstMessage.getSourceContext() == null) {
            throw new IllegalStateException("first message has no peer context");
        }
        message.setSourceContext(this.firstMessage.getSourceContext());
        message.setLocalAddress(this.firstMessage.getLocalAddress());
        message.setType(this.firstMessage.getType());
        message.setMID(this.firstMessage.getMID());
        message.setToken(this.firstMessage.getToken());
        message.setOptions(this.firstMessage.getOptions());
        message.getOptions().removeBlock1();
        message.getOptions().removeBlock2();
        if (this.buf.position() > 0) {
            if (!message.isIntendedPayload()) {
                message.setUnintendedPayload();
            }
            message.setPayload(getBody());
        }
    }

    protected void prepareOutgoingMessage(final Message initialMessage, final Message message, boolean first) {
        if (message == null) {
            throw new NullPointerException("message must not be null!");
        }
        if (initialMessage == null) {
            throw new NullPointerException("initial message must not be null!");
        }
        if (initialMessage.getDestinationContext() == null) {
            throw new IllegalArgumentException("initial message has no destinationcontext!");
        }
        message.setDestinationContext(initialMessage.getDestinationContext());
        message.setType(initialMessage.getType());
        message.setOptions(initialMessage.getOptions());
        message.setMaxResourceBodySize(initialMessage.getMaxResourceBodySize());
        message.addMessageObservers(initialMessage.getMessageObservers());
        if (initialMessage.isUnintendedPayload()) {
            message.setUnintendedPayload();
        }
        if (first && (initialMessage.getToken() == null || !initialMessage.hasMID())) {
            message.addMessageObserver(0, new MessageObserverAdapter() { // from class: org.eclipse.californium.core.network.stack.BlockwiseStatus.1
                @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
                public void onReadyToSend() {
                    if (initialMessage.getToken() == null) {
                        initialMessage.setToken(message.getToken());
                    }
                    if (!initialMessage.hasMID()) {
                        initialMessage.setMID(message.getMID());
                    }
                }
            });
        }
        message.addMessageObserver(new MessageObserverAdapter() { // from class: org.eclipse.californium.core.network.stack.BlockwiseStatus.2
            @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
            public void onCancel() {
                BlockwiseStatus.this.removeHandler.remove(BlockwiseStatus.this);
            }

            @Override // org.eclipse.californium.core.coap.MessageObserverAdapter
            protected void failed() {
                BlockwiseStatus.this.removeHandler.remove(BlockwiseStatus.this);
            }
        });
    }

    public void timeoutCurrentTranfer() {
        final Exchange exchange = getExchange(true);
        if (exchange != null && !exchange.isComplete()) {
            exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.stack.BlockwiseStatus.3
                @Override // java.lang.Runnable
                public void run() {
                    exchange.setTimedOut(exchange.getCurrentRequest());
                }
            });
        }
    }
}
