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

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationPathApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/path")
public class NavigationPathApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationPathApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationPathApi][onSuccess: " + result + "]");
            Bean.DataBean dataBean = (Bean.DataBean) GsonUtil.gson2Bean(result, Bean.DataBean.class);
            Bean bean = new Bean();
            bean.setData(dataBean);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationPathApi][onSuccess json: " + jsonResult + "]");
            if (NavigationPathApi.this.callBack == null) {
                return;
            }
            if (NavigationPathApi.this.requestEnum == RequestEnum.GET) {
                NavigationPathApi.this.callBack.success(jsonResult);
            } else if (NavigationPathApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationPathApi.this.callBack == null) {
                return;
            }
            if (NavigationPathApi.this.requestEnum == RequestEnum.GET) {
                NavigationPathApi.this.callBack.error(error);
            } else if (NavigationPathApi.this.requestEnum == RequestEnum.OBSERVER) {
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationPathApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        public String toString() {
            return "{, data=" + this.data.toString() + '}';
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationPathApi$Bean$DataBean.class */
        public class DataBean {
            private RobotPoseBean robot_pose;
            private RobotSpeedBean robot_speed;
            private int run_status;
            private int dest_type;
            private DestPoseBean dest_pose;
            private List<PathToBean> path_to;
            private List<PathBack> path_back;

            public DataBean() {
            }

            public RobotPoseBean getRobot_pose() {
                return this.robot_pose;
            }

            public void setRobot_pose(RobotPoseBean robot_pose) {
                this.robot_pose = robot_pose;
            }

            public RobotSpeedBean getRobot_speed() {
                return this.robot_speed;
            }

            public void setRobot_speed(RobotSpeedBean robot_speed) {
                this.robot_speed = robot_speed;
            }

            public int getRun_status() {
                return this.run_status;
            }

            public void setRun_status(int run_status) {
                this.run_status = run_status;
            }

            public int getDest_type() {
                return this.dest_type;
            }

            public void setDest_type(int dest_type) {
                this.dest_type = dest_type;
            }

            public DestPoseBean getDest_pose() {
                return this.dest_pose;
            }

            public void setDest_pose(DestPoseBean dest_pose) {
                this.dest_pose = dest_pose;
            }

            public List<PathToBean> getPath_to() {
                return this.path_to;
            }

            public void setPath_to(List<PathToBean> path_to) {
                this.path_to = path_to;
            }

            public List<PathBack> getPath_back() {
                return this.path_back;
            }

            public void setPath_back(List<PathBack> path_back) {
                this.path_back = path_back;
            }

            public String toString() {
                return "{robot_pose=" + this.robot_pose.toString() + ", robot_speed=" + this.robot_speed.toString() + ", run_status=" + this.run_status + ", dest_type=" + this.dest_type + ", dest_pose=" + this.dest_pose.toString() + ", path_to=" + this.path_to.toString() + ", path_back=" + this.path_back.toString() + '}';
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationPathApi$Bean$DataBean$RobotPoseBean.class */
            public class RobotPoseBean {
                private double x;
                private double y;
                private double phi;

                public RobotPoseBean() {
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

                public double getPhi() {
                    return this.phi;
                }

                public void setPhi(double phi) {
                    this.phi = phi;
                }

                public String toString() {
                    return "{x=" + this.x + ", y=" + this.y + ", phi=" + this.phi + '}';
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationPathApi$Bean$DataBean$RobotSpeedBean.class */
            public class RobotSpeedBean {
                private int left;
                private int right;

                public RobotSpeedBean() {
                }

                public int getLeft() {
                    return this.left;
                }

                public void setLeft(int left) {
                    this.left = left;
                }

                public int getRight() {
                    return this.right;
                }

                public void setRight(int right) {
                    this.right = right;
                }

                public String toString() {
                    return "{left=" + this.left + ", right=" + this.right + '}';
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationPathApi$Bean$DataBean$DestPoseBean.class */
            public class DestPoseBean {
                private double x;
                private double y;
                private double phi;

                public DestPoseBean() {
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

                public double getPhi() {
                    return this.phi;
                }

                public void setPhi(double phi) {
                    this.phi = phi;
                }

                public String toString() {
                    return "{x=" + this.x + ", y=" + this.y + ", phi=" + this.phi + '}';
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationPathApi$Bean$DataBean$PathToBean.class */
            public class PathToBean {
                private int index;
                private double x;
                private double y;
                private double phi;

                public PathToBean() {
                }

                public int getIndex() {
                    return this.index;
                }

                public void setIndex(int index) {
                    this.index = index;
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

                public double getPhi() {
                    return this.phi;
                }

                public void setPhi(double phi) {
                    this.phi = phi;
                }

                public String toString() {
                    return "{index=" + this.index + ", x=" + this.x + ", y=" + this.y + ", phi=" + this.phi + '}';
                }
            }
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationPathApi$Bean$PathBack.class */
        public class PathBack {
            private int index;
            private double x;
            private double y;
            private double phi;

            public PathBack() {
            }

            public int getIndex() {
                return this.index;
            }

            public void setIndex(int index) {
                this.index = index;
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

            public double getPhi() {
                return this.phi;
            }

            public void setPhi(double phi) {
                this.phi = phi;
            }
        }
    }
}
