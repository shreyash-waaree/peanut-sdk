package org.eclipse.californium.core.network.stack;

import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/ReliabilityLayerParameters.class */
public class ReliabilityLayerParameters {
    private final int ackTimeout;
    private final int maxAckTimeout;
    private final float ackRandomFactor;
    private final float ackTimeoutScale;
    private final int maxRetransmit;
    private final int nstart;

    ReliabilityLayerParameters(int ackTimeout, int maxAckTimeout, float ackRandomFactor, float ackTimeoutScale, int maxRetransmit, int nstart) {
        this.ackTimeout = ackTimeout;
        this.maxAckTimeout = maxAckTimeout;
        this.ackRandomFactor = ackRandomFactor;
        this.ackTimeoutScale = ackTimeoutScale;
        this.maxRetransmit = maxRetransmit;
        this.nstart = nstart;
    }

    public int getAckTimeout() {
        return this.ackTimeout;
    }

    public int getMaxAckTimeout() {
        return this.maxAckTimeout;
    }

    public float getAckRandomFactor() {
        return this.ackRandomFactor;
    }

    public float getAckTimeoutScale() {
        return this.ackTimeoutScale;
    }

    public int getMaxRetransmit() {
        return this.maxRetransmit;
    }

    public int getNstart() {
        return this.nstart;
    }

    public static Builder builder() {
        return new Builder();
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/ReliabilityLayerParameters$Builder.class */
    public static final class Builder {
        private int ackTimeout;
        private int maxAckTimeout;
        private float ackRandomFactor;
        private float ackTimeoutScale;
        private int maxRetransmit;
        private int nstart;

        private Builder() {
        }

        public Builder applyConfig(Configuration config) {
            this.ackTimeout = config.getTimeAsInt(CoapConfig.ACK_TIMEOUT, TimeUnit.MILLISECONDS);
            this.maxAckTimeout = config.getTimeAsInt(CoapConfig.MAX_ACK_TIMEOUT, TimeUnit.MILLISECONDS);
            this.ackRandomFactor = ((Float) config.get(CoapConfig.ACK_INIT_RANDOM)).floatValue();
            this.ackTimeoutScale = ((Float) config.get(CoapConfig.ACK_TIMEOUT_SCALE)).floatValue();
            this.maxRetransmit = ((Integer) config.get(CoapConfig.MAX_RETRANSMIT)).intValue();
            this.nstart = ((Integer) config.get(CoapConfig.NSTART)).intValue();
            check();
            return this;
        }

        public Builder ackTimeout(int ackTimeout) {
            this.ackTimeout = ackTimeout;
            return this;
        }

        public Builder maxAckTimeout(int maxAckTimeout) {
            this.maxAckTimeout = maxAckTimeout;
            return this;
        }

        public Builder ackRandomFactor(float ackRandomFactor) {
            this.ackRandomFactor = ackRandomFactor;
            return this;
        }

        public Builder ackTimeoutScale(float ackTimeoutScale) {
            this.ackTimeoutScale = ackTimeoutScale;
            return this;
        }

        public Builder maxRetransmit(int maxRetransmit) {
            this.maxRetransmit = maxRetransmit;
            return this;
        }

        public Builder nstart(int nstart) {
            this.nstart = nstart;
            return this;
        }

        public ReliabilityLayerParameters build() {
            check();
            return new ReliabilityLayerParameters(this.ackTimeout, this.maxAckTimeout, this.ackRandomFactor, this.ackTimeoutScale, this.maxRetransmit, this.nstart);
        }

        private void check() {
            if (this.maxAckTimeout < this.ackTimeout) {
                throw new IllegalStateException("Maximum ack timeout " + this.maxAckTimeout + "ms must not be less than ack timeout " + this.ackTimeout + "ms!");
            }
            if (1 > this.maxRetransmit) {
                throw new IllegalStateException("Maxium retransmit " + this.maxRetransmit + " must not be less than 1!");
            }
            if (1 > this.nstart) {
                throw new IllegalStateException("Nstart " + this.nstart + " must not be less than 1!");
            }
            if (1.0d > this.ackRandomFactor) {
                throw new IllegalStateException("Ack random factor " + this.ackRandomFactor + " must not be less than 1.0!");
            }
            if (1.0d > this.ackTimeoutScale) {
                throw new IllegalStateException("Ack scale factor " + this.ackTimeoutScale + " must not be less than 1.0!");
            }
        }
    }
}
