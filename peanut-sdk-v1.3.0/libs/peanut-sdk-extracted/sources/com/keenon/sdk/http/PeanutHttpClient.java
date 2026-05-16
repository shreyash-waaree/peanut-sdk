package com.keenon.sdk.http;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutThreadFactory;
import com.keenon.sdk.hedera.model.ICallback;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.http.adapter.HttpObserveRelation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/PeanutHttpClient.class */
public class PeanutHttpClient {
    private static final String TAG = "[PeanutHttpClient]";
    private static PeanutHttpClient mInstance;
    private OkHttpClient client;
    private HashMap<String, HttpObserveRelation> httpObserverMap = new HashMap<>();

    private PeanutHttpClient() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutHttpClient][new]");
        this.client = OkHttpFactory.getInstance().getClient();
    }

    public static PeanutHttpClient getInstance() {
        if (mInstance == null) {
            synchronized (PeanutHttpClient.class) {
                if (mInstance == null) {
                    mInstance = new PeanutHttpClient();
                }
            }
        }
        return mInstance;
    }

    public void send(HttpRequestBody body, ICallback callback) {
        Request request = buildRequest(body);
        if (body.getMethod() == RequestEnum.OBSERVER && body.getInterval() > 0) {
            observe(request, callback, body.getInterval());
        } else {
            this.client.newCall(request).enqueue(new BaseHttpHandler(callback));
        }
    }

    private Request buildRequest(HttpRequestBody body) {
        RequestBody requestBody;
        Request request = null;
        if (body.getMethod() == RequestEnum.GET) {
            request = new Request.Builder().url(body.getUri()).get().build();
        } else if (body.getMethod() == RequestEnum.POST) {
            if (body.getParams() == null) {
                requestBody = RequestBody.create((MediaType) null, new byte[0]);
            } else {
                requestBody = RequestBody.create(OkHttpFactory.JSON, body.getParams());
            }
            request = new Request.Builder().url(body.getUri()).post(requestBody).build();
        } else if (body.getMethod() == RequestEnum.OBSERVER) {
            request = new Request.Builder().url(body.getUri()).get().build();
        }
        return request;
    }

    private void observe(Request request, ICallback callback, long interval) {
        if (request == null || callback == null) {
            LogUtils.e(PeanutConstants.TAG_SDK, "[PeanutHttpClient][observer][null request or callback]");
            return;
        }
        String tag = request.url().encodedPath();
        HttpObserveRelation previousRelation = this.httpObserverMap.get(tag);
        if (previousRelation != null && !previousRelation.isCanceled()) {
            LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutHttpClient][observer][poll: " + tag + "]");
            previousRelation.longPoll();
            dumpObserveRelation();
        } else {
            ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new PeanutThreadFactory("HttpObserve(poll)#"));
            HttpObserveRelation relation = new HttpObserveRelation(this.client, request, callback, interval, scheduledThreadPoolExecutor);
            this.httpObserverMap.put(tag, relation);
            LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutHttpClient][observer][new: " + tag + "]");
            dumpObserveRelation();
        }
    }

    public void cancelObserver(String tag) {
        if (this.httpObserverMap.get(tag) != null) {
            this.httpObserverMap.get(tag).proactiveCancel();
            this.httpObserverMap.remove(tag);
        }
    }

    private void dumpObserveRelation() {
        if (this.httpObserverMap.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, HttpObserveRelation> entry : this.httpObserverMap.entrySet()) {
            sb.append(String.format("%-30s, %s", entry.getKey(), Boolean.valueOf(entry.getValue().isCanceled())));
            sb.append("\r\n");
        }
        LogUtils.d(PeanutConstants.TAG_SDK, "[PeanutHttpClient][dump][\n" + ((Object) sb) + "]");
    }

    public void handlePushEvent(String tag, String response) {
        if (tag.equals("update/event")) {
            tag = "/upgrade/status";
        }
        if (this.httpObserverMap.get(tag) != null) {
            LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutHttpClient][handlePushEvent][tag: " + tag + "][" + response + "]");
            this.httpObserverMap.get(tag).onResponse(response);
        }
    }
}
