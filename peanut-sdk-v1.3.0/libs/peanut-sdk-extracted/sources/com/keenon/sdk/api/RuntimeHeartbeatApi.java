package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHeartbeatApi.class */
@LinkAdapter
@CoapCommond(path = "/runtime/heartbeat")
public class RuntimeHeartbeatApi {
    private IDataCallback callBack;
    private long sync;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RuntimeHeartbeatApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RuntimeHeartbeatApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RuntimeHeartbeatApi][onSuccess json: " + jsonResult + "]");
            if (RuntimeHeartbeatApi.this.callBack == null) {
                return;
            }
            if (RuntimeHeartbeatApi.this.requestEnum == RequestEnum.GET) {
                RuntimeHeartbeatApi.this.callBack.success(jsonResult);
            } else if (RuntimeHeartbeatApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RuntimeHeartbeatApi.this.callBack == null) {
                return;
            }
            if (RuntimeHeartbeatApi.this.requestEnum == RequestEnum.GET) {
                RuntimeHeartbeatApi.this.callBack.error(error);
            } else if (RuntimeHeartbeatApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sync", this.sync);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[RuntimeHeartbeatApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    @RequestType
    public RequestEnum requestType() {
        return this.requestEnum;
    }

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    public void observe(IDataCallback callBack) {
        this.requestEnum = RequestEnum.OBSERVER;
        send(callBack);
    }

    public void sync(IDataCallback callBack, long sync) {
        this.requestEnum = RequestEnum.POST;
        this.sync = sync;
        send(callBack);
    }

    public void cancel() {
        SenderManager.getInstance().cancel(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHeartbeatApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHeartbeatApi$Bean$DataBean.class */
        public class DataBean {
            private int time;
            private int timestamp;
            private Stm32Bean stm32;
            private MotorBean motor;
            private ModeBean mode;
            private SyncBean sync;

            public DataBean() {
            }

            public int getTime() {
                return this.time;
            }

            public void setTime(int time) {
                this.time = time;
            }

            public int getTimestamp() {
                return this.timestamp;
            }

            public void setTimestamp(int timestamp) {
                this.timestamp = timestamp;
            }

            public Stm32Bean getStm32() {
                return this.stm32;
            }

            public void setStm32(Stm32Bean stm32) {
                this.stm32 = stm32;
            }

            public MotorBean getMotor() {
                return this.motor;
            }

            public void setMotor(MotorBean motor) {
                this.motor = motor;
            }

            public ModeBean getMode() {
                return this.mode;
            }

            public void setMode(ModeBean mode) {
                this.mode = mode;
            }

            public SyncBean getSync() {
                return this.sync;
            }

            public void setSync(SyncBean sync) {
                this.sync = sync;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHeartbeatApi$Bean$DataBean$Stm32Bean.class */
            public class Stm32Bean {
                private int status;
                private String desc;

                public Stm32Bean() {
                }

                public int getStatus() {
                    return this.status;
                }

                public void setStatus(int status) {
                    this.status = status;
                }

                public String getDesc() {
                    return this.desc;
                }

                public void setDesc(String desc) {
                    this.desc = desc;
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHeartbeatApi$Bean$DataBean$MotorBean.class */
            public class MotorBean {
                private int status;
                private String desc;

                public MotorBean() {
                }

                public int getStatus() {
                    return this.status;
                }

                public void setStatus(int status) {
                    this.status = status;
                }

                public String getDesc() {
                    return this.desc;
                }

                public void setDesc(String desc) {
                    this.desc = desc;
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHeartbeatApi$Bean$DataBean$ModeBean.class */
            public class ModeBean {
                private int status;
                private String desc;

                public ModeBean() {
                }

                public int getStatus() {
                    return this.status;
                }

                public void setStatus(int status) {
                    this.status = status;
                }

                public String getDesc() {
                    return this.desc;
                }

                public void setDesc(String desc) {
                    this.desc = desc;
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RuntimeHeartbeatApi$Bean$DataBean$SyncBean.class */
            public class SyncBean {
                private int status;
                private String desc;

                public SyncBean() {
                }

                public int getStatus() {
                    return this.status;
                }

                public void setStatus(int status) {
                    this.status = status;
                }

                public String getDesc() {
                    return this.desc;
                }

                public void setDesc(String desc) {
                    this.desc = desc;
                }
            }
        }
    }
}
