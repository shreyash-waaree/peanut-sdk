package com.keenon.sdk.hedera.model;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/model/ApiCallback.class */
public class ApiCallback<T> implements ICallback<T> {
    @Override // com.keenon.sdk.hedera.model.ICallback
    public void onSuccess(T result) {
    }

    @Override // com.keenon.sdk.hedera.model.ICallback
    public void onFail(ApiError error) {
    }
}
