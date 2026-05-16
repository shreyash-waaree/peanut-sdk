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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/dest_poses")
public class NavigationDestPoseApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationDestPoseApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationDestPoseApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationDestPoseApi][onSuccess json: " + jsonResult + "]");
            if (NavigationDestPoseApi.this.callBack == null) {
                return;
            }
            NavigationDestPoseApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationDestPoseApi.this.callBack == null) {
                return;
            }
            NavigationDestPoseApi.this.callBack.error(error);
        }
    };

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApi$Bean.class */
    public static class Bean extends ApiData {
        private List<DataBean> data;

        public List<DataBean> getData() {
            return this.data;
        }

        public void setData(List<DataBean> data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApi$Bean$DataBean.class */
        public static class DataBean {
            private String bind_map_md5;
            private int elevator_id;
            private int floor;
            private int id;
            private String name;
            private int phone;
            private String phoneStr;
            private PoseBean pose;
            private String type;
            private String buildingInfo;
            private String floorInfo;
            private List<String> buildDesc;

            public List<String> getBuildDesc() {
                return this.buildDesc;
            }

            public void setBuildDesc(List<String> buildDesc) {
                this.buildDesc = buildDesc;
            }

            public String getPhoneStr() {
                return this.phoneStr;
            }

            public void setPhoneStr(String phoneStr) {
                this.phoneStr = phoneStr;
            }

            public String getFloorInfo() {
                return this.floorInfo;
            }

            public void setFloorInfo(String floorInfo) {
                this.floorInfo = floorInfo;
            }

            public String getBuildingInfo() {
                return this.buildingInfo;
            }

            public void setBuildingInfo(String buildingInfo) {
                this.buildingInfo = buildingInfo;
            }

            public String getBind_map_md5() {
                return this.bind_map_md5;
            }

            public void setBind_map_md5(String bind_map_md5) {
                this.bind_map_md5 = bind_map_md5;
            }

            public int getElevator_id() {
                return this.elevator_id;
            }

            public void setElevator_id(int elevator_id) {
                this.elevator_id = elevator_id;
            }

            public int getFloor() {
                return this.floor;
            }

            public void setFloor(int floor) {
                this.floor = floor;
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

            public int getPhone() {
                return this.phone;
            }

            public void setPhone(int phone) {
                this.phone = phone;
            }

            public PoseBean getPose() {
                return this.pose;
            }

            public void setPose(PoseBean pose) {
                this.pose = pose;
            }

            public String getType() {
                return this.type;
            }

            public void setType(String type) {
                this.type = type;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApi$Bean$DataBean$PoseBean.class */
            public static class PoseBean {
                private OrientationBean orientation;
                private PositionBean position;

                public OrientationBean getOrientation() {
                    return this.orientation;
                }

                public void setOrientation(OrientationBean orientation) {
                    this.orientation = orientation;
                }

                public PositionBean getPosition() {
                    return this.position;
                }

                public void setPosition(PositionBean position) {
                    this.position = position;
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApi$Bean$DataBean$PoseBean$OrientationBean.class */
                public static class OrientationBean {
                    private double w;
                    private int x;
                    private int y;
                    private double z;

                    public double getW() {
                        return this.w;
                    }

                    public void setW(double w) {
                        this.w = w;
                    }

                    public int getX() {
                        return this.x;
                    }

                    public void setX(int x) {
                        this.x = x;
                    }

                    public int getY() {
                        return this.y;
                    }

                    public void setY(int y) {
                        this.y = y;
                    }

                    public double getZ() {
                        return this.z;
                    }

                    public void setZ(double z) {
                        this.z = z;
                    }
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationDestPoseApi$Bean$DataBean$PoseBean$PositionBean.class */
                public static class PositionBean {
                    private double x;
                    private double y;
                    private int z;

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

                    public int getZ() {
                        return this.z;
                    }

                    public void setZ(int z) {
                        this.z = z;
                    }
                }
            }
        }
    }
}
