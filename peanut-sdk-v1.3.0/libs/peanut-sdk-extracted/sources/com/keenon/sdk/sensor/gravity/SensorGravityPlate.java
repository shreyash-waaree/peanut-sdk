package com.keenon.sdk.sensor.gravity;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.bean.response.PlateSensorGravityValue;
import com.keenon.sdk.sensor.common.Sensor;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/gravity/SensorGravityPlate.class */
public class SensorGravityPlate extends Sensor {
    private static SensorGravityPlate sInstance;
    public IDataCallback dataCallback;
    private String TAG = "SensorGravityPlate";
    public List<Info.DetailBean> gravits = new ArrayList();
    public int offset = 1;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/gravity/SensorGravityPlate$CalibrationCallback.class */
    public interface CalibrationCallback {
        void onSuccess(String str);

        void onError(int i);
    }

    public static SensorGravityPlate getInstance() {
        if (sInstance == null) {
            synchronized (SensorGravityPlate.class) {
                if (sInstance == null) {
                    sInstance = new SensorGravityPlate();
                }
            }
        }
        return sInstance;
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return SensorGravityPlate.class.getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
        if (this.gravits != null) {
            this.gravits.clear();
            this.gravits = null;
            this.offset = 1;
            this.dataCallback = null;
        }
    }

