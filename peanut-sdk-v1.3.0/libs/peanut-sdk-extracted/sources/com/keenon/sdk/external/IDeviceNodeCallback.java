package com.keenon.sdk.external;

import com.keenon.sdk.hedera.model.ApiError;
import java.util.Map;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/external/IDeviceNodeCallback.class */
public interface IDeviceNodeCallback {
    void success(String str, Map<String, String> map);

    void error(ApiError apiError);
}
