package com.keenon.sdk.hedera;

import com.keenon.common.external.iPing;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/DefaultPing.class */
public class DefaultPing extends iPing {
    public DefaultPing(long timeout, iPing.Listener listener) {
        super(timeout, listener);
    }

    @Override // com.keenon.common.external.iPing
    public boolean isTimeout() {
        return false;
    }

    @Override // com.keenon.common.external.iPing
    public void start() {
    }

    @Override // com.keenon.common.external.iPing
    public void stop() {
    }

    @Override // com.keenon.common.external.iPing
    public void active() {
    }
}