    public void init(int offset, IDataCallback dataCallback) {
        this.dataCallback = dataCallback;
        this.offset = offset;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Info convertSensorGravity(int location, String result) {
        PlateSensorGravityValue rawBean = (PlateSensorGravityValue) GsonUtil.gson2Bean(result, PlateSensorGravityValue.class);
        Info gravityInfo = new Info();
        Info.DetailBean gravityDetail = new Info.DetailBean();
        gravityInfo.setCode(0);
        gravityInfo.setStatus(0);
        gravityInfo.setMsg(PeanutConstants.API_SUCCESS_MESSAGE);
        gravityDetail.setGravity_1(rawBean.getGravity_1() / getInstance().offset);
        gravityDetail.setGravity_2(rawBean.getGravity_2() / getInstance().offset);
        gravityDetail.setGravity_3(rawBean.getGravity_3() / getInstance().offset);
        gravityDetail.setGravity_4(rawBean.getGravity_4() / getInstance().offset);
        gravityDetail.setGravity_sum(gravityDetail.getGravity_sum());
        gravityDetail.setLocation(location);
        gravityDetail.setGravity_average(gravityDetail.getGravity_average());
        gravityDetail.setDifference(getInstance().getDifference(gravityDetail));
        gravityInfo.setData(gravityDetail);
        return gravityInfo;
    }

    public void onCalibration(final CalibrationCallback callback, int... devs) {
        LogUtils.d(PeanutConstants.TAG_GRAVITY, this.TAG + "[onCalibration][topics length :" + devs.length + " ]+");
        if (this.gravits == null) {
            this.gravits = new ArrayList();
        }
        for (final int dev : devs) {
            SCMRequest request = new SCMRequest();
            request.setDev(dev);
            request.setTopic(12);
            request.setType(0);
            request.setCmd(0);
            request.setSerialDirect(true, "v1.0");
            SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.gravity.SensorGravityPlate.1
                @Override // com.keenon.sdk.external.IDataCallback
                public void success(String result) {
                    LogUtils.d(PeanutConstants.TAG_GRAVITY, SensorGravityPlate.this.TAG + "[onCalibration][result is :" + result + " ]+");
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                    Info gravityInfo = SensorGravityPlate.this.convertSensorGravity(dev, result);
                    if (gravityInfo != null && gravityInfo.getData() != null && SensorGravityPlate.this.gravits != null) {
                        if (SensorGravityPlate.this.gravits.size() > 0) {
                            for (Info.DetailBean gravity : SensorGravityPlate.this.gravits) {
                                if (gravity.getLocation() == gravityInfo.getData().getLocation()) {
                                    SensorGravityPlate.this.gravits.remove(gravity);
                                }
                                SensorGravityPlate.this.gravits.add(gravityInfo.getData());
                            }
                            return;
                        }
                        SensorGravityPlate.this.gravits.add(gravityInfo.getData());
                    }
                }

                @Override // com.keenon.sdk.external.IDataCallback
                public void error(ApiError error) {
                    LogUtils.d(PeanutConstants.TAG_GRAVITY, SensorGravityPlate.this.TAG + "[onCalibration][error is :" + error.toString() + " ]+");
                    if (callback != null) {
                        callback.onError(error.code);
                    }
                }
            });
        }
    }

    public void requestGravityInfo(int... topics) {
        if (this.dataCallback == null) {
            LogUtils.d(PeanutConstants.TAG_GRAVITY, this.TAG + "[requestGravityInfo][dataCallback is null ]");
        } else {
            getGravityInfo(this.dataCallback, topics);
        }
    }

    public int checkGravity(int[] initialGravity, int[] nowGravity) {
        int initialSum = 0;
        int nowGravitySum = 0;
        for (int i = 0; i < initialGravity.length; i++) {
            initialSum += initialGravity[i];
            nowGravitySum += Math.max(initialGravity[i], nowGravity[i]);
        }
        return Math.max(nowGravitySum - initialSum, 0);
    }

    public void setInitialGravity(int[]... initialGravity) {
        this.gravits.clear();
        for (int[] ints : initialGravity) {
            Info.DetailBean bean = new Info.DetailBean();
            bean.setGravity_1(ints[0]);
            bean.setGravity_2(ints[1]);
            bean.setGravity_3(ints[2]);
            bean.setGravity_4(ints[3]);
            this.gravits.add(bean);
        }
    }

    public int getDifference(Info.DetailBean detailBean) {
        LogUtils.d(PeanutConstants.TAG_GRAVITY, this.TAG + "[getDifference][DetailBean : " + detailBean.toString() + "]");
        int difference = 0;
        int[] nowGravity = {detailBean.getGravity_1(), detailBean.getGravity_2(), detailBean.getGravity_3(), detailBean.getGravity_4()};
        if (this.gravits != null && this.gravits.size() > 0) {
            difference = checkGravity(getIntArray(this.gravits, detailBean.getLocation()), nowGravity);
        }
        LogUtils.d(PeanutConstants.TAG_GRAVITY, this.TAG + "[getDifference][Difference in: " + difference + "]");
        return difference;
    }

    public int[] getIntArray(List<Info.DetailBean> dataBeans, int location) {
        int[] ints = new int[4];
        for (int i = 0; i < dataBeans.size(); i++) {
            if (location == dataBeans.get(i).getLocation()) {
                ints[0] = dataBeans.get(i).getGravity_1();
                ints[1] = dataBeans.get(i).getGravity_2();
                ints[2] = dataBeans.get(i).getGravity_3();
                ints[3] = dataBeans.get(i).getGravity_4();
            }
        }
        return ints;
    }

    public void getGravityInfo(final IDataCallback dataCallback, int... devs) {
        for (final int dev : devs) {
            SCMRequest request = new SCMRequest();
            request.setDev(dev);
            request.setTopic(12);
            request.setType(0);
            request.setCmd(0);
            request.setSerialDirect(true, "v1.0");
            SCMIoTSender.sendRequest(request, new IDataCallback() { // from class: com.keenon.sdk.sensor.gravity.SensorGravityPlate.2
                @Override // com.keenon.sdk.external.IDataCallback
                public void success(String result) {
                    Info gravityInfo = SensorGravityPlate.this.convertSensorGravity(dev, result);
                    dataCallback.success(GsonUtil.bean2String(gravityInfo));
                }

                @Override // com.keenon.sdk.external.IDataCallback
                public void error(ApiError error) {
                    dataCallback.error(error);
                }
            });
        }
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/gravity/SensorGravityPlate$Info.class */
    public static class Info extends ApiData {
        private DetailBean data;

        public DetailBean getData() {
            return this.data;
        }

        public void setData(DetailBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/gravity/SensorGravityPlate$Info$DetailBean.class */
        public static class DetailBean {
            private int location;
            private int gravity_1;
            private int gravity_2;
            private int gravity_3;
            private int gravity_4;
            private int gravity_sum;
            private int gravity_average;
            private int difference;

            public int getDifference() {
                return this.difference;
            }

            public void setDifference(int difference) {
                this.difference = difference;
            }

            public int getLocation() {
                return this.location;
            }

            public void setLocation(int location) {
                this.location = location;
            }

            public int getGravity_1() {
                return this.gravity_1;
            }

            public void setGravity_1(int gravity_1) {
                this.gravity_1 = gravity_1;
            }

            public int getGravity_2() {
                return this.gravity_2;
            }

            public void setGravity_2(int gravity_2) {
                this.gravity_2 = gravity_2;
            }

            public int getGravity_3() {
                return this.gravity_3;
            }

            public void setGravity_3(int gravity_3) {
                this.gravity_3 = gravity_3;
            }

            public int getGravity_4() {
                return this.gravity_4;
            }

            public void setGravity_4(int gravity_4) {
                this.gravity_4 = gravity_4;
            }

            public int getGravity_sum() {
                return this.gravity_1 + this.gravity_2 + this.gravity_3 + this.gravity_4;
            }

            public void setGravity_sum(int gravity_sum) {
                this.gravity_sum = gravity_sum;
            }

            public int getGravity_average() {
                return getGravity_sum() / 4;
            }

            public void setGravity_average(int gravity_average) {
                this.gravity_average = gravity_average;
            }

            public String toString() {
                return "DetailBean{location=" + this.location + ", gravity_1=" + this.gravity_1 + ", gravity_2=" + this.gravity_2 + ", gravity_3=" + this.gravity_3 + ", gravity_4=" + this.gravity_4 + ", gravity_sum=" + this.gravity_sum + ", gravity_average=" + this.gravity_average + ", difference=" + this.difference + '}';
            }
        }
    }
}
