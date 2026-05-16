package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
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
import java.math.BigDecimal;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleStatusApi.class */
@LinkAdapter
@CoapCommond(path = "/schedule/status")
public class ScheduleStatusApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.ScheduleStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[ScheduleStatusApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[ScheduleStatusApi][onSuccess json: " + jsonResult + "]");
            if (ScheduleStatusApi.this.callBack == null) {
                return;
            }
            if (ScheduleStatusApi.this.requestEnum == RequestEnum.GET) {
                ScheduleStatusApi.this.callBack.success(jsonResult);
            } else if (ScheduleStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (ScheduleStatusApi.this.callBack == null) {
                return;
            }
            if (ScheduleStatusApi.this.requestEnum == RequestEnum.GET) {
                ScheduleStatusApi.this.callBack.error(error);
            } else if (ScheduleStatusApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiError(this, error);
            }
        }
    };

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

    public void cancel() {
        SenderManager.getInstance().cancel(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleStatusApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleStatusApi$Bean$DataBean.class */
        public class DataBean {
            private int status;
            private String msg;
            private DataBeanInner data;

            public DataBean() {
            }

            public int getStatus() {
                return this.status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public String getMsg() {
                return this.msg;
            }

            public void setMsg(String msg) {
                this.msg = msg;
            }

            public DataBeanInner getData() {
                return this.data;
            }

            public void setData(DataBeanInner data) {
                this.data = data;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleStatusApi$Bean$DataBean$DataBeanInner.class */
            public class DataBeanInner {
                private LocalBean local;
                private OtherBean other;
                private FromBean from;
                private ToBean to;
                private double distance;
                private double time;

                public DataBeanInner() {
                }

                public LocalBean getLocal() {
                    return this.local;
                }

                public void setLocal(LocalBean local) {
                    this.local = local;
                }

                public OtherBean getOther() {
                    return this.other;
                }

                public void setOther(OtherBean other) {
                    this.other = other;
                }

                public FromBean getFrom() {
                    return this.from;
                }

                public void setFrom(FromBean from) {
                    this.from = from;
                }

                public ToBean getTo() {
                    return this.to;
                }

                public void setTo(ToBean to) {
                    this.to = to;
                }

                public double getDistance() {
                    return new BigDecimal(this.distance).setScale(1, 4).doubleValue();
                }

                public void setDistance(double distance) {
                    this.distance = distance;
                }

                public double getTime() {
                    return new BigDecimal(this.time).setScale(1, 4).doubleValue();
                }

                public void setTime(double time) {
                    this.time = time;
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleStatusApi$Bean$DataBean$DataBeanInner$LocalBean.class */
                public class LocalBean {
                    private int id;
                    private double x;
                    private double y;

                    public LocalBean() {
                    }

                    public int getId() {
                        return this.id;
                    }

                    public void setId(int id) {
                        this.id = id;
                    }

                    public double getX() {
                        return new BigDecimal(this.x).setScale(1, 4).floatValue();
                    }

                    public void setX(double x) {
                        this.x = x;
                    }

                    public double getY() {
                        BigDecimal b = new BigDecimal(this.y);
                        return b.setScale(1, 4).floatValue();
                    }

                    public void setY(double y) {
                        this.y = y;
                    }
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleStatusApi$Bean$DataBean$DataBeanInner$OtherBean.class */
                public class OtherBean {
                    private int id;
                    private int status;
                    private double x;
                    private double y;

                    public OtherBean() {
                    }

                    public int getId() {
                        return this.id;
                    }

                    public void setId(int id) {
                        this.id = id;
                    }

                    public int getStatus() {
                        return this.status;
                    }

                    public void setStatus(int status) {
                        this.status = status;
                    }

                    public double getX() {
                        return new BigDecimal(this.x).setScale(1, 4).floatValue();
                    }

                    public void setX(double x) {
                        this.x = x;
                    }

                    public double getY() {
                        return new BigDecimal(this.y).setScale(1, 4).floatValue();
                    }

                    public void setY(double y) {
                        this.y = y;
                    }
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleStatusApi$Bean$DataBean$DataBeanInner$FromBean.class */
                public class FromBean {
                    private int id;
                    private double x;
                    private double y;

                    public FromBean() {
                    }

                    public int getId() {
                        return this.id;
                    }

                    public void setId(int id) {
                        this.id = id;
                    }

                    public double getX() {
                        return this.x;
                    }

                    public void setX(double x) {
                        this.x = x;
                    }

                    public double getY() {
                        return this.y;
                    }

                    public void setY(double y) {
                        this.y = y;
                    }
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/ScheduleStatusApi$Bean$DataBean$DataBeanInner$ToBean.class */
                public class ToBean {
                    private int id;
                    private double x;
                    private double y;

                    public ToBean() {
                    }

                    public int getId() {
                        return this.id;
                    }

                    public void setId(int id) {
                        this.id = id;
                    }

                    public double getX() {
                        return this.x;
                    }

                    public void setX(double x) {
                        this.x = x;
                    }

                    public double getY() {
                        return this.y;
                    }

                    public void setY(double y) {
                        this.y = y;
                    }
                }
            }
        }
    }
}
