package org.eclipse.californium.core.network.stack;

import java.util.Arrays;
import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.stack.BlockwiseStatus;
import org.eclipse.californium.core.observe.NotificationOrder;
import org.eclipse.californium.elements.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/Block2BlockwiseStatus.class */
public final class Block2BlockwiseStatus extends BlockwiseStatus {
    private static final Logger LOGGER = LoggerFactory.getLogger(Block2BlockwiseStatus.class);
    private final NotificationOrder order;
    private final byte[] etag;

    private Block2BlockwiseStatus(KeyUri keyUri, BlockwiseStatus.RemoveHandler removeHandler, Exchange exchange, Response response, int maxSize, int maxTcpBertBulkBlocks) {
        super(keyUri, removeHandler, exchange, response, maxSize, maxTcpBertBulkBlocks);
        Integer observeCount = response.getOptions().getObserve();
        if (observeCount != null && OptionSet.isValidObserveOption(observeCount.intValue())) {
            this.order = new NotificationOrder(observeCount.intValue());
            exchange.setNotificationNumber(observeCount.intValue());
        } else {
            this.order = null;
        }
        if (response.getOptions().getETagCount() > 0) {
            this.etag = response.getOptions().getETags().get(0);
        } else {
            this.etag = null;
        }
    }

    public static Block2BlockwiseStatus forOutboundResponse(KeyUri keyUri, BlockwiseStatus.RemoveHandler removeHandler, Exchange exchange, Response response, int maxTcpBertBulkBlocks) {
        int size = response.getPayloadSize();
        Block2BlockwiseStatus status = new Block2BlockwiseStatus(keyUri, removeHandler, exchange, response, size, maxTcpBertBulkBlocks);
        if (size > 0) {
            try {
                status.addBlock(response.getPayload());
                status.flipBlocksBuffer();
            } catch (BlockwiseTransferException ex) {
                LOGGER.warn("buffer overflow on start", ex);
            }
        }
        return status;
    }

    public static Block2BlockwiseStatus forInboundResponse(KeyUri keyUri, BlockwiseStatus.RemoveHandler removeHandler, Exchange exchange, Response block, int maxBodySize, int maxTcpBertBulkBlocks) {
        int bufferSize = maxBodySize;
        if (block.getOptions().hasSize2()) {
            bufferSize = block.getOptions().getSize2().intValue();
        }
        Block2BlockwiseStatus status = new Block2BlockwiseStatus(keyUri, removeHandler, exchange, block, bufferSize, maxTcpBertBulkBlocks);
        return status;
    }

    public final Integer getObserve() {
        if (this.order == null) {
            return null;
        }
        return Integer.valueOf(this.order.getObserve());
    }

    public final boolean isNew(Response response) {
        if (response == null) {
            throw new NullPointerException("response block must not be null");
        }
        if (response.getOptions().hasObserve()) {
            return this.order == null || this.order.isNew(response);
        }
        return false;
    }

    public final boolean matchTransfer(Exchange exchange) {
        Integer notification = exchange.getNotificationNumber();
        return (notification == null || this.order == null) ? notification == null && this.order == null : this.order.getObserve() == notification.intValue();
    }

    public synchronized void addBlock(Response responseBlock) throws BlockwiseTransferException {
        if (responseBlock == null) {
            throw new NullPointerException("response block must not be null");
        }
        if (!responseBlock.getOptions().hasBlock2()) {
            throw new IllegalArgumentException("response block has no block2 option");
        }
        int currentOffset = getCurrentPosition();
        int responseOffset = responseBlock.getOptions().getBlock2().getOffset();
        if (currentOffset != responseOffset) {
            String msg = String.format("response offset %d does not match the expected offset %d!", Integer.valueOf(responseOffset), Integer.valueOf(currentOffset));
            throw new BlockwiseTransferException(msg);
        }
        if (this.etag != null) {
            if (responseBlock.getOptions().getETagCount() != 1) {
                throw new BlockwiseTransferException("response does not contain a single ETag");
            }
            if (!Arrays.equals(this.etag, responseBlock.getOptions().getETags().get(0))) {
                throw new BlockwiseTransferException("response does not contain expected ETag");
            }
        }
        addBlock(responseBlock.getPayload());
        setCurrentNum(getCurrentPosition() / getCurrentSize());
    }

