package com.keenon.sdk.coap.adapter;

import com.keenon.sdk.hedera.model.ICallback;
import org.eclipse.californium.core.coap.Request;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/coap/adapter/IProgressCallBack.class */
public abstract class IProgressCallBack<T> implements ICallback<T> {
    public void readyToSend(Request request) {
    }

    public void progress(int percent) {
    }
}
