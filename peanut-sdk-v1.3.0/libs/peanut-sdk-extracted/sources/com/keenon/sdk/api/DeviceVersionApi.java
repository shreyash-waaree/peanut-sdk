package com.keenon.sdk.api;

import com.google.gson.annotations.SerializedName;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import java.io.Serializable;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceVersionApi.class */
@LinkAdapter
@CoapCommond(path = "/devices/version")
public class DeviceVersionApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.DeviceVersionApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DeviceVersionApi][onSuccess: " + result + "]");
            if (DeviceVersionApi.this.callBack == null) {
                return;
            }
            DeviceVersionApi.this.callBack.success(result);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DeviceVersionApi.this.callBack == null) {
                return;
            }
            DeviceVersionApi.this.callBack.error(error);
        }
    };

    public void send(IDataCallback callBack) {
        this.callBack = callBack;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceVersionApi$Bean.class */
    public static class Bean implements Serializable {
        private int status;
        private int code;
        private String msg;
        private DataBean data;

        public int getStatus() {
            return this.status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public int getCode() {
            return this.code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return this.msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        public String toString() {
            return "{status=" + this.status + ", code=" + this.code + ", msg=" + this.msg + ", data=" + this.data.toString() + '}';
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceVersionApi$Bean$DataBean.class */
        public static class DataBean {

            @SerializedName("property-name-en")
            private PropertynameenBean propertynameen;

            @SerializedName("property-name-cn")
            private PropertynamecnBean propertynamecn;
            private List<String> property;
            private List<DevicesBean> devices;

            public PropertynameenBean getPropertynameen() {
                return this.propertynameen;
            }

            public void setPropertynameen(PropertynameenBean propertynameen) {
                this.propertynameen = propertynameen;
            }

            public PropertynamecnBean getPropertynamecn() {
                return this.propertynamecn;
            }

            public void setPropertynamecn(PropertynamecnBean propertynamecn) {
                this.propertynamecn = propertynamecn;
            }

            public List<String> getProperty() {
                return this.property;
            }

            public void setProperty(List<String> property) {
                this.property = property;
            }

            public List<DevicesBean> getDevices() {
                return this.devices;
            }

            public void setDevices(List<DevicesBean> devices) {
                this.devices = devices;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceVersionApi$Bean$DataBean$PropertynameenBean.class */
            public static class PropertynameenBean {
                private String abc;

                public String getAbc() {
                    return this.abc;
                }

                public void setAbc(String abc) {
                    this.abc = abc;
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceVersionApi$Bean$DataBean$PropertynamecnBean.class */
            public static class PropertynamecnBean {
                private String abc;

                public String getAbc() {
                    return this.abc;
                }

                public void setAbc(String abc) {
                    this.abc = abc;
                }
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceVersionApi$Bean$DataBean$DevicesBean.class */
            public static class DevicesBean {
                private int id;
                private String sw;
                private String hw;
                private String sn;
                private String env;

                @SerializedName("name-cn")
                private String namecn;

                @SerializedName("name-en")
                private String nameen;

                @SerializedName("sub-version")
                private List<SubversionBean> subversion;

                public int getId() {
                    return this.id;
                }

                public void setId(int id) {
                    this.id = id;
                }

                public String getSw() {
                    return this.sw;
                }

                public void setSw(String sw) {
                    this.sw = sw;
                }

                public String getHw() {
                    return this.hw;
                }

                public void setHw(String hw) {
                    this.hw = hw;
                }

                public String getSn() {
                    return this.sn;
                }

                public void setSn(String sn) {
                    this.sn = sn;
                }

                public String getEnv() {
                    return this.env;
                }

                public void setEnv(String env) {
                    this.env = env;
                }

                public String getNamecn() {
                    return this.namecn;
                }

                public void setNamecn(String namecn) {
                    this.namecn = namecn;
                }

                public String getNameen() {
                    return this.nameen;
                }

                public void setNameen(String nameen) {
                    this.nameen = nameen;
                }

                public List<SubversionBean> getSubversion() {
                    return this.subversion;
                }

                public void setSubversion(List<SubversionBean> subversion) {
                    this.subversion = subversion;
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceVersionApi$Bean$DataBean$DevicesBean$SubversionBean.class */
                public static class SubversionBean {
                    private String name;
                    private String version;

                    public String getName() {
                        return this.name;
                    }

                    public void setName(String name) {
                        this.name = name;
                    }

                    public String getVersion() {
                        return this.version;
                    }

                    public void setVersion(String version) {
                        this.version = version;
                    }
                }
            }
        }
    }
}
