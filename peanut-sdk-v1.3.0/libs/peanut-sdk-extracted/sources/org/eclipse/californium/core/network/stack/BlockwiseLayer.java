package org.eclipse.californium.core.network.stack;

import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.stack.BlockwiseStatus;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.util.LeastRecentlyUsedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/BlockwiseLayer.class */
public class BlockwiseLayer extends AbstractLayer {
    private static final int MINIMAL_BLOCK_SIZE = 16;
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockwiseLayer.class);
    private static final Logger HEALTH_LOGGER = LoggerFactory.getLogger(LOGGER.getName() + ".health");
    private final LeastRecentlyUsedCache<KeyUri, Block1BlockwiseStatus> block1Transfers;
    private final LeastRecentlyUsedCache<KeyUri, Block2BlockwiseStatus> block2Transfers;
    private final String tag;
    private volatile boolean enableStatus;
    private ScheduledFuture<?> statusLogger;
    private ScheduledFuture<?> cleanup;
    private final int maxTcpBertBulkBlocks;
    private final int maxMessageSize;
    private final int preferredBlockSzx;
    private final int blockTimeout;
    private final int blockInterval;
    private final int maxResourceBodySize;
    private final boolean strictBlock2Option;
    private final long healthStatusInterval;
    private final boolean enableAutoFailoverOn413;
    private final BlockwiseStatus.RemoveHandler removeHandler = new BlockwiseStatus.RemoveHandler() { // from class: org.eclipse.californium.core.network.stack.BlockwiseLayer.1
        @Override // org.eclipse.californium.core.network.stack.BlockwiseStatus.RemoveHandler
        public void remove(BlockwiseStatus status) {
            if (status instanceof Block1BlockwiseStatus) {
                BlockwiseLayer.this.clearBlock1Status((Block1BlockwiseStatus) status);
            } else if (status instanceof Block2BlockwiseStatus) {
                BlockwiseLayer.this.clearBlock2Status((Block2BlockwiseStatus) status);
            }
        }
    };
    private final AtomicInteger ignoredBlock2 = new AtomicInteger();

    public BlockwiseLayer(String tag, boolean enableBert, Configuration config) {
        this.tag = tag;
        int blockSize = ((Integer) config.get(CoapConfig.PREFERRED_BLOCK_SIZE)).intValue();
        int szx = BlockOption.size2Szx(blockSize);
        String blockSizeDescription = String.valueOf(blockSize);
        this.maxTcpBertBulkBlocks = enableBert ? ((Integer) config.get(CoapConfig.TCP_NUMBER_OF_BULK_BLOCKS)).intValue() : 1;
        if (this.maxTcpBertBulkBlocks > 1) {
            szx = 7;
            blockSizeDescription = "1024(BERT)";
        }
        this.maxMessageSize = ((Integer) config.get(CoapConfig.MAX_MESSAGE_SIZE)).intValue();
        this.preferredBlockSzx = szx;
        this.blockTimeout = config.getTimeAsInt(CoapConfig.BLOCKWISE_STATUS_LIFETIME, TimeUnit.MILLISECONDS);
        this.blockInterval = config.getTimeAsInt(CoapConfig.BLOCKWISE_STATUS_INTERVAL, TimeUnit.MILLISECONDS);
        this.maxResourceBodySize = ((Integer) config.get(CoapConfig.MAX_RESOURCE_BODY_SIZE)).intValue();
        int maxActivePeers = ((Integer) config.get(CoapConfig.MAX_ACTIVE_PEERS)).intValue();
        this.block1Transfers = new LeastRecentlyUsedCache<>(maxActivePeers / 10, maxActivePeers, this.blockTimeout, TimeUnit.MILLISECONDS);
        this.block1Transfers.setEvictingOnReadAccess(false);
        this.block1Transfers.addEvictionListener(new LeastRecentlyUsedCache.EvictionListener<Block1BlockwiseStatus>() { // from class: org.eclipse.californium.core.network.stack.BlockwiseLayer.2
            @Override // org.eclipse.californium.elements.util.LeastRecentlyUsedCache.EvictionListener
            public void onEviction(Block1BlockwiseStatus status) {
                if (status.complete()) {
                    BlockwiseLayer.LOGGER.debug("{}block1 transfer timed out!", BlockwiseLayer.this.tag);
                    status.timeoutCurrentTranfer();
                }
            }
        });
        this.block2Transfers = new LeastRecentlyUsedCache<>(maxActivePeers / 10, maxActivePeers, this.blockTimeout, TimeUnit.MILLISECONDS);
        this.block2Transfers.setEvictingOnReadAccess(false);
        this.block2Transfers.addEvictionListener(new LeastRecentlyUsedCache.EvictionListener<Block2BlockwiseStatus>() { // from class: org.eclipse.californium.core.network.stack.BlockwiseLayer.3
            @Override // org.eclipse.californium.elements.util.LeastRecentlyUsedCache.EvictionListener
            public void onEviction(Block2BlockwiseStatus status) {
                if (status.complete()) {
                    BlockwiseLayer.LOGGER.debug("{}block2 transfer timed out!", BlockwiseLayer.this.tag);
                    status.timeoutCurrentTranfer();
                }
            }
        });
        this.strictBlock2Option = ((Boolean) config.get(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION)).booleanValue();
        this.healthStatusInterval = config.get(SystemConfig.HEALTH_STATUS_INTERVAL, TimeUnit.MILLISECONDS).longValue();
        this.enableAutoFailoverOn413 = ((Boolean) config.get(CoapConfig.BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER)).booleanValue();
        LOGGER.info("{}BlockwiseLayer uses MAX_MESSAGE_SIZE={}, PREFERRED_BLOCK_SIZE={}, BLOCKWISE_STATUS_LIFETIME={}, MAX_RESOURCE_BODY_SIZE={}, BLOCKWISE_STRICT_BLOCK2_OPTION={}", new Object[]{tag, Integer.valueOf(this.maxMessageSize), blockSizeDescription, Integer.valueOf(this.blockTimeout), Integer.valueOf(this.maxResourceBodySize), Boolean.valueOf(this.strictBlock2Option)});
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void start() {
        if (this.healthStatusInterval > 0 && HEALTH_LOGGER.isDebugEnabled() && this.statusLogger == null) {
            this.statusLogger = this.secondaryExecutor.scheduleAtFixedRate(new Runnable() { // from class: org.eclipse.californium.core.network.stack.BlockwiseLayer.4
                @Override // java.lang.Runnable
                public void run() {
                    if (BlockwiseLayer.this.enableStatus) {
                        BlockwiseLayer.HEALTH_LOGGER.debug("{}{} block1 transfers", BlockwiseLayer.this.tag, Integer.valueOf(BlockwiseLayer.this.block1Transfers.size()));
                        Iterator<Block1BlockwiseStatus> iterator = BlockwiseLayer.this.block1Transfers.valuesIterator(false);
                        int max = 5;
                        while (iterator.hasNext()) {
                            BlockwiseLayer.HEALTH_LOGGER.debug("   block1 {}", iterator.next());
                            max--;
                            if (max == 0) {
                                break;
                            }
                        }
                        BlockwiseLayer.HEALTH_LOGGER.debug("{}{} block2 transfers", BlockwiseLayer.this.tag, Integer.valueOf(BlockwiseLayer.this.block2Transfers.size()));
                        Iterator<Block2BlockwiseStatus> iterator2 = BlockwiseLayer.this.block2Transfers.valuesIterator(false);
                        int max2 = 5;
                        while (iterator2.hasNext()) {
                            BlockwiseLayer.HEALTH_LOGGER.debug("   block2 {}", iterator2.next());
                            max2--;
                            if (max2 == 0) {
                                break;
                            }
                        }
                        BlockwiseLayer.HEALTH_LOGGER.debug("{}{} block2 responses ignored", BlockwiseLayer.this.tag, Integer.valueOf(BlockwiseLayer.this.ignoredBlock2.get()));
                        BlockwiseLayer.this.cleanupExpiredBlockStatus(true);
                    }
                }
            }, this.healthStatusInterval, this.healthStatusInterval, TimeUnit.MILLISECONDS);
        }
        this.cleanup = this.secondaryExecutor.scheduleAtFixedRate(new Runnable() { // from class: org.eclipse.californium.core.network.stack.BlockwiseLayer.5
            @Override // java.lang.Runnable
            public void run() {
                BlockwiseLayer.this.cleanupExpiredBlockStatus(false);
            }
        }, this.blockInterval, this.blockInterval, TimeUnit.MILLISECONDS);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void destroy() {
        if (this.statusLogger != null) {
            this.statusLogger.cancel(false);
            this.statusLogger = null;
        }
        if (this.cleanup != null) {
            this.cleanup.cancel(false);
            this.cleanup = null;
        }
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendRequest(Exchange exchange, Request request) {
        Request requestToSend = request;
        if (isTransparentBlockwiseHandlingEnabled() && !request.isMulticast() && !isRandomAccess(exchange)) {
            KeyUri key = KeyUri.getKey(exchange);
            Block2BlockwiseStatus status = getBlock2Status(key);
            if (status != null) {
                clearBlock2Status(status);
                status.completeOldTransfer(null);
            }
            if (requiresBlock1wise(request)) {
                try {
                    requestToSend = startBlockwiseUpload(key, exchange, request, this.preferredBlockSzx);
                } catch (BlockwiseTransferException ex) {
                    LOGGER.debug("{}{} {}", new Object[]{this.tag, key, ex.getMessage()});
                    if (!ex.isCompleted()) {
                        request.setSendError(ex);
                    }
                }
            }
        }
        exchange.setCurrentRequest(requestToSend);
        lower().sendRequest(exchange, requestToSend);
    }

    private Request startBlockwiseUpload(KeyUri key, Exchange exchange, Request request, int blockSzx) throws BlockwiseTransferException {
        Block1BlockwiseStatus status = getOutboundBlock1Status(key, exchange, request, true);
        Request block = status.getNextRequestBlock(blockSzx);
        Token token = request.getToken();
        if (token != null) {
            block.setToken(token);
        }
        return block;
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveRequest(Exchange exchange, Request request) {
        if (isTransparentBlockwiseHandlingEnabled()) {
            if (request.getOptions().hasBlock1()) {
                handleInboundBlockwiseUpload(exchange, request);
                return;
            }
            BlockOption block2 = request.getOptions().getBlock2();
            if (block2 != null && block2.getNum() > 0) {
                KeyUri key = KeyUri.getKey(exchange);
                Block2BlockwiseStatus status = getBlock2Status(key);
                if (status != null) {
                    handleInboundRequestForNextBlock(exchange, request, status);
                    return;
                }
                LOGGER.debug("{}peer wants to retrieve individual block2 {} of {}, delivering request to application layer", new Object[]{this.tag, block2, key});
            }
        }
        upper().receiveRequest(exchange, request);
    }

    private void handleInboundBlockwiseUpload(Exchange exchange, Request request) {
        if (requestExceedsMaxBodySize(request)) {
            int maxResourceBodySize = getMaxResourceBodySize(request);
            Response error = Response.createResponse(request, CoAP.ResponseCode.REQUEST_ENTITY_TOO_LARGE);
            error.setPayload(String.format("body too large, max. %d bytes", Integer.valueOf(maxResourceBodySize)));
            error.getOptions().setSize1(maxResourceBodySize);
            lower().sendResponse(exchange, error);
            return;
        }
        BlockOption block1 = request.getOptions().getBlock1();
        LOGGER.debug("{}inbound request contains block1 option {}", this.tag, block1);
        KeyUri key = KeyUri.getKey(exchange);
        Block1BlockwiseStatus status = getInboundBlock1Status(key, exchange, request, false);
        int blockOffset = block1.getOffset();
        if (blockOffset == 0 && !status.isStarting()) {
            status = getInboundBlock1Status(key, exchange, request, true);
        } else if (!status.hasContentFormat(request.getOptions().getContentFormat())) {
            sendBlock1ErrorResponse(status, exchange, request, CoAP.ResponseCode.REQUEST_ENTITY_INCOMPLETE, "unexpected Content-Format");
            return;
        }
        try {
            status.addBlock(request);
            if (block1.isM()) {
                LOGGER.debug("{}acknowledging incoming block1 [num={}], expecting more blocks to come", this.tag, Integer.valueOf(block1.getNum()));
                Response piggybacked = Response.createResponse(request, CoAP.ResponseCode.CONTINUE);
                BlockOption block12 = getLimitedBlockOption(block1);
                piggybacked.getOptions().setBlock1(block12.getSzx(), true, block12.getNum());
                lower().sendResponse(exchange, piggybacked);
            } else {
                LOGGER.debug("{}peer has sent last block1 [num={}], delivering request to application layer", this.tag, Integer.valueOf(block1.getNum()));
                exchange.setBlock1ToAck(block1);
                Request assembled = new Request(request.getCode());
                status.assembleReceivedMessage(assembled);
                assembled.setMID(request.getMID());
                assembled.setToken(request.getToken());
                assembled.setScheme(request.getScheme());
                assembled.getOptions().setBlock2(request.getOptions().getBlock2());
                clearBlock1Status(status);
                exchange.setRequest(assembled);
                upper().receiveRequest(exchange, assembled);
            }
        } catch (BlockwiseTransferException ex) {
            CoAP.ResponseCode code = ex.getResponseCode();
            LOGGER.debug("{}peer {} {}. Responding with {}", new Object[]{this.tag, key, ex.getMessage(), code});
            sendBlock1ErrorResponse(status, exchange, request, code, ex.getMessage());
        }
    }

    private void sendBlock1ErrorResponse(Block1BlockwiseStatus status, Exchange exchange, Request request, CoAP.ResponseCode errorCode, String message) {
        BlockOption block1 = request.getOptions().getBlock1();
        Response error = Response.createResponse(request, errorCode);
        error.getOptions().setBlock1(block1.getSzx(), block1.isM(), block1.getNum());
        error.setPayload(message);
        clearBlock1Status(status);
        lower().sendResponse(exchange, error);
    }

    private void handleInboundRequestForNextBlock(Exchange exchange, Request request, Block2BlockwiseStatus status) {
        BlockOption block2 = request.getOptions().getBlock2();
        Response nextBlockResponse = status.getNextResponseBlock(getLimitedBlockOption(block2));
        if (nextBlockResponse.getOptions().getBlock2().isM()) {
            LOGGER.debug("{}peer has requested intermediary block of blockwise transfer: {}", this.tag, status);
        } else {
            LOGGER.debug("{}peer has requested last block of blockwise transfer: {}", this.tag, status);
            clearBlock2Status(status);
        }
        lower().sendResponse(exchange, nextBlockResponse);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendResponse(Exchange exchange, Response response) {
        BlockOption block2;
        Response responseToSend = response;
        if (isTransparentBlockwiseHandlingEnabled()) {
            BlockOption requestBlock2 = exchange.getRequest().getOptions().getBlock2();
            if (isRandomAccess(exchange)) {
                BlockOption responseBlock2 = response.getOptions().getBlock2();
                if (responseBlock2 != null) {
                    if (requestBlock2.getOffset() != responseBlock2.getOffset()) {
                        LOGGER.warn("{}resource [{}] implementation error, peer requested block offset {} but resource returned block offest {}", new Object[]{this.tag, exchange.getRequest().getURI(), Integer.valueOf(requestBlock2.getOffset()), Integer.valueOf(responseBlock2.getOffset())});
                        responseToSend = Response.createResponse(exchange.getRequest(), CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                        responseToSend.setType(response.getType());
                        responseToSend.setMID(response.getMID());
                        responseToSend.addMessageObservers(response.getMessageObservers());
                    }
                } else if (response.hasBlock(requestBlock2)) {
                    BlockOption block22 = getLimitedBlockOption(requestBlock2);
                    Block2BlockwiseStatus.crop(responseToSend, block22, this.maxTcpBertBulkBlocks);
                } else if (!response.isError()) {
                    responseToSend = Response.createResponse(exchange.getRequest(), CoAP.ResponseCode.BAD_OPTION);
                    responseToSend.setType(response.getType());
                    responseToSend.setMID(response.getMID());
                    responseToSend.addMessageObservers(response.getMessageObservers());
                }
            } else if (requiresBlock2wise(response, requestBlock2)) {
                KeyUri key = KeyUri.getKey(exchange);
                Block2BlockwiseStatus status = getOutboundBlock2Status(key, exchange, response, true);
                if (requestBlock2 != null) {
                    block2 = getLimitedBlockOption(requestBlock2);
                } else {
                    block2 = new BlockOption(this.preferredBlockSzx, false, 0);
                }
                responseToSend = status.getNextResponseBlock(block2);
                if (!responseToSend.getOptions().getBlock2().isM()) {
                    clearBlock2Status(status);
                }
            } else if (requiresBlock2(requestBlock2)) {
                BlockOption block23 = getLimitedBlockOption(requestBlock2);
                Block2BlockwiseStatus.crop(responseToSend, block23, this.maxTcpBertBulkBlocks);
            }
            BlockOption block1 = exchange.getBlock1ToAck();
            if (block1 != null) {
                exchange.setBlock1ToAck(null);
                responseToSend.getOptions().setBlock1(block1);
            }
        }
        lower().sendResponse(exchange, responseToSend);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveResponse(Exchange exchange, Response response) {
        if (isTransparentBlockwiseHandlingEnabled() && !exchange.getRequest().isMulticast()) {
            if (response.isError()) {
                LOGGER.debug("{} received error {}:", this.tag, response);
                switch (response.getCode()) {
                    case REQUEST_ENTITY_INCOMPLETE:
                    case REQUEST_ENTITY_TOO_LARGE:
                        if (handleEntityTooLarge(exchange, response)) {
                            return;
                        }
                        Block1BlockwiseStatus status = getBlock1Status(KeyUri.getKey(exchange));
                        if (status != null) {
                            clearBlock1Status(status);
                        }
                        break;
                        break;
                }
                if (exchange.getRequest() != exchange.getCurrentRequest()) {
                    Response resp = new Response(response.getCode());
                    resp.setToken(exchange.getRequest().getToken());
                    if (exchange.getRequest().getType() == CoAP.Type.CON) {
                        resp.setType(CoAP.Type.ACK);
                        resp.setMID(exchange.getRequest().getMID());
                    } else {
                        resp.setType(CoAP.Type.NON);
                    }
                    resp.setSourceContext(response.getSourceContext());
                    resp.setPayload(response.getPayload());
                    resp.setOptions(response.getOptions());
                    resp.setApplicationRttNanos(exchange.calculateApplicationRtt());
                    Long rtt = response.getTransmissionRttNanos();
                    if (rtt != null) {
                        resp.setTransmissionRttNanos(rtt.longValue());
                    }
                    exchange.setResponse(resp);
                    upper().receiveResponse(exchange, resp);
                    return;
                }
                upper().receiveResponse(exchange, response);
                return;
            }
            if (response.getMaxResourceBodySize() == 0) {
                response.setMaxResourceBodySize(exchange.getRequest().getMaxResourceBodySize());
            }
            if (!isRandomAccess(exchange)) {
                KeyUri key = KeyUri.getKey(exchange);
                if (discardBlock2(key, getBlock2Status(key), exchange, response)) {
                    return;
                }
            }
            if (!response.hasBlockOption()) {
                exchange.setResponse(response);
                upper().receiveResponse(exchange, response);
                return;
            }
            if (response.getOptions().hasBlock1()) {
                handleBlock1Response(exchange, response);
            }
            if (response.getOptions().hasBlock2()) {
                handleBlock2Response(exchange, response);
                return;
            }
            return;
        }
        exchange.setResponse(response);
        upper().receiveResponse(exchange, response);
    }

    private boolean handleEntityTooLarge(Exchange exchange, Response response) {
        Block1BlockwiseStatus status;
        if (this.enableAutoFailoverOn413) {
            KeyUri key = KeyUri.getKey(exchange);
            try {
                Request initialRequest = exchange.getRequest();
                if (!response.getOptions().hasBlock1()) {
                    if (!exchange.getRequest().isCanceled()) {
                        Request requestToSend = null;
                        Integer maxSize = response.getOptions().getSize1();
                        if (maxSize != null && (maxSize.intValue() < 16 || maxSize.intValue() >= initialRequest.getPayloadSize())) {
                            maxSize = null;
                        }
                        if (maxSize == null && initialRequest.getPayloadSize() > 16) {
                            maxSize = Integer.valueOf(initialRequest.getPayloadSize() - 1);
                        }
                        if (maxSize != null) {
                            synchronized (this.block1Transfers) {
                                if (getBlock1Status(key) == null) {
                                    int blockszx = BlockOption.size2Szx(maxSize.intValue());
                                    requestToSend = startBlockwiseUpload(key, exchange, initialRequest, Math.min(blockszx, this.preferredBlockSzx));
                                }
                            }
                        }
                        if (requestToSend != null) {
                            exchange.setCurrentRequest(requestToSend);
                            lower().sendRequest(exchange, requestToSend);
                            return true;
                        }
                        return false;
                    }
                    return false;
                }
                BlockOption block1 = response.getOptions().getBlock1();
                Request blockRequest = null;
                boolean start = !initialRequest.isCanceled() && block1.getNum() == 0 && block1.getSize() < initialRequest.getPayloadSize();
                synchronized (this.block1Transfers) {
                    status = getBlock1Status(key);
                    if (status == null && start) {
                        blockRequest = startBlockwiseUpload(key, exchange, initialRequest, Math.min(block1.getSzx(), this.preferredBlockSzx));
                    }
                }
                if (status == null) {
                    if (blockRequest != null) {
                        exchange.setCurrentRequest(blockRequest);
                        lower().sendRequest(exchange, blockRequest);
                        return true;
                    }
                } else {
                    if (!status.hasMatchingToken(response)) {
                        LOGGER.debug("{}discarding obsolete block1 response: {}", this.tag, response);
                        return true;
                    }
                    if (initialRequest.isCanceled()) {
                        clearBlock1Status(status);
                        return true;
                    }
                    if (status.isStarting() && block1.getSzx() < this.preferredBlockSzx) {
                        status.restart();
                        sendNextBlock(exchange, response, status);
                        return true;
                    }
                }
                return false;
            } catch (BlockwiseTransferException ex) {
                LOGGER.debug("{}{} {}", new Object[]{this.tag, key, ex.getMessage()});
                return false;
            }
            LOGGER.debug("{}{} {}", new Object[]{this.tag, key, ex.getMessage()});
            return false;
        }
        return false;
    }

    private void handleBlock1Response(Exchange exchange, Response response) {
        BlockOption block1 = response.getOptions().getBlock1();
        LOGGER.debug("{}received response acknowledging block1 {}", this.tag, block1);
        KeyUri key = KeyUri.getKey(exchange);
        Block1BlockwiseStatus status = getBlock1Status(key);
        if (status == null) {
            LOGGER.debug("{}discarding unexpected block1 response: {}", this.tag, response);
            return;
        }
        if (!status.hasMatchingToken(response)) {
            LOGGER.debug("{}discarding obsolete block1 response: {}", this.tag, response);
            return;
        }
        if (exchange.getRequest().isCanceled()) {
            clearBlock1Status(status);
            return;
        }
        if (!status.isComplete()) {
            if (block1.isM()) {
                if (response.getCode() == CoAP.ResponseCode.CONTINUE) {
                    sendNextBlock(exchange, response, status);
                    return;
                } else {
                    clearBlock1Status(status);
                    exchange.getRequest().setRejected(true);
                    return;
                }
            }
            sendNextBlock(exchange, response, status);
            return;
        }
        clearBlock1Status(status);
        if (response.getOptions().hasBlock2()) {
            LOGGER.debug("{}Block1 followed by Block2 transfer", this.tag);
        } else {
            exchange.setResponse(response);
            upper().receiveResponse(exchange, response);
        }
    }

    private void sendNextBlock(Exchange exchange, Response response, Block1BlockwiseStatus status) {
        Request nextBlock = null;
        try {
            if (status.isComplete()) {
                LOGGER.debug("{}stopped block1 transfer, droping request.", this.tag);
            } else {
                int blockSzx = Math.min(response.getOptions().getBlock1().getSzx(), this.preferredBlockSzx);
                Request nextBlock2 = status.getNextRequestBlock(blockSzx);
                nextBlock2.setToken(response.getToken());
                nextBlock2.setDestinationContext(status.getFollowUpEndpointContext(response.getSourceContext()));
                LOGGER.debug("{}sending (next) Block1 [num={}]: {}", new Object[]{this.tag, Integer.valueOf(nextBlock2.getOptions().getBlock1().getNum()), nextBlock2});
                exchange.setCurrentRequest(nextBlock2);
                lower().sendRequest(exchange, nextBlock2);
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("{}cannot process next block request, aborting request!", this.tag, ex);
            if (0 != 0) {
                nextBlock.setSendError(ex);
            } else {
                exchange.getRequest().setSendError(ex);
            }
        } catch (BlockwiseTransferException ex2) {
            LOGGER.warn("{}cannot process next block request, aborting request!", this.tag, ex2);
            if (!ex2.isCompleted()) {
                exchange.getRequest().setSendError(ex2);
            }
        }
    }

    private boolean discardBlock2(KeyUri key, Block2BlockwiseStatus status, Exchange exchange, Response response) {
        BlockOption block = response.getOptions().getBlock2();
        if (status == null) {
            if (block != null && block.getNum() != 0) {
                LOGGER.debug("{}discarding stale block2 response [{}, {}] received without ongoing block2 transfer for {}", new Object[]{this.tag, exchange.getNotificationNumber(), response, key});
                exchange.setComplete();
                return true;
            }
            return false;
        }
        boolean starting = block == null || block.getNum() == 0;
        if (starting) {
            if (status.isNew(response)) {
                LOGGER.debug("{}discarding outdated block2 transfer {}, current is [{}]", new Object[]{this.tag, status.getObserve(), response});
                clearBlock2Status(status);
                status.completeOldTransfer(exchange);
                return false;
            }
            LOGGER.debug("{}discarding old block2 transfer [{}], received during ongoing block2 transfer {}", new Object[]{this.tag, response, status.getObserve()});
            status.completeNewTranfer(exchange);
            return true;
        }
        if (!status.matchTransfer(exchange)) {
            LOGGER.debug("{}discarding outdate block2 response [{}, {}] received during ongoing block2 transfer {}", new Object[]{this.tag, exchange.getNotificationNumber(), response, status.getObserve()});
            status.completeNewTranfer(exchange);
            return true;
        }
        return false;
    }

    private void handleBlock2Response(Exchange exchange, Response response) {
        BlockOption block2 = response.getOptions().getBlock2();
        KeyUri key = KeyUri.getKey(exchange);
        if (exchange.getRequest().isCanceled()) {
            Block2BlockwiseStatus status = getBlock2Status(key);
            if (status != null) {
                clearBlock2Status(status);
            }
            if (response.isNotification()) {
                upper().receiveResponse(exchange, response);
                return;
            }
            return;
        }
        if (responseExceedsMaxBodySize(response)) {
            String msg = String.format("requested resource body [%d bytes] exceeds max buffer size [%d bytes], aborting request", response.getOptions().getSize2(), Integer.valueOf(getMaxResourceBodySize(response)));
            LOGGER.debug("{}{}", this.tag, msg);
            exchange.getRequest().setOnResponseError(new IllegalStateException(msg));
            return;
        }
        if (isRandomAccess(exchange)) {
            exchange.setResponse(response);
            upper().receiveResponse(exchange, response);
            return;
        }
        synchronized (this.block2Transfers) {
            if (discardBlock2(key, getBlock2Status(key), exchange, response)) {
                return;
            }
            Block2BlockwiseStatus status2 = getInboundBlock2Status(key, exchange, response);
            try {
                status2.addBlock(response);
                if (block2.isM()) {
                    requestNextBlock(exchange, response, status2);
                } else {
                    LOGGER.debug("{}all blocks have been retrieved, assembling response and delivering to application layer", this.tag);
                    Response assembled = new Response(response.getCode());
                    status2.assembleReceivedMessage(assembled);
                    assembled.setApplicationRttNanos(exchange.calculateApplicationRtt());
                    Long rtt = response.getTransmissionRttNanos();
                    if (rtt != null) {
                        assembled.setTransmissionRttNanos(rtt.longValue());
                    }
                    clearBlock2Status(status2);
                    LOGGER.debug("{}assembled response: {}", this.tag, assembled);
                    exchange.setCurrentRequest(exchange.getRequest());
                    exchange.setResponse(assembled);
                    upper().receiveResponse(exchange, assembled);
                }
            } catch (BlockwiseTransferException ex) {
                this.ignoredBlock2.incrementAndGet();
                LOGGER.debug("{}peer {}{}. Ignores response", new Object[]{this.tag, key, ex.getMessage()});
                if (!ex.isCompleted()) {
                    exchange.getRequest().setOnResponseError(ex);
                }
            }
        }
    }

    private void requestNextBlock(Exchange exchange, Response response, Block2BlockwiseStatus status) {
        int blockSzx = Math.min(response.getOptions().getBlock2().getSzx(), this.preferredBlockSzx);
        if (response.isNotification() && exchange.isNotification()) {
            exchange.getRequest().addMessageObserver(new CleanupMessageObserver(exchange));
        }
        try {
            Request block = status.getNextRequestBlock(blockSzx);
            block.setDestinationContext(status.getFollowUpEndpointContext(response.getSourceContext()));
            if (!response.isNotification()) {
                block.setToken(response.getToken());
            }
            if (status.isComplete()) {
                LOGGER.debug("{}stopped block2 transfer, droping response.", this.tag);
            } else {
                LOGGER.debug("{}requesting next Block2 [num={}]: {}", new Object[]{this.tag, Integer.valueOf(block.getOptions().getBlock2().getNum()), block});
                exchange.setCurrentRequest(block);
                lower().sendRequest(exchange, block);
            }
        } catch (RuntimeException ex) {
            LOGGER.debug("{}cannot process next block request, aborting request!", this.tag, ex);
            if (!exchange.isComplete()) {
                exchange.getRequest().setSendError(ex);
            }
        } catch (BlockwiseTransferException ex2) {
            LOGGER.debug("{}{} Stop next block request!", this.tag, ex2.getMessage());
            if (!ex2.isCompleted()) {
                exchange.getRequest().setSendError(ex2);
            }
        }
    }

    private Block1BlockwiseStatus getOutboundBlock1Status(KeyUri key, Exchange exchange, Request request, boolean reset) {
        Integer size = null;
        Block1BlockwiseStatus previousStatus = null;
        Block1BlockwiseStatus status = null;
        synchronized (this.block1Transfers) {
            if (reset) {
                previousStatus = this.block1Transfers.remove(key);
            } else {
                status = this.block1Transfers.get(key);
            }
            if (status == null) {
                status = Block1BlockwiseStatus.forOutboundRequest(key, this.removeHandler, exchange, request, this.maxTcpBertBulkBlocks);
                this.block1Transfers.put(key, status);
                this.enableStatus = true;
                size = Integer.valueOf(this.block1Transfers.size());
            }
        }
        if (previousStatus != null && previousStatus.cancelRequest()) {
            LOGGER.debug("{}stop previous block1 transfer {} {} for new {}", new Object[]{this.tag, key, previousStatus, request});
        }
        if (size != null) {
            LOGGER.debug("{}created tracker for outbound block1 transfer {}, transfers in progress: {}", new Object[]{this.tag, status, size});
        } else {
            LOGGER.debug("{}block1 transfer {} for {}", new Object[]{this.tag, key, request});
        }
        return status;
    }

    private Block1BlockwiseStatus getInboundBlock1Status(KeyUri key, Exchange exchange, Request request, boolean reset) {
        Integer size = null;
        Block1BlockwiseStatus previousStatus = null;
        Block1BlockwiseStatus status = null;
        int maxPayloadSize = getMaxResourceBodySize(request);
        synchronized (this.block1Transfers) {
            if (reset) {
                previousStatus = this.block1Transfers.remove(key);
            } else {
                status = this.block1Transfers.get(key);
            }
            if (status == null) {
                status = Block1BlockwiseStatus.forInboundRequest(key, this.removeHandler, exchange, request, maxPayloadSize, this.maxTcpBertBulkBlocks);
                this.block1Transfers.put(key, status);
                this.enableStatus = true;
                size = Integer.valueOf(this.block1Transfers.size());
            }
        }
        if (previousStatus != null && previousStatus.complete()) {
            LOGGER.debug("{}stop previous block1 transfer {} {} for new {}", new Object[]{this.tag, key, previousStatus, request});
        }
        if (size != null) {
            LOGGER.debug("{}created tracker for inbound block1 transfer {}, transfers in progress: {}", new Object[]{this.tag, status, size});
        } else {
            LOGGER.debug("{}block1 transfer {} for {}", new Object[]{this.tag, key, request});
        }
        return status;
    }

    private Block2BlockwiseStatus getOutboundBlock2Status(KeyUri key, Exchange exchange, Response response, boolean reset) {
        Integer size = null;
        Block2BlockwiseStatus previousStatus = null;
        Block2BlockwiseStatus status = null;
        synchronized (this.block2Transfers) {
            if (reset) {
                previousStatus = this.block2Transfers.remove(key);
            } else {
                status = this.block2Transfers.get(key);
            }
            if (status == null) {
                status = Block2BlockwiseStatus.forOutboundResponse(key, this.removeHandler, exchange, response, this.maxTcpBertBulkBlocks);
                this.block2Transfers.put(key, status);
                this.enableStatus = true;
                size = Integer.valueOf(this.block2Transfers.size());
            }
        }
        if (previousStatus != null && previousStatus.completeResponse()) {
            LOGGER.debug("{}stop previous block2 transfer {} {} for new {}", new Object[]{this.tag, key, previousStatus, response});
        }
        if (size != null) {
            LOGGER.debug("{}created tracker for outbound block2 transfer {}, transfers in progress: {}", new Object[]{this.tag, status, size});
        } else {
            LOGGER.debug("{}block2 transfer {} for {}", new Object[]{this.tag, key, response});
        }
        return status;
    }

    private Block2BlockwiseStatus getInboundBlock2Status(KeyUri key, Exchange exchange, Response response) {
        Block2BlockwiseStatus status;
        Integer size = null;
        int maxPayloadSize = getMaxResourceBodySize(response);
        synchronized (this.block2Transfers) {
            status = this.block2Transfers.get(key);
            if (status == null) {
                status = Block2BlockwiseStatus.forInboundResponse(key, this.removeHandler, exchange, response, maxPayloadSize, this.maxTcpBertBulkBlocks);
                this.block2Transfers.put(key, status);
                this.enableStatus = true;
                size = Integer.valueOf(this.block2Transfers.size());
            }
        }
        if (size != null) {
            LOGGER.debug("{}created tracker for {} inbound block2 transfer {}, transfers in progress: {}, {}", new Object[]{this.tag, key, status, size, response});
        }
        return status;
    }

    private Block1BlockwiseStatus getBlock1Status(KeyUri key) {
        Block1BlockwiseStatus block1BlockwiseStatus;
        synchronized (this.block1Transfers) {
            block1BlockwiseStatus = this.block1Transfers.get(key);
        }
        return block1BlockwiseStatus;
    }

    private Block2BlockwiseStatus getBlock2Status(KeyUri key) {
        Block2BlockwiseStatus block2BlockwiseStatus;
        synchronized (this.block2Transfers) {
            block2BlockwiseStatus = this.block2Transfers.get(key);
        }
        return block2BlockwiseStatus;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanupExpiredBlockStatus(boolean dump) {
        int count;
        int count2;
        synchronized (this.block1Transfers) {
            count = 0 + this.block1Transfers.removeExpiredEntries(128);
        }
        synchronized (this.block2Transfers) {
            count2 = count + this.block2Transfers.removeExpiredEntries(128);
        }
        if (dump) {
            HEALTH_LOGGER.debug("{}cleaned up {} block transfers!", this.tag, Integer.valueOf(count2));
        } else if (this.enableStatus && count2 > 0) {
            LOGGER.info("{}cleaned up {} block transfers!", this.tag, Integer.valueOf(count2));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Block1BlockwiseStatus clearBlock1Status(Block1BlockwiseStatus status) {
        Block1BlockwiseStatus removedTracker;
        int size;
        synchronized (this.block1Transfers) {
            removedTracker = this.block1Transfers.remove(status.getKeyUri(), status);
            size = this.block1Transfers.size();
        }
        if (removedTracker != null && removedTracker.complete()) {
            LOGGER.debug("{}removing block1 tracker [{}], block1 transfers still in progress: {}", new Object[]{this.tag, status.getKeyUri(), Integer.valueOf(size)});
        }
        return removedTracker;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Block2BlockwiseStatus clearBlock2Status(Block2BlockwiseStatus status) {
        Block2BlockwiseStatus removedTracker;
        int size;
        synchronized (this.block2Transfers) {
            removedTracker = this.block2Transfers.remove(status.getKeyUri(), status);
            size = this.block2Transfers.size();
        }
        if (removedTracker != null && removedTracker.complete()) {
            LOGGER.debug("{}removing block2 tracker [{}], block2 transfers still in progress: {}", new Object[]{this.tag, status.getKeyUri(), Integer.valueOf(size)});
        }
        return removedTracker;
    }

    private boolean requiresBlock1wise(Request request) {
        boolean blockwiseRequired = request.getPayloadSize() > this.maxMessageSize;
        if (blockwiseRequired) {
            LOGGER.debug("{}request body [{}/{}] requires blockwise transfer", new Object[]{this.tag, Integer.valueOf(request.getPayloadSize()), Integer.valueOf(this.maxMessageSize)});
        }
        return blockwiseRequired;
    }

    private boolean requiresBlock2wise(Response response, BlockOption requestBlock2) {
        boolean blockwiseRequired = response.getPayloadSize() > this.maxMessageSize;
        if (!blockwiseRequired && requestBlock2 != null) {
            int szx = Math.min(requestBlock2.getSzx(), this.preferredBlockSzx);
            int size = BlockOption.szx2Size(szx);
            blockwiseRequired = response.getPayloadSize() > size;
        }
        if (blockwiseRequired) {
            LOGGER.debug("{}response body [{}/{}] requires blockwise transfer", new Object[]{this.tag, Integer.valueOf(response.getPayloadSize()), Integer.valueOf(this.maxMessageSize)});
        }
        return blockwiseRequired;
    }

    private boolean requiresBlock2(BlockOption requestBlock2) {
        boolean block2Required = this.strictBlock2Option && requestBlock2 != null;
        if (block2Required) {
            LOGGER.debug("{}response requires requested {} blockwise transfer", this.tag, requestBlock2);
        }
        return block2Required;
    }

    private boolean isRandomAccess(Exchange exchange) {
        BlockOption block2 = exchange.getRequest().getOptions().getBlock2();
        return block2 != null && block2.getNum() > 0;
    }

    private boolean isTransparentBlockwiseHandlingEnabled() {
        return this.maxResourceBodySize > 0;
    }

    private boolean responseExceedsMaxBodySize(Response response) {
        return response.getOptions().hasSize2() && response.getOptions().getSize2().intValue() > getMaxResourceBodySize(response);
    }

    private boolean requestExceedsMaxBodySize(Request request) {
        return request.getOptions().hasSize1() && request.getOptions().getSize1().intValue() > getMaxResourceBodySize(request);
    }

    private int getMaxResourceBodySize(Message message) {
        int maxPayloadSize = message.getMaxResourceBodySize();
        if (maxPayloadSize == 0) {
            maxPayloadSize = this.maxResourceBodySize;
        }
        return maxPayloadSize;
    }

    private BlockOption getLimitedBlockOption(BlockOption block) {
        if (this.preferredBlockSzx < block.getSzx()) {
            int offset = block.getOffset();
            int size = BlockOption.szx2Size(this.preferredBlockSzx);
            if (offset % size != 0) {
                throw new IllegalStateException("Block offset " + offset + " doesn't align with preferred blocksize " + size + "!");
            }
            return new BlockOption(this.preferredBlockSzx, block.isM(), offset / size);
        }
        return block;
    }

    public boolean isEmpty() {
        return this.block1Transfers.size() == 0 && this.block2Transfers.size() == 0;
    }
}
