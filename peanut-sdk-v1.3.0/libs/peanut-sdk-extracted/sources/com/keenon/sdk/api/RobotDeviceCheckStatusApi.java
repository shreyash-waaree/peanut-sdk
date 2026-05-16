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
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDeviceCheckStatusApi.class */
@LinkAdapter
@CoapCommond(path = "/diagnosis/deviceCheckStatus", requestType = RequestEnum.GET)
public class RobotDeviceCheckStatusApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.RobotDeviceCheckStatusApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[RobotDeviceCheckStatusApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[RobotDeviceCheckStatusApi][onSuccess json: " + jsonResult + "]");
            if (RobotDeviceCheckStatusApi.this.callBack == null) {
                return;
            }
            RobotDeviceCheckStatusApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (RobotDeviceCheckStatusApi.this.callBack == null) {
                return;
            }
            RobotDeviceCheckStatusApi.this.callBack.error(error);
        }
    };

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDeviceCheckStatusApi$Bean.class */
    static class Bean extends ApiData {
        private DataBean data;

        Bean() {
        }

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDeviceCheckStatusApi$Bean$DataBean.class */
        public static class DataBean {
            private String type;
            private List<InfoBean> info;

            public String getType() {
                return this.type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public List<InfoBean> getInfo() {
                return this.info;
            }

            public void setInfo(List<InfoBean> info) {
                this.info = info;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDeviceCheckStatusApi$Bean$DataBean$InfoBean.class */
            public static class InfoBean {
                private int id;
                private String name;
                private int checkStatus;
                private CheckDataBean checkData;

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

                public int getCheckStatus() {
                    return this.checkStatus;
                }

                public void setCheckStatus(int checkStatus) {
                    this.checkStatus = checkStatus;
                }

                public CheckDataBean getCheckData() {
                    return this.checkData;
                }

                public void setCheckData(CheckDataBean checkData) {
                    this.checkData = checkData;
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/RobotDeviceCheckStatusApi$Bean$DataBean$InfoBean$CheckDataBean.class */
                public static class CheckDataBean {
                    private int percent;
                    private String result;
                    private int errCode;

                    public int getPercent() {
                        return this.percent;
                    }

                    public void setPercent(int percent) {
                        this.percent = percent;
                    }

                    public String getResult() {
                        return this.result;
                    }

                    public void setResult(String result) {
                        this.result = result;
                    }

                    public int getErrCode() {
                        return this.errCode;
                    }

                    public void setErrCode(int errCode) {
                        this.errCode = errCode;
                    }
                }
            }
        }
    }
}
