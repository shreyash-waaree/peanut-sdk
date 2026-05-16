package com.keenon.sdk.coap;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.utils.Dispatcher;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.PeanutMainThreadExecutor;
import com.keenon.sdk.coap.adapter.IProgressCallBack;
import com.keenon.sdk.hedera.model.ICallback;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.WebLink;
import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.ResponseTimeout;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.elements.util.ExecutorsUtil;
import org.eclipse.californium.elements.util.NamedThreadFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/coap/PeanutCoapClient.class */
public class PeanutCoapClient {
    public static final String TAG = "[PeanutCoapClient]";
    public static final int DEFAULT_BLOCK_SIZE = 1024;
    private static final int INIT_THREAD_COUNT = Runtime.getRuntime().availableProcessors() + 1;
    private static PeanutCoapClient mInstance;
    private CoapClient mClient;
    private Dispatcher mDispatcher;
    private ScheduledExecutorService mExecutorService;
    private PeanutMainThreadExecutor mainThreadExecutor;
    private HashMap<String, CoapObserveRelation> mObserverMap = new HashMap<>();
    private volatile boolean isFinish;

    private PeanutCoapClient() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutCoapClient][new]");
        ExecutorService mExecutor = ExecutorsUtil.newScheduledThreadPool(INIT_THREAD_COUNT, new NamedThreadFactory("CoapClient1#"));
        this.mExecutorService = ExecutorsUtil.newScheduledThreadPool(INIT_THREAD_COUNT, new NamedThreadFactory("Timeout#"));
        Configuration config = Configuration.createStandardWithoutFile().set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 5242880).set(CoapConfig.PREFERRED_BLOCK_SIZE, 1024);
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConfiguration(config);
        CoapEndpoint endpoint = builder.build();
        this.mClient = new CoapClient();
        this.mClient.setEndpoint(endpoint);
        this.mClient.setTimeout(Long.valueOf(PeanutConfig.getRequestTimeout()));
        this.mClient.setExecutors(mExecutor, ExecutorsUtil.newDefaultSecondaryScheduler("CoapClient2#"), false);
        this.mainThreadExecutor = new PeanutMainThreadExecutor();
        this.mDispatcher = new Dispatcher(null);
        this.isFinish = false;
    }

    private void discover() throws ConnectorException, IOException {
        this.mClient.setURI(PeanutConfig.getLinkUrl());
        Set<WebLink> links = this.mClient.discover();
        if (links == null || links.size() == 0) {
            LogUtils.w(PeanutConstants.TAG_SDK, "[PeanutCoapClient][discover][未发现资源]");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (WebLink link : links) {
            Object[] objArr = new Object[2];
            objArr[0] = link.getURI();
            objArr[1] = link.getAttributes().hasObservable() ? "ob" : "";
            sb.append(String.format("%-30s, %s", objArr));
            sb.append("\r\n");
        }
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutCoapClient][discover][links]\r\n" + ((Object) sb));
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutCoapClient][discover][发现资源数: " + links.size() + "个]");
    }

    public static PeanutCoapClient getInstance() {
        if (mInstance == null) {
            synchronized (PeanutCoapClient.class) {
                if (mInstance == null) {
                    mInstance = new PeanutCoapClient();
                }
            }
        }
        return mInstance;
    }

    public boolean ping(String url) {
        this.mClient.setURI(url);
        return this.mClient.ping();
    }

    public void release() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutCoapClient][release]");
        cancelCoapObserver();
        if (this.mClient != null) {
            this.mClient.shutdown();
        }
        this.mainThreadExecutor.release();
        mInstance = null;
    }

    public void request(RequestBody body, ICallback callBack) {
        switch (body.getType()) {
            case POST:
            case GET:
            case GET_RAW:
                sendRequest(body, callBack);
                break;
            case GET_LARGE:
                getLarge(body, (IProgressCallBack) callBack);
                break;
            case POST_LARGE:
            case FILE:
                postLarge(body, (IProgressCallBack) callBack);
                break;
            case OBSERVER:
            case OBSERVER_RAW:
                observe(body, callBack);
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Request prepareRequest(RequestBody body) {
        Request request;
        switch (body.getType()) {
            case POST:
                request = Request.newPost();
                request.setPayload(body.getRequestParams());
                request.getOptions().setAccept(50);
                request.getOptions().setContentFormat(50);
                break;
            case GET:
            case GET_RAW:
            default:
                request = Request.newGet();
                request.getOptions().setAccept(50);
                request.getOptions().setContentFormat(50);
                break;
            case GET_LARGE:
                request = Request.newGet();
                request.getOptions().setBlock2(new BlockOption(BlockOption.size2Szx(1024), false, 0));
                request.getOptions().setContentFormat(-1);
                break;
            case POST_LARGE:
            case FILE:
                request = Request.newPost();
                request.setPayload(body.getRequestParams());
                request.getOptions().setBlock2(new BlockOption(BlockOption.size2Szx(1024), false, 0));
                request.getOptions().setContentFormat(-1);
                break;
            case OBSERVER:
            case OBSERVER_RAW:
                request = Request.newGet();
                request.setObserve();
                request.getOptions().setAccept(50);
                request.getOptions().setContentFormat(50);
                break;
        }
        request.setConfirmable(body.getCoapType() != CoAP.Type.NON);
        request.setURI(body.getUri());
        request.addMessageObserver(new ResponseTimeout(request, PeanutConfig.getRequestTimeout(), this.mExecutorService));
        return request;
    }

    private boolean needSequential(RequestBody body) {
        return (!body.getPath().startsWith("/navigation") || body.getPath().startsWith("/navigation/status") || body.getPath().startsWith("/navigation/dest_poses") || body.getPath().startsWith("/navigation/disinfectionInfo")) ? false : true;
    }

    private void sendRequest(RequestBody body, ICallback callback) {
        if (needSequential(body)) {
            syncRequest(body, callback);
        } else {
            asyncRequest(body, callback);
        }
    }

    private void observe(RequestBody body, ICallback callback) {
        asyncObserver(body, callback);
    }

    private void syncRequest(final RequestBody body, final ICallback callback) {
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutCoapClient][syncRequest][队列的请求有:" + this.mDispatcher.queuedCallsCount() + "个, 当前的请求是:" + body.getTag() + "]");
        this.mDispatcher.enqueue(new Dispatcher.NodeCall(body.getTag(), "", false, this.mDispatcher) { // from class: com.keenon.sdk.coap.PeanutCoapClient.1
            final Dispatcher.NodeCall requestNodeCall = this;

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                String tag = body.getTag();
                LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutCoapClient][syncRequest][real execute][" + tag + "]");
                Request request = PeanutCoapClient.this.prepareRequest(body);
                BaseCoapHandler baseCoapHandler = new BaseCoapHandler(body, request, callback);
                try {
                    try {
                        CoapResponse response = PeanutCoapClient.this.mClient.advanced(request);
                        baseCoapHandler.handleCallback(response);
                        if (PeanutCoapClient.this.mDispatcher != null) {
                            PeanutCoapClient.this.mDispatcher.executeNextNodeCall(this.requestNodeCall);
                        }
                    } catch (Exception e) {
                        baseCoapHandler.handleCallback(null);
                        LogUtils.e(PeanutConstants.TAG_SDK, "[PeanutCoapClient][syncRequest] [CallBack is Null]" + e.getMessage());
                        e.printStackTrace();
                        if (PeanutCoapClient.this.mDispatcher != null) {
                            PeanutCoapClient.this.mDispatcher.executeNextNodeCall(this.requestNodeCall);
                        }
                    }
                } catch (Throwable th) {
                    if (PeanutCoapClient.this.mDispatcher != null) {
                        PeanutCoapClient.this.mDispatcher.executeNextNodeCall(this.requestNodeCall);
                    }
                    throw th;
                }
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return PeanutCoapClient.this.isFinish;
            }
        });
    }

    private void syncObserve(final RequestBody body, final ICallback callback) {
        LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutCoapClient][syncObserve][队列的请求有:" + this.mDispatcher.queuedCallsCount() + "个, 当前的请求是:" + body.getTag() + "]");
        this.mDispatcher.enqueue(new Dispatcher.NodeCall(body.getTag(), "", false, this.mDispatcher) { // from class: com.keenon.sdk.coap.PeanutCoapClient.2
            final Dispatcher.NodeCall requestNodeCall = this;

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected void execute() {
                String tag = body.getTag();
                LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutCoapClient][syncObserve][real execute][" + tag + "]");
                try {
                    try {
                        Request request = PeanutCoapClient.this.prepareRequest(body);
                        CoapObserveRelation relation = PeanutCoapClient.this.mClient.observeAndWait(request, PeanutCoapClient.this.new MyResponseHandler(body, request, callback));
                        if (relation.isCanceled()) {
                            LogUtils.w(PeanutConstants.TAG_SDK, "[PeanutCoapClient][syncObserve][同步订阅失败]");
                        }
                        PeanutCoapClient.this.mObserverMap.put(tag, relation);
                        PeanutCoapClient.this.dumpObserveRelation();
                        if (PeanutCoapClient.this.mDispatcher != null) {
                            PeanutCoapClient.this.mDispatcher.executeNextNodeCall(this.requestNodeCall);
                        }
                    } catch (IOException | ConnectorException e) {
                        e.printStackTrace();
                        if (PeanutCoapClient.this.mDispatcher != null) {
                            PeanutCoapClient.this.mDispatcher.executeNextNodeCall(this.requestNodeCall);
                        }
                    }
                } catch (Throwable th) {
                    if (PeanutCoapClient.this.mDispatcher != null) {
                        PeanutCoapClient.this.mDispatcher.executeNextNodeCall(this.requestNodeCall);
                    }
                    throw th;
                }
            }

            @Override // com.keenon.common.utils.Dispatcher.NodeCall
            protected boolean interceptExecute() {
                return PeanutCoapClient.this.isFinish;
            }
        });
    }

    private void asyncRequest(RequestBody body, ICallback callback) {
        Request request = prepareRequest(body);
        BaseCoapHandler baseCoapHandler = new BaseCoapHandler(body, request, callback);
        try {
            this.mClient.advanced(baseCoapHandler, request);
        } catch (Exception e) {
            baseCoapHandler.handleCallback(null);
            LogUtils.e(PeanutConstants.TAG_SDK, "[PeanutCoapClient][syncRequest] [CallBack is Null]" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void asyncObserver(RequestBody body, ICallback callback) {
        if (this.mClient == null || body == null) {
            return;
        }
        CoapObserveRelation previousRelation = getCoapObserveRelation(body.getTag());
        if (previousRelation != null && !previousRelation.isCanceled()) {
            LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutCoapClient][asyncObserver][reregister: " + body.getTag() + "]");
            previousRelation.reregister();
            dumpObserveRelation();
        } else {
            Request request = prepareRequest(body);
            CoapObserveRelation relation = this.mClient.observe(request, new MyResponseHandler(body, request, callback));
            this.mObserverMap.put(body.getTag(), relation);
            LogUtils.i(PeanutConstants.TAG_SDK, "[PeanutCoapClient][asyncObserver][new: " + body.getTag() + "]");
            dumpObserveRelation();
        }
    }

    public void getLarge(RequestBody body, final IProgressCallBack callback) {
        Request request = prepareRequest(body);
        request.addMessageObserver(new BlockResponseHandler(request) { // from class: com.keenon.sdk.coap.PeanutCoapClient.3
            @Override // com.keenon.sdk.coap.PeanutCoapClient.BlockResponseHandler, org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
            public void onSent(boolean retransmission) {
                super.onSent(retransmission);
                PeanutCoapClient.this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.coap.PeanutCoapClient.3.1
                    @Override // java.lang.Runnable
                    public void run() {
                        int num = 1024 * getSentBlockCount();
                        callback.progress(num);
                    }
                });
            }
        });
        this.mClient.advanced(new BaseCoapHandler(body, request, callback), request);
    }

    private void postLarge(final RequestBody body, final IProgressCallBack callback) {
        final Request request = prepareRequest(body);
        request.addMessageObserver(new BlockResponseHandler(request) { // from class: com.keenon.sdk.coap.PeanutCoapClient.4
            @Override // com.keenon.sdk.coap.PeanutCoapClient.BlockResponseHandler, org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
            public void onSent(boolean retransmission) {
                super.onSent(retransmission);
                PeanutCoapClient.this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.coap.PeanutCoapClient.4.1
                    @Override // java.lang.Runnable
                    public void run() {
                        float num = 1024 * getSentBlockCount();
                        float percent = (num * 100.0f) / body.getRequestParams().length;
                        callback.progress(Math.min(Math.round(percent), 100));
                    }
                });
            }

            @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
            public void onReadyToSend() {
                PeanutCoapClient.this.mainThreadExecutor.execute(new Runnable() { // from class: com.keenon.sdk.coap.PeanutCoapClient.4.2
                    @Override // java.lang.Runnable
                    public void run() {
                        callback.readyToSend(request);
                    }
                });
            }
        });
        this.mClient.advanced(new BaseCoapHandler(body, request, callback), request);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpObserveRelation() {
        if (this.mObserverMap.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, CoapObserveRelation> entry : this.mObserverMap.entrySet()) {
            sb.append(String.format("%-30s, %s", entry.getKey(), Boolean.valueOf(entry.getValue().isCanceled())));
            sb.append("\r\n");
        }
        LogUtils.d(PeanutConstants.TAG_SDK, "[PeanutCoapClient][dump][\n" + ((Object) sb) + "]");
    }

    public CoapObserveRelation getCoapObserveRelation(String tag) {
        return this.mObserverMap.get(tag);
    }

    public void cancelCoapObserver(String tag) {
        if (this.mObserverMap.get(tag) != null) {
            this.mObserverMap.get(tag).proactiveCancel();
            this.mObserverMap.remove(tag);
        }
    }

    public void cancelCoapObserver() {
        for (Map.Entry<String, CoapObserveRelation> entry : this.mObserverMap.entrySet()) {
            entry.getValue().proactiveCancel();
        }
        this.mObserverMap.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reRegister(RequestBody body, ICallback callback) {
        if (body == null || callback == null) {
            LogUtils.d(PeanutConstants.TAG_SDK, "[PeanutCoapClient][reRegister][null]");
            return;
        }
        LogUtils.d(PeanutConstants.TAG_SDK, "[PeanutCoapClient][reRegister][topic : ]" + body.getTag());
        Set<String> strings = this.mObserverMap.keySet();
        for (String string : strings) {
            if (string.equals(body.getTag())) {
                if (needSequential(body)) {
                    syncObserve(body, callback);
                } else {
                    asyncObserver(body, callback);
                }
            }
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/coap/PeanutCoapClient$MyResponseHandler.class */
    public class MyResponseHandler extends BaseCoapHandler {
        private RequestBody body;
        private ICallback callback;

        public MyResponseHandler(RequestBody body, Request request, ICallback callback) {
            super(body, request, callback);
            this.body = body;
            this.callback = callback;
        }

        @Override // com.keenon.sdk.coap.BaseCoapHandler, org.eclipse.californium.core.CoapHandler
        public void onError() {
            super.onError();
            LogUtils.w(PeanutConstants.TAG_SDK, "[PeanutCoapClient][onError][订阅异常][重新订阅][" + this.body.getTag() + "]");
            PeanutCoapClient.this.reRegister(this.body, this.callback);
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/coap/PeanutCoapClient$BlockResponseHandler.class */
    class BlockResponseHandler extends MessageObserverAdapter {
        private Request request;
        private final AtomicInteger sentCounter = new AtomicInteger();

        public BlockResponseHandler(Request request) {
            this.request = request;
        }

        public int getSentBlockCount() {
            return this.sentCounter.get();
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onRetransmission() {
            this.sentCounter.decrementAndGet();
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onSent(boolean retransmission) {
            this.sentCounter.incrementAndGet();
        }
    }
}
