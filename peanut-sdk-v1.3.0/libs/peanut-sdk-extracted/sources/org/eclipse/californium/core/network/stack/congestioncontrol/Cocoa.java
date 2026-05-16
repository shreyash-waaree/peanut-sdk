package org.eclipse.californium.core.network.stack.congestioncontrol;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.core.network.stack.CongestionControlLayer;
import org.eclipse.californium.core.network.stack.RemoteEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.ClockUtil;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/congestioncontrol/Cocoa.class */
public class Cocoa extends CongestionControlLayer {
    private static final int KSTRONG = 4;
    private static final int KWEAK = 1;
    private static final double STRONGWEIGHTING = 0.5d;
    private static final double WEAKWEIGHTING = 0.25d;
    private static final long LOWERVBFLIMIT = 1000;
    private static final long UPPERVBFLIMIT = 3000;
    private static final float VBFLOW = 3.0f;
    private static final float VBFHIGH = 1.5f;
    private final boolean strong;

    public Cocoa(String tag, Configuration config, boolean strong) {
        super(tag, config);
        this.strong = strong;
        setDithering(true);
    }

    @Override // org.eclipse.californium.core.network.stack.CongestionControlLayer
    protected RemoteEndpoint createRemoteEndpoint(InetSocketAddress remoteSocketAddress) {
        return new CocoaRemoteEndpoint(remoteSocketAddress, this.defaultReliabilityLayerParameters.getAckTimeout(), this.defaultReliabilityLayerParameters.getNstart(), this.strong);
    }

    @Override // org.eclipse.californium.core.network.stack.CongestionControlLayer
    protected float calculateVBF(long rto, float scale) {
        if (rto > UPPERVBFLIMIT) {
            return VBFHIGH;
        }
        if (rto < LOWERVBFLIMIT) {
            return VBFLOW;
        }
        return scale;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/congestioncontrol/Cocoa$CocoaRemoteEndpoint.class */
    private static class CocoaRemoteEndpoint extends RemoteEndpoint {
        private final boolean onlyStrong;
        private Rto weakRto;
        private Rto strongRto;
        private long nanoTimestamp;

        private CocoaRemoteEndpoint(InetSocketAddress remoteAddress, int ackTimeout, int nstart, boolean strong) {
            super(remoteAddress, ackTimeout, nstart, true);
            this.onlyStrong = strong;
            this.weakRto = new Rto(1, ackTimeout);
            this.strongRto = new Rto(4, ackTimeout);
            this.nanoTimestamp = ClockUtil.nanoRealtime();
        }

        @Override // org.eclipse.californium.core.network.stack.RemoteEndpoint
        public synchronized void processRttMeasurement(RemoteEndpoint.RtoType rtoType, long measuredRTT) {
            long newRto;
            double weighting;
            if (this.onlyStrong && rtoType != RemoteEndpoint.RtoType.STRONG) {
                return;
            }
            switch (rtoType) {
                case WEAK:
                    newRto = this.weakRto.apply(measuredRTT);
                    weighting = 0.25d;
                    break;
                case STRONG:
                    newRto = this.strongRto.apply(measuredRTT);
                    weighting = 0.5d;
                    break;
                default:
                    return;
            }
            updateRTO(Math.round((weighting * newRto) + ((1.0d - weighting) * getRTO())));
            this.nanoTimestamp = ClockUtil.nanoRealtime();
        }

        @Override // org.eclipse.californium.core.network.stack.RemoteEndpoint
        public synchronized void checkAging() {
            long overallDifference = getRtoAge(TimeUnit.MILLISECONDS);
            long rto = getRTO();
            while (true) {
                if (rto < Cocoa.LOWERVBFLIMIT && overallDifference > 16 * rto) {
                    overallDifference -= 16 * rto;
                    rto *= 2;
                    updateRTO(rto);
                    this.nanoTimestamp = ClockUtil.nanoRealtime();
                } else if (rto > Cocoa.UPPERVBFLIMIT && overallDifference > 4 * rto) {
                    overallDifference -= 4 * rto;
                    rto = Cocoa.LOWERVBFLIMIT + (rto / 2);
                    updateRTO(rto);
                    this.nanoTimestamp = ClockUtil.nanoRealtime();
                } else {
                    return;
                }
            }
        }

        private long getRtoAge(TimeUnit unit) {
            long nanos = ClockUtil.nanoRealtime() - this.nanoTimestamp;
            return unit.convert(nanos, TimeUnit.NANOSECONDS);
        }
    }
}
