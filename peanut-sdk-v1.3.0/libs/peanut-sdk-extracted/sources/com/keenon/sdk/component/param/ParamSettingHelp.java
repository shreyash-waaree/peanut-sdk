package com.keenon.sdk.component.param;

import com.keenon.sdk.api.ParamUploadApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IDataCallback2;
import com.keenon.sdk.hedera.model.ApiError;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/param/ParamSettingHelp.class */
public class ParamSettingHelp {
    public void checkParam(final String paramKey, final IDataCallback2<Boolean> callback) {
        new ParamUploadApi().send(new IDataCallback() { // from class: com.keenon.sdk.component.param.ParamSettingHelp.1
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                if (result != null) {
                    callback.success(Boolean.valueOf(result.contains(paramKey)));
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                callback.error(error);
            }
        });
    }
}
