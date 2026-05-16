package org.eclipse.californium.core.network.stack.congestioncontrol;

import java.net.InetSocketAddress;
import org.eclipse.californium.core.network.stack.CongestionControlLayer;
import org.eclipse.californium.core.network.stack.RemoteEndpoint;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/congestioncontrol/PeakhopperRto.class */
public class PeakhopperRto extends CongestionControlLayer {
    public PeakhopperRto(String tag, Configuration config) {
        super(tag, config);
    }

    @Override // org.eclipse.californium.core.network.stack.CongestionControlLayer
    protected RemoteEndpoint createRemoteEndpoint(InetSocketAddress remoteSocketAddress) {
        return new PeakhopperRemoteEndoint(remoteSocketAddress, this.defaultReliabilityLayerParameters.getAckTimeout(), this.defaultReliabilityLayerParameters.getNstart());
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/congestioncontrol/PeakhopperRto$PeakhopperRemoteEndoint.class */
    private static class PeakhopperRemoteEndoint extends RemoteEndpoint {
        private float delta;
        private float B_value;
        private static final float F_value = 24.0f;
        private static final float B_max_value = 1.0f;
        private static final float D_value = 0.9583333f;
        private static final int RTT_HISTORY_SIZE = 2;
        private long RTO_min;
        private long RTT_max;
        private long RTT_previous;
        private long[] RTT_sample;
        private int currentRtt;

        private PeakhopperRemoteEndoint(InetSocketAddress remoteAddress, int ackTimeout, int nstart) {
            super(remoteAddress, ackTimeout, nstart, true);
            this.RTT_sample = new long[2];
        }

        private void initializeRTOEstimators(long measuredRTT) {
            addRttValue(measuredRTT);
            long newRTO = (long) (1.75d * measuredRTT);
            updateRTO(newRTO);
        }

        private void updateEstimator(long measuredRTT) {
            addRttValue(measuredRTT);
            this.delta = Math.abs((measuredRTT - this.RTT_previous) / measuredRTT);
            this.B_value = Math.min(Math.max(this.delta * 2.0f, D_value * this.B_value), 1.0f);
            this.RTT_max = Math.max(measuredRTT, this.RTT_previous);
            this.RTO_min = getMaxRtt() + 100;
            long newRTO = (long) Math.max(D_value * getRTO(), (1.0f + this.B_value) * this.RTT_max);
            long newRTO2 = Math.max(Math.max(newRTO, this.RTT_max + 100), this.RTO_min);
            printPeakhopperStats();
            this.RTT_previous = measuredRTT;
            updateRTO(newRTO2);
        }

        @Override // org.eclipse.californium.core.network.stack.RemoteEndpoint
        public synchronized void processRttMeasurement(RemoteEndpoint.RtoType rtoType, long measuredRTT) {
            if (rtoType != RemoteEndpoint.RtoType.STRONG) {
                return;
            }
            if (initialRto()) {
                initializeRTOEstimators(measuredRTT);
            } else {
                updateEstimator(measuredRTT);
            }
        }

        private void addRttValue(long rtt) {
            synchronized (this.RTT_sample) {
                long[] jArr = this.RTT_sample;
                int i = this.currentRtt;
                this.currentRtt = i + 1;
                jArr[i] = rtt;
                if (this.currentRtt >= this.RTT_sample.length) {
                    this.currentRtt = 0;
                }
            }
        }

        private long getMaxRtt() {
            long max = -1;
            synchronized (this.RTT_sample) {
                for (long rtt : this.RTT_sample) {
                    max = Math.max(max, rtt);
                }
            }
            return max;
        }

        private void printPeakhopperStats() {
            PeakhopperRto.LOGGER.trace("Delta: {}, D: {}, B: {}, RTT_max: {}", new Object[]{Float.valueOf(this.delta), Float.valueOf(D_value), Float.valueOf(this.B_value), Long.valueOf(this.RTT_max)});
        }
    }
}
