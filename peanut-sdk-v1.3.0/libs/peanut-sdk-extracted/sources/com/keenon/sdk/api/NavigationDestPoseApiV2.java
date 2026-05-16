package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApiV2.class */
@LinkAdapter
@CoapCommond(path = "/navigation/dest_posesV2")
public class NavigationDestPoseApiV2 {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationDestPoseApiV2.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationDestPoseApiV2][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationDestPoseApiV2][onSuccess json: " + jsonResult + "]");
            if (NavigationDestPoseApiV2.this.callBack == null) {
                return;
            }
            NavigationDestPoseApiV2.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationDestPoseApiV2.this.callBack == null) {
                return;
            }
            NavigationDestPoseApiV2.this.callBack.error(error);
        }
    };

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApiV2$Bean.class */
    public static class Bean extends ApiData {
        private List<DataBean> data;

        public List<DataBean> getData() {
            return this.data;
        }

        public void setData(List<DataBean> data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApiV2$Bean$DataBean.class */
        public static class DataBean {
            private String buildingInfo;
            private List<FloorBean> floorArray;

            public String getBuildingInfo() {
                return this.buildingInfo;
            }

            public void setBuildingInfo(String buildingInfo) {
                this.buildingInfo = buildingInfo;
            }

            public List<FloorBean> getFloorArray() {
                return this.floorArray;
            }

            public void setFloorArray(List<FloorBean> floorArray) {
                this.floorArray = floorArray;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApiV2$Bean$DataBean$FloorBean.class */
            public static class FloorBean {
                private int floor;
                private String floorInfo;
                private List<DestBean> destArray;

                public int getFloor() {
                    return this.floor;
                }

                public void setFloor(int floor) {
                    this.floor = floor;
                }

                public String getFloorInfo() {
                    return this.floorInfo;
                }

                public void setFloorInfo(String floorInfo) {
                    this.floorInfo = floorInfo;
                }

                public List<DestBean> getDestArray() {
                    return this.destArray;
                }

                public void setDestArray(List<DestBean> destArray) {
                    this.destArray = destArray;
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApiV2$Bean$DataBean$FloorBean$DestBean.class */
                public static class DestBean {
                    private int id;
                    private String name;
                    private String type;
                    private String phoneStr;

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

                    public String getPhoneStr() {
                        return this.phoneStr;
                    }

                    public void setPhoneStr(String phoneStr) {
                        this.phoneStr = phoneStr;
                    }
                }
            }
        }
    }
}
