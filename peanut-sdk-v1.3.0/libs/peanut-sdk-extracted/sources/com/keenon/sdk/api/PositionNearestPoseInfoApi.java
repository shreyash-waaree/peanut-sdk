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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/PositionNearestPoseInfoApi.class */
@LinkAdapter
@CoapCommond(path = "/position/nearestPoseInfo")
public class PositionNearestPoseInfoApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.PositionNearestPoseInfoApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[PositionNearestPoseInfoApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[PositionNearestPoseInfoApi][onSuccess json: " + jsonResult + "]");
            if (PositionNearestPoseInfoApi.this.callBack == null) {
                return;
            }
            if (PositionNearestPoseInfoApi.this.requestEnum == RequestEnum.GET) {
                PositionNearestPoseInfoApi.this.callBack.success(jsonResult);
            } else if (PositionNearestPoseInfoApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (PositionNearestPoseInfoApi.this.callBack == null) {
                return;
            }
            if (PositionNearestPoseInfoApi.this.requestEnum == RequestEnum.GET) {
                PositionNearestPoseInfoApi.this.callBack.error(error);
            } else if (PositionNearestPoseInfoApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/PositionNearestPoseInfoApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/PositionNearestPoseInfoApi$Bean$DataBean.class */
        public class DataBean {
            private double distance;
            private PosBean pose;

            public DataBean() {
            }

            public double getDistance() {
                return this.distance;
            }

            public void setDistance(double distance) {
                this.distance = distance;
            }

            public PosBean getPose() {
                return this.pose;
            }

            public void setPose(PosBean pose) {
                this.pose = pose;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/PositionNearestPoseInfoApi$Bean$DataBean$PosBean.class */
            public class PosBean {
                private double x;
                private double y;
                private int id;
                private String name;
                private String type;
                private double angle;
                private int floor;

                public PosBean() {
                }

                public int getFloor() {
                    return this.floor;
                }

                public void setFloor(int floor) {
                    this.floor = floor;
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

                public int getId() {
                    return this.id;
                }

                public void setId(int id) {
                    this.id = id;
                }

                public String getName() {
                    return this.name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public String getType() {
                    return this.type;
                }

                public void setType(String type) {
                    this.type = type;
                }

                public double getAngle() {
                    return this.angle;
                }

                public void setAngle(double angle) {
                    this.angle = angle;
                }
            }
        }
    }
}
