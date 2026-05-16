package com.keenon.sdk.hedera.model;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/model/ICallback.class */
public interface ICallback<T> {
    void onSuccess(T t);

    void onFail(ApiError apiError);
}
