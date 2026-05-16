package org.eclipse.californium.core.network.stack.congestioncontrol;

import java.net.InetSocketAddress;
import org.eclipse.californium.core.network.stack.CongestionControlLayer;
import org.eclipse.californium.core.network.stack.RemoteEndpoint;
import org.eclipse.californium.elements.config.Configuration;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/congestioncontrol/LinuxRto.class */
public class LinuxRto extends CongestionControlLayer {
    public LinuxRto(String tag, Configuration config) {
        super(tag, config);
    }

    @Override // org.eclipse.californium.core.network.stack.CongestionControlLayer
    protected RemoteEndpoint createRemoteEndpoint(InetSocketAddress remoteSocketAddress) {
        return new LinuxRemoteEndpoint(remoteSocketAddress, this.defaultReliabilityLayerParameters.getAckTimeout(), this.defaultReliabilityLayerParameters.getNstart());
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/congestioncontrol/LinuxRto$LinuxRemoteEndpoint.class */
    private static class LinuxRemoteEndpoint extends RemoteEndpoint {
        private long SRTT;
        private long RTTVAR;
        private long mdev;
        private long mdev_max;

        private LinuxRemoteEndpoint(InetSocketAddress remoteAddress, int ackTimeout, int nstart) {
            super(remoteAddress, ackTimeout, nstart, true);
        }

        private void initializeRTOEstimators(long measuredRTT) {
            this.SRTT = measuredRTT;
            this.mdev = measuredRTT / 2;
            this.mdev_max = Math.max(this.mdev, 50L);
            this.RTTVAR = this.mdev_max;
            long newRTO = this.SRTT + (4 * this.RTTVAR);
            printLinuxStats();
            updateRTO(newRTO);
        }

        private void updateEstimator(long measuredRTT) {
            this.SRTT += Math.round(0.125d * (measuredRTT - this.SRTT));
            if (measuredRTT < this.SRTT - this.mdev) {
                this.mdev = Math.round((0.96875d * this.mdev) + (0.03125d * Math.abs(measuredRTT - this.SRTT)));
            } else {
                this.mdev = Math.round(0.75d * this.mdev) + Math.round(0.25d * Math.abs(measuredRTT - this.SRTT));
            }
            if (this.mdev > this.mdev_max) {
                this.mdev_max = this.mdev;
                if (this.mdev_max > this.RTTVAR) {
                    this.RTTVAR = this.mdev_max;
                }
            }
            if (this.mdev_max < this.RTTVAR) {
                this.RTTVAR = Math.round((0.75d * this.RTTVAR) + (0.25d * this.mdev_max));
            }
            this.mdev_max = 50L;
            long newRTO = this.SRTT + (4 * this.RTTVAR);
            printLinuxStats();
            updateRTO(newRTO);
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

        private void printLinuxStats() {
            LinuxRto.LOGGER.trace("SRTT: {}, RTTVAR: {}, mdev: {}, mdev_max: {}", new Object[]{Long.valueOf(this.SRTT), Long.valueOf(this.RTTVAR), Long.valueOf(this.mdev), Long.valueOf(this.mdev_max)});
        }
    }
}
