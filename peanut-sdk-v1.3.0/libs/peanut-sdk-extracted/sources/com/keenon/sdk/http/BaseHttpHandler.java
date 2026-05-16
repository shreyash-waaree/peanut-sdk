package com.keenon.sdk.http;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.model.ApiCode;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.ICallback;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/BaseHttpHandler.class */
public class BaseHttpHandler implements Callback {
    private static final String TAG = "[BaseHttpHandler]";
    private final ICallback callback;

    public BaseHttpHandler(ICallback callback) {
        this.callback = callback;
    }

    public void onFailure(Call call, IOException e) {
        LogUtils.d(TAG, "[onFailure][" + e.toString() + "]");
        if (this.callback != null) {
            ApiError error = new ApiError();
            error.setCode(PeanutError.getExceptionCode(e));
            error.setMsg(e.toString());
            error.setTag(call.request().url().encodedPath());
            this.callback.onFail(error);
        }
    }

    public void onResponse(Call call, Response response) throws IOException {
        try {
            if (response.isSuccessful()) {
                String body = null;
                try {
                    body = response.body().string();
                    LogUtils.d(TAG, "[onResponse][" + body + "]");
                } catch (Exception e) {
                    if (this.callback != null) {
                        ApiError error = new ApiError(ApiCode.HTTP_RESPONSE_INVALID);
                        error.setTag(call.request().url().encodedPath());
                        this.callback.onFail(error);
                        return;
                    }
                }
                if (this.callback != null) {
                    this.callback.onSuccess(body);
                }
            } else if (this.callback != null) {
                ApiError error2 = new ApiError();
                error2.setTag(call.request().url().encodedPath());
                error2.setCode(response.code());
                error2.setMsg(response.message());
                this.callback.onFail(error2);
            }
        } catch (Exception e2) {
            LogUtils.e(PeanutConstants.TAG_SDK, TAG + e2.toString());
        }
    }
}
