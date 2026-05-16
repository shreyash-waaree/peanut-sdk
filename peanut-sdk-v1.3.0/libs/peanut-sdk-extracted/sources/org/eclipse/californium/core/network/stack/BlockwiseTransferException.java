package org.eclipse.californium.core.network.stack;

import org.eclipse.californium.core.coap.CoAP;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/BlockwiseTransferException.class */
public class BlockwiseTransferException extends Exception {
    private static final long serialVersionUID = 1357;
    private final boolean completed;
    private final CoAP.ResponseCode code;

    public BlockwiseTransferException(String message) {
        super(message);
        this.completed = false;
        this.code = null;
    }

    public BlockwiseTransferException(String message, CoAP.ResponseCode code) {
        super(message);
        this.completed = false;
        this.code = code;
    }

    public BlockwiseTransferException(String message, boolean completed) {
        super(message);
        this.completed = completed;
        this.code = null;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public CoAP.ResponseCode getResponseCode() {
        return this.code;
    }
}
