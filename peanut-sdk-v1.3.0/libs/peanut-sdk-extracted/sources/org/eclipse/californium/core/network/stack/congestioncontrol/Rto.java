package org.eclipse.californium.core.network.stack.congestioncontrol;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/congestioncontrol/Rto.class */
public class Rto {
    private static final double ALPHA = 0.25d;
    private static final double BETA = 0.125d;
    private static final int G = 100;
    private final int kFactor;
    private boolean init;
    private long rto;
    private long rtt = 0;
    private long rttVar = 0;

    public Rto(int kFactor, long ackTimeout) {
        this.kFactor = kFactor;
        this.rto = ackTimeout;
    }

    public long apply(long measuredRTT) {
        long RTTVAR;
        long RTT;
        if (this.init) {
            RTTVAR = Math.round((0.875d * this.rttVar) + (BETA * Math.abs(this.rtt - measuredRTT)));
            RTT = Math.round((0.75d * this.rtt) + (ALPHA * measuredRTT));
        } else {
            this.init = true;
            RTTVAR = measuredRTT / 2;
            RTT = measuredRTT;
        }
        this.rtt = RTT;
        this.rttVar = RTTVAR;
        this.rto = RTT + Math.max(100L, ((long) this.kFactor) * RTTVAR);
        return this.rto;
    }

    public long getRto() {
        return this.rto;
    }

    public long getRtt() {
        return this.rtt;
    }

    public long getRttVar() {
        return this.rttVar;
    }
}
