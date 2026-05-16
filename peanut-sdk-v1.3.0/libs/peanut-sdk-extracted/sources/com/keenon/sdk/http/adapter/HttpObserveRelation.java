package com.keenon.sdk.http.adapter;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.StringUtils;
import com.keenon.sdk.hedera.model.ApiCode;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.ICallback;
import com.keenon.sdk.http.BaseHttpHandler;
import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/adapter/HttpObserveRelation.class */
public class HttpObserveRelation {
    private static final String TAG = "[HttpObserveRelation]";
    private final ScheduledThreadPoolExecutor scheduler;
    private OkHttpClient client;
    private volatile Call call;
    private volatile Request request;
    private volatile long interval;
    private volatile ICallback callback;
    private final AtomicBoolean requestPending = new AtomicBoolean(true);
    private final AtomicReference<ScheduledFuture<?>> longPollingHandle = new AtomicReference<>();
    private volatile Response current = null;
    private volatile boolean canceled = false;
    private volatile boolean proactiveCancel = false;
    private final Runnable polling = new Runnable() { // from class: com.keenon.sdk.http.adapter.HttpObserveRelation.1
        @Override // java.lang.Runnable
        public void run() {
            HttpObserveRelation.this.longPoll();
        }
    };
    private final long longPollingBackoffMillis = 50;

    public HttpObserveRelation(OkHttpClient client, Request request, ICallback callback, long interval, ScheduledThreadPoolExecutor executor) {
        this.client = client;
        this.request = request;
        this.interval = interval;
        this.call = client.newCall(request);
        this.callback = callback;
        this.scheduler = executor;
        this.requestPending.set(false);
        longPoll();
    }

    public boolean longPoll() {
        if (this.call.isCanceled()) {
            throw new IllegalStateException("observe request already canceled! url " + this.request.url().encodedPath());
        }
        if (isCanceled()) {
            throw new IllegalStateException("observe already canceled!");
        }
        if (this.requestPending.compareAndSet(false, true)) {
            Request refresh = this.request;
            LogUtils.d(TAG, "[polling]");
            this.client.newCall(refresh).enqueue(new ObserveHttpHandler(this, this.callback));
            this.request = refresh;
            return true;
        }
        LogUtils.w(TAG, "[pending]");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendCancelObserve() {
        this.proactiveCancel = false;
        if (!this.call.isCanceled()) {
            this.call.cancel();
        }
    }

    private void cancel() {
        setCanceled(true);
    }

    public void proactiveCancel() {
        cancel();
        this.proactiveCancel = true;
        if (this.requestPending.compareAndSet(false, true)) {
            sendCancelObserve();
        }
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public Response getCurrentResponse() {
        return this.current;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
        if (this.canceled) {
            setLongPollingHandle(null);
        }
    }

    public void onResponse(String response) {
        if (!StringUtils.isEmpty(response) && this.callback != null) {
            this.callback.onSuccess(response);
        }
    }

    public void onResponse(Response response) {
        if (null != response) {
            this.current = response;
            try {
                if (response.isSuccessful()) {
                    String body = null;
                    try {
                        body = response.body().string();
                        LogUtils.d(TAG, "[onResponse][" + body + "]");
                    } catch (Exception e) {
                        if (this.callback != null) {
                            ApiError error = new ApiError(ApiCode.HTTP_RESPONSE_INVALID);
                            error.setTag(response.request().url().encodedPath());
                            this.callback.onFail(error);
                            return;
                        }
                    }
                    if (this.callback != null) {
                        this.callback.onSuccess(body);
                    }
                } else if (this.callback != null) {
                    ApiError error2 = new ApiError();
                    error2.setTag(response.request().url().encodedPath());
                    error2.setCode(response.code());
                    error2.setMsg(response.message());
                    this.callback.onFail(error2);
                }
            } catch (Exception e2) {
                LogUtils.e(PeanutConstants.TAG_SDK, TAG + e2.toString());
            }
        }
        boolean prepareNext = !isCanceled();
        if (prepareNext) {
            prepareLongPolling();
        } else {
            cancel();
        }
    }

    public boolean matchRequest(Request request) {
        return this.request.url().equals(request.url());
    }

    private void setLongPollingHandle(ScheduledFuture<?> longPollingHandle) {
        ScheduledFuture<?> previousHandle = this.longPollingHandle.getAndSet(longPollingHandle);
        if (previousHandle != null) {
            if (previousHandle instanceof Runnable) {
                this.scheduler.remove((Runnable) previousHandle);
            } else {
                previousHandle.cancel(false);
            }
        }
    }

    private void prepareLongPolling() {
        long timeout = this.interval + this.longPollingBackoffMillis;
        ScheduledFuture<?> f = this.scheduler.schedule(this.polling, timeout, TimeUnit.MILLISECONDS);
        setLongPollingHandle(f);
        LogUtils.d(TAG, "[prepareLongPolling for " + timeout + "ms]");
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/adapter/HttpObserveRelation$ObserveHttpHandler.class */
    public class ObserveHttpHandler extends BaseHttpHandler {
        HttpObserveRelation relation;

        public ObserveHttpHandler(HttpObserveRelation relation, ICallback callback) {
            super(callback);
            this.relation = relation;
        }

        @Override // com.keenon.sdk.http.BaseHttpHandler
        public void onFailure(Call call, IOException e) {
            LogUtils.d(HttpObserveRelation.TAG, "[onFailure for " + this.relation.request.url().encodedPath() + "]");
            super.onFailure(call, e);
            next();
        }

        @Override // com.keenon.sdk.http.BaseHttpHandler
        public void onResponse(Call call, Response response) throws IOException {
            this.relation.onResponse(response);
            HttpObserveRelation.this.current = response;
            next();
        }

        private void next() {
            if (HttpObserveRelation.this.proactiveCancel) {
                HttpObserveRelation.this.sendCancelObserve();
            } else {
                HttpObserveRelation.this.requestPending.set(false);
            }
        }
    }
}
