package org.eclipse.californium.core.network.stack;

import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.stack.BlockwiseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/Block1BlockwiseStatus.class */
public final class Block1BlockwiseStatus extends BlockwiseStatus {
    private static final Logger LOGGER = LoggerFactory.getLogger(Block1BlockwiseStatus.class);

    private Block1BlockwiseStatus(KeyUri keyUri, BlockwiseStatus.RemoveHandler removeHandler, Exchange exchange, Request request, int maxSize, int maxTcpBertBulkBlocks) {
        super(keyUri, removeHandler, exchange, request, maxSize, maxTcpBertBulkBlocks);
    }

    public static Block1BlockwiseStatus forOutboundRequest(KeyUri keyUri, BlockwiseStatus.RemoveHandler removeHandler, Exchange exchange, Request request, int maxTcpBertBulkBlocks) {
        Block1BlockwiseStatus status = new Block1BlockwiseStatus(keyUri, removeHandler, exchange, request, request.getPayloadSize(), maxTcpBertBulkBlocks);
        try {
            status.addBlock(request.getPayload());
            status.flipBlocksBuffer();
        } catch (BlockwiseTransferException ex) {
            LOGGER.warn("buffer overflow on start", ex);
        }
        return status;
    }

    public static Block1BlockwiseStatus forInboundRequest(KeyUri keyUri, BlockwiseStatus.RemoveHandler removeHandler, Exchange exchange, Request block, int maxBodySize, int maxTcpBertBulkBlocks) {
        int bufferSize = maxBodySize;
        if (block.getOptions().hasSize1()) {
            bufferSize = block.getOptions().getSize1().intValue();
        }
        Block1BlockwiseStatus status = new Block1BlockwiseStatus(keyUri, removeHandler, exchange, block, bufferSize, maxTcpBertBulkBlocks);
        return status;
    }

    public synchronized void addBlock(Request requestBlock) throws BlockwiseTransferException {
        if (requestBlock == null) {
            throw new NullPointerException("request block must not be null");
        }
        BlockOption block1 = requestBlock.getOptions().getBlock1();
        if (block1 == null) {
            throw new IllegalArgumentException("request block has no block1 option");
        }
        int from = getCurrentPosition();
        int offset = block1.getOffset();
        if (from != offset) {
            throw new BlockwiseTransferException("request block1 offset " + offset + " doesn't match the current position " + from + "!", CoAP.ResponseCode.REQUEST_ENTITY_INCOMPLETE);
        }
        addBlock(requestBlock.getPayload());
        if (block1.isM()) {
            setCurrentSzx(block1.getSzx());
            int size = block1.getSize();
            int from2 = getCurrentPosition();
            if (from2 % size != 0) {
                throw new BlockwiseTransferException("Block1 buffer position " + from2 + " doesn't align with blocksize " + size + "!", CoAP.ResponseCode.REQUEST_ENTITY_INCOMPLETE);
            }
            setCurrentNum(from2 / size);
        }
    }

    public synchronized Request getNextRequestBlock(int blockSzx) throws BlockwiseTransferException {
        byte[] blockPayload;
        setCurrentSzx(blockSzx);
        int size = getCurrentSize();
        int from = getCurrentPosition();
        if (from % size != 0) {
            throw new BlockwiseTransferException("Block1 buffer position " + from + " doesn't align with blocksize " + size + "!");
        }
        boolean m = false;
        int bodySize = getBufferSize();
        int num = from / size;
        setCurrentNum(num);
        Request block = new Request(((Request) this.firstMessage).getCode());
        prepareOutgoingMessage(this.firstMessage, block, num == 0);
        if (num == 0) {
            if (!block.getOptions().hasSize1()) {
                block.getOptions().setSize1(bodySize);
            }
        } else {
            block.getOptions().removeSize1();
            block.getOptions().setIfNoneMatch(false);
        }
        if (0 < bodySize && from < bodySize && (blockPayload = getBlock(from, getCurrentPayloadSize())) != null) {
            m = from + blockPayload.length < bodySize;
            block.setPayload(blockPayload);
        }
        block.getOptions().setBlock1(blockSzx, m, num);
        setComplete(!m);
        return block;
    }

    public boolean cancelRequest() {
        if (complete()) {
            Request request = (Request) this.firstMessage;
            request.cancel();
            return true;
        }
        return false;
    }

    public boolean hasMatchingToken(Response response) {
        return response.getToken().equals(this.firstMessage.getToken());
    }
}
