package com.keenon.sdk.api;

import android.util.Base64;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.coap.adapter.IProgressCallBack;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import java.util.List;
import org.eclipse.californium.core.coap.Request;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDevicePostCheckStatusV2Api.class */
@LinkAdapter
@CoapCommond(path = "/diagnosis/deviceCheckStatus/v2", requestType = RequestEnum.POST_LARGE)
public class RobotDevicePostCheckStatusV2Api {
    private IDataCallback callBack;
    private String type;

    @CoapResponse
    IProgressCallBack coapCallback = new IProgressCallBack<String>() { // from class: com.keenon.sdk.api.RobotDevicePostCheckStatusV2Api.1
        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RobotDeviceCheckStatusApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RobotDeviceCheckStatusApi][onSuccess json: " + jsonResult + "]");
            if (RobotDevicePostCheckStatusV2Api.this.callBack == null) {
                return;
            }
            RobotDevicePostCheckStatusV2Api.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void progress(int percent) {
        }

        @Override // com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RobotDevicePostCheckStatusV2Api.this.callBack == null) {
                return;
            }
            RobotDevicePostCheckStatusV2Api.this.callBack.error(error);
        }

        @Override // com.keenon.sdk.coap.adapter.IProgressCallBack
        public void readyToSend(Request request) {
        }
    };

    @CoapParams
    public String CoapParams() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", this.type);
        } catch (JSONException e) {
            LogUtils.e(PeanutConstants.TAG_API, "[RobotDeviceCheckApi]", (Exception) e);
        }
        return jsonObject.toString();
    }

    public void send(IDataCallback callBack, String type) {
        this.callBack = callBack;
        this.type = type;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDevicePostCheckStatusV2Api$Bean.class */
    static class Bean extends ApiData {
        private DataBean data;

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDevicePostCheckStatusV2Api$Bean$DataBean.class */
        public static class DataBean {
            public List<InfoBean> info;

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDevicePostCheckStatusV2Api$Bean$DataBean$DetailBean.class */
            public static class DetailBean {
                public String width;
                public String widthRef;
                public String ratio;
                public String ratioRef;
                public String roll;
                public String pitch;
                public String diameter;
                public String diameterRef;
                public String points;
                public int count;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDevicePostCheckStatusV2Api$Bean$DataBean$InfoBean.class */
            public static class InfoBean {
                public int id;
                public String name;
                public String type;
                public int checkStatus;
                public CheckDataBean checkData;

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDevicePostCheckStatusV2Api$Bean$DataBean$InfoBean$CheckDataBean.class */
                public static class CheckDataBean {
                    public int percent;
                    public String result;
                    public int errCode;
                    public DetailBean detail;
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDevicePostCheckStatusV2Api$Bean$DataBean$PointXY.class */
            public static class PointXY {
                public float centimeterX;
                public float centimeterY;
            }
        }

        Bean() {
        }

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        public void setBytes() {
            byte[] bytes = Base64.decode("", 0);
            int len = bytes.length / 4;
            for (int iPoint = 0; iPoint < len; iPoint++) {
                int iByte = iPoint * 4;
                DataBean.PointXY point = new DataBean.PointXY();
                byte data1 = bytes[iByte];
                byte data2 = bytes[iByte + 1];
                byte data3 = bytes[iByte + 2];
                byte data4 = bytes[iByte + 3];
                int signBitX = (data1 >> 4) & 15;
                int dataX = ((data1 & 15) << 8) | (data2 & 255);
                int signBitY = (data3 >> 4) & 15;
                int dataY = ((data3 & 15) << 8) | (data4 & 255);
                point.centimeterX = signBitX == 1 ? dataX : dataX - (2 * dataX);
                point.centimeterY = signBitY == 1 ? dataY : dataY - (2 * dataY);
            }
        }
    }
}
