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
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/PositionPosInfoApi.class */
@LinkAdapter
@CoapCommond(path = "/position/robotPosInfo")
public class PositionPosInfoApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.PositionPosInfoApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[PositionPosInfoApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[PositionPosInfoApi][onSuccess json: " + jsonResult + "]");
            if (PositionPosInfoApi.this.callBack == null) {
                return;
            }
            if (PositionPosInfoApi.this.requestEnum == RequestEnum.GET) {
                PositionPosInfoApi.this.callBack.success(jsonResult);
            } else if (PositionPosInfoApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (PositionPosInfoApi.this.callBack == null) {
                return;
            }
            if (PositionPosInfoApi.this.requestEnum == RequestEnum.GET) {
                PositionPosInfoApi.this.callBack.error(error);
            } else if (PositionPosInfoApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/PositionPosInfoApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/PositionPosInfoApi$Bean$DataBean.class */
        public class DataBean {
            private int elevatorPosState;
            private int floor;
            private List<String> buildDesc;
            private PosBean robotPos;

            public DataBean() {
            }

            public int getElevatorPosState() {
                return this.elevatorPosState;
            }

            public void setElevatorPosState(int elevatorPosState) {
                this.elevatorPosState = elevatorPosState;
            }

            public int getFloor() {
                return this.floor;
            }

            public void setFloor(int floor) {
                this.floor = floor;
            }

            public List<String> getBuildDesc() {
                return this.buildDesc;
            }

            public void setBuildDesc(List<String> buildDesc) {
                this.buildDesc = buildDesc;
            }

            public PosBean getRobotPos() {
                return this.robotPos;
            }

            public void setRobotPos(PosBean robotPos) {
                this.robotPos = robotPos;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/PositionPosInfoApi$Bean$DataBean$PosBean.class */
            public class PosBean {
                private double x;
                private double y;
                private double rotation;
                private int curId;
                private String curName;
                private double curDistance;

                public PosBean() {
                }

                public int getCurId() {
                    return this.curId;
                }

                public void setCurId(int curId) {
                    this.curId = curId;
                }

                public String getCurName() {
                    return this.curName;
                }

                public void setCurName(String curName) {
                    this.curName = curName;
                }

                public double getCurDistance() {
                    return this.curDistance;
                }

                public void setCurDistance(double curDistance) {
                    this.curDistance = curDistance;
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

                public double getRotation() {
                    return this.rotation;
                }

                public void setRotation(double rotation) {
                    this.rotation = rotation;
                }
            }
        }
    }
}
