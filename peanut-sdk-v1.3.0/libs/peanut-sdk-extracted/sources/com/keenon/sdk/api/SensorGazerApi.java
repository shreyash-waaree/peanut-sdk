package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorGazerApi.class */
@LinkAdapter
@CoapCommond(path = "/sensor/gazer")
public class SensorGazerApi {
    private IDataCallback callBack;
    private RequestEnum requestEnum = RequestEnum.GET;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.SensorGazerApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[SensorGazerApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            bean.setTopic(getClass());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[SensorGazerApi][onSuccess json: " + jsonResult + "]");
            if (SensorGazerApi.this.callBack == null) {
                return;
            }
            if (SensorGazerApi.this.requestEnum == RequestEnum.GET) {
                SensorGazerApi.this.callBack.success(jsonResult);
            } else if (SensorGazerApi.this.requestEnum == RequestEnum.OBSERVER) {
                PeanutSDK.getInstance().notifyApiSuccess(this, jsonResult);
            }
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (SensorGazerApi.this.callBack == null) {
                return;
            }
            SensorGazerApi.this.callBack.error(error);
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

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorGazerApi$Bean.class */
    public static class Bean extends ApiData implements Serializable {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorGazerApi$Bean$DataBean.class */
        public static class DataBean {
            private int count;
            private String base64;

            public int getCount() {
                return this.count;
            }

            public void setCount(int count) {
                this.count = count;
            }

            public String getBase64() {
                return this.base64;
            }

            public void setBase64(String base64) {
                this.base64 = base64;
            }

            public String toString() {
                return "{count=" + this.count + ", base64=" + this.base64 + '}';
            }
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorGazerApi$Bean$LocatorBean.class */
        public static class LocatorBean {
            private static final int LOCATOR_ITEM_NUM = 5;
            List<LocatorData> locatorDataList;
            private Byte[] bytes;

            public Byte[] getBytes() {
                return this.bytes;
            }

            public void setBytes(Byte[] bytes) {
                this.bytes = bytes;
            }

            public String toString() {
                byte[] bytes = ByteUtils.ObjectToByte(getBytes());
                return new String(bytes);
            }

            public List<LocatorData> getLocatorDataList(byte[] result) throws NumberFormatException {
                this.locatorDataList = new ArrayList();
                if (null == result) {
                    return this.locatorDataList;
                }
                String resultLocator = new String(result);
                String[] temp = resultLocator.substring(1).split("\\|");
                int tagNum = Integer.parseInt(resultLocator.substring(0, 1));
                if (tagNum * 5 != temp.length) {
                    return null;
                }
                for (int i = 0; i < tagNum; i++) {
                    LocatorData locatorData = new LocatorData(Integer.valueOf(temp[i * 5]).intValue(), Float.parseFloat(temp[(i * 5) + 1]), Float.parseFloat(temp[(i * 5) + 2]), Float.parseFloat(temp[(i * 5) + 3]), Float.parseFloat(temp[(i * 5) + 4]));
                    this.locatorDataList.add(locatorData);
                }
                return this.locatorDataList;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/SensorGazerApi$Bean$LocatorBean$LocatorData.class */
            public class LocatorData {
                int id;
                float degreeAngle;
                float centimeterX;
                float centimeterY;
                float centimeterZ;

                public LocatorData(int id, float degreeAngle, float centimeterX, float centimeterY, float centimeterZ) {
                    this.id = id;
                    this.degreeAngle = degreeAngle;
                    this.centimeterX = centimeterX;
                    this.centimeterY = centimeterY;
                    this.centimeterZ = centimeterZ;
                }

                public int getId() {
                    return this.id;
                }

                public void setId(int id) {
                    this.id = id;
                }

                public float getDegreeAngle() {
                    return this.degreeAngle;
                }

                public void setDegreeAngle(float degreeAngle) {
                    this.degreeAngle = degreeAngle;
                }

                public float getCentimeterX() {
                    return this.centimeterX;
                }

                public void setCentimeterX(float centimeterX) {
                    this.centimeterX = centimeterX;
                }

                public float getCentimeterY() {
                    return this.centimeterY;
                }

                public void setCentimeterY(float centimeterY) {
                    this.centimeterY = centimeterY;
                }

                public float getCentimeterZ() {
                    return this.centimeterZ;
                }

                public void setCentimeterZ(float centimeterZ) {
                    this.centimeterZ = centimeterZ;
                }
            }
        }
    }
}
