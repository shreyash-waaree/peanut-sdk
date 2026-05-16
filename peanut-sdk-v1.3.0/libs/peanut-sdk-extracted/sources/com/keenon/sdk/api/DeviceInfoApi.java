package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.StringUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.hedera.model.ApiCallback;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.proxy.sender.SenderManager;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceInfoApi.class */
@LinkAdapter
@CoapCommond(path = "/devices/info", requestType = RequestEnum.POST)
public class DeviceInfoApi {
    private IDataCallback callBack;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.DeviceInfoApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[DeviceInfoApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[DeviceInfoApi][onSuccess json: " + jsonResult + "]");
            if (DeviceInfoApi.this.callBack == null) {
                return;
            }
            DeviceInfoApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (DeviceInfoApi.this.callBack == null) {
                return;
            }
            DeviceInfoApi.this.callBack.error(error);
        }
    };
    private String board;

    @CoapParams
    public String CoapParams() {
        return this.board;
    }

    public void send(IDataCallback callBack, String board) {
        this.callBack = callBack;
        this.board = board;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceInfoApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceInfoApi$Bean$DataBean.class */
        public class DataBean {
            private int id;
            private String device;
            private String name;
            private String sn;
            private VersionBean version;

            public DataBean() {
            }

            public int getId() {
                return this.id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getDevice() {
                return this.device;
            }

            public void setDevice(String device) {
                this.device = device;
            }

            public String getName() {
                if (StringUtils.isEmpty(this.name)) {
                    return getDevice();
                }
                return this.name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getSn() {
                return this.sn;
            }

            public void setSn(String sn) {
                this.sn = sn;
            }

            public VersionBean getVersion() {
                return this.version;
            }

            public void setVersion(VersionBean version) {
                this.version = version;
            }

            /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceInfoApi$Bean$DataBean$VersionBean.class */
            public class VersionBean {
                private SwBean sw;
                private HwBean hw;
                private EnvBean env;

                public VersionBean() {
                }

                public SwBean getSw() {
                    return this.sw;
                }

                public void setSw(SwBean sw) {
                    this.sw = sw;
                }

                public HwBean getHw() {
                    return this.hw;
                }

                public void setHw(HwBean hw) {
                    this.hw = hw;
                }

                public EnvBean getEnv() {
                    return this.env;
                }

                public void setEnv(EnvBean env) {
                    this.env = env;
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceInfoApi$Bean$DataBean$VersionBean$SwBean.class */
                public class SwBean {
                    private String number;
                    private String base;
                    private String md5;

                    public SwBean() {
                    }

                    public String getNumber() {
                        return this.number;
                    }

                    public void setNumber(String number) {
                        this.number = number;
                    }

                    public String getBase() {
                        return this.base;
                    }

                    public void setBase(String base) {
                        this.base = base;
                    }

                    public String getMd5() {
                        return this.md5;
                    }

                    public void setMd5(String md5) {
                        this.md5 = md5;
                    }
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceInfoApi$Bean$DataBean$VersionBean$HwBean.class */
                public class HwBean {
                    private String number;

                    public HwBean() {
                    }

                    public String getNumber() {
                        return this.number;
                    }

                    public void setNumber(String number) {
                        this.number = number;
                    }
                }

                /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/DeviceInfoApi$Bean$DataBean$VersionBean$EnvBean.class */
                public class EnvBean {
                    private String number;
                    private String base;
                    private String md5;

                    public EnvBean() {
                    }

                    public String getNumber() {
                        return this.number;
                    }

                    public void setNumber(String number) {
                        this.number = number;
                    }

                    public String getBase() {
                        return this.base;
                    }

                    public void setBase(String base) {
                        this.base = base;
                    }

                    public String getMd5() {
                        return this.md5;
                    }

                    public void setMd5(String md5) {
                        this.md5 = md5;
                    }
                }
            }
        }
    }
}
