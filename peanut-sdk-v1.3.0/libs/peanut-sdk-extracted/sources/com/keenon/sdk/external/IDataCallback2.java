package com.keenon.sdk.external;

import com.keenon.sdk.hedera.model.ApiError;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/external/IDataCallback2.class */
public interface IDataCallback2<T> {
    void success(T t);

    void error(ApiError apiError);
}
