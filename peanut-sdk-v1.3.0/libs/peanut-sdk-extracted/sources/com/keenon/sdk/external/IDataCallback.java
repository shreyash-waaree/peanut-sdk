package com.keenon.sdk.external;

import com.keenon.sdk.hedera.model.ApiError;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/external/IDataCallback.class */
public interface IDataCallback {
    void success(String str);

    void error(ApiError apiError);
}