    public synchronized Request getNextRequestBlock(int blockSzx) throws BlockwiseTransferException {
        Exchange exchange = getExchange(false);
        if (exchange == null) {
            throw new BlockwiseTransferException("Block2 exchange already completed!", true);
        }
        setCurrentSzx(blockSzx);
        int size = getCurrentSize();
        int from = getCurrentPosition();
        if (from % size != 0) {
            throw new BlockwiseTransferException("Block2 buffer position " + from + " doesn't align with blocksize " + size + "!");
        }
        int num = from / size;
        setCurrentNum(num);
        Request request = exchange.getRequest();
        Request block = new Request(request.getCode());
        prepareOutgoingMessage(request, block, num == 0);
        block.getOptions().removeObserve();
        block.getOptions().setBlock2(blockSzx, false, num);
        return block;
    }

    public synchronized Response getNextResponseBlock(BlockOption block2) {
        if (block2 == null) {
            throw new NullPointerException("block option must not be null.");
        }
        int from = block2.getOffset();
        int szx = block2.getSzx();
        int size = block2.getSize();
        setCurrentSzx(szx);
        int num = from / size;
        setCurrentNum(num);
        Response block = new Response(((Response) this.firstMessage).getCode());
        int bodySize = getBufferSize();
        prepareOutgoingMessage(this.firstMessage, block, num == 0);
        if (num == 0) {
            if (!block.getOptions().hasSize2()) {
                block.getOptions().setSize2(bodySize);
            }
        } else {
            block.getOptions().removeObserve();
            block.setType(null);
        }
        boolean m = false;
        if (0 < bodySize && from < bodySize) {
            byte[] blockPayload = getBlock(from, getCurrentPayloadSize());
            m = from + blockPayload.length < bodySize;
            block.setPayload(blockPayload);
        }
        block.getOptions().setBlock2(szx, m, num);
        if (!m) {
            setComplete(true);
        }
        return block;
    }

    public final void completeOldTransfer(Exchange newExchange) {
        Exchange oldExchange = getExchange(true);
        if (oldExchange != null) {
            if (newExchange != oldExchange) {
                if (oldExchange.isNotification()) {
                    oldExchange.executeComplete();
                    return;
                } else {
                    oldExchange.getRequest().setCanceled(true);
                    return;
                }
            }
            oldExchange.setCurrentRequest(oldExchange.getRequest());
        }
    }

    public final void completeNewTranfer(Exchange newExchange) {
        Exchange oldExchange = getExchange(false);
        if (newExchange != oldExchange) {
            if (newExchange.isNotification()) {
                newExchange.setComplete();
            } else {
                newExchange.getRequest().setCanceled(true);
            }
        }
    }

    final boolean completeResponse() {
        Response response;
        if (complete()) {
            synchronized (this) {
                response = (Response) this.firstMessage;
            }
            if (response != null) {
                response.onTransferComplete();
                return true;
            }
            return false;
        }
        return false;
    }

    @Override // org.eclipse.californium.core.network.stack.BlockwiseStatus
    public synchronized String toString() {
        String result = super.toString();
        if (this.order != null) {
            StringBuilder builder = new StringBuilder(result);
            builder.setLength(result.length() - 1);
            builder.append(", observe=").append(this.order.getObserve()).append("]");
            result = builder.toString();
        }
        return result;
    }

    public static final void crop(Response responseToCrop, BlockOption requestedBlock, int maxTcpBertBulkBlocks) {
        if (responseToCrop == null) {
            throw new NullPointerException("response message must not be null");
        }
        if (requestedBlock == null) {
            throw new NullPointerException("block option must not be null");
        }
        if (!responseToCrop.hasBlock(requestedBlock)) {
            throw new IllegalArgumentException("given response does not contain block");
        }
        int bodySize = responseToCrop.getPayloadSize();
        int from = requestedBlock.getOffset();
        int size = requestedBlock.getSize();
        if (requestedBlock.isBERT()) {
            size *= maxTcpBertBulkBlocks;
        }
        boolean m = false;
        if (responseToCrop.getOptions().hasBlock2()) {
            from -= responseToCrop.getOptions().getBlock2().getOffset();
            m = responseToCrop.getOptions().getBlock2().isM();
        }
        int to = Math.min(from + size, bodySize);
        int length = to - from;
        boolean m2 = m || to < bodySize;
        responseToCrop.getOptions().setBlock2(requestedBlock.getSzx(), m2, requestedBlock.getNum());
        LOGGER.debug("cropping response body [size={}] to block {}", Integer.valueOf(bodySize), requestedBlock);
        if (length > 0) {
            byte[] blockPayload = new byte[length];
            System.arraycopy(responseToCrop.getPayload(), from, blockPayload, 0, length);
            responseToCrop.setPayload(blockPayload);
            return;
        }
        responseToCrop.setPayload(Bytes.EMPTY);
    }
}
