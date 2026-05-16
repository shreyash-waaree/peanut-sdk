package com.keenon.sdk.api;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
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
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialParams;
import com.keenon.sdk.serial.adapter.SerialResponse;
import com.keenon.sdk.serial.base.SerialData;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationSetMultiTargetApi.class */
@LinkAdapter
@CoapCommond(path = "/navigation/multi", requestType = RequestEnum.POST)
@SerialCommand(action = "62", body = "29")
public class NavigationSetMultiTargetApi {
    private IDataCallback callBack;
    private List<Integer> targetList;

    @CoapResponse
    ApiCallback coapCallback = new ApiCallback<String>() { // from class: com.keenon.sdk.api.NavigationSetMultiTargetApi.1
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationSetMultiTargetApi][onSuccess: " + result + "]");
            Bean bean = (Bean) GsonUtil.gson2Bean(result, Bean.class);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationSetMultiTargetApi][onSuccess json: " + jsonResult + "]");
            if (NavigationSetMultiTargetApi.this.callBack == null) {
                return;
            }
            NavigationSetMultiTargetApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            if (NavigationSetMultiTargetApi.this.callBack == null) {
                return;
            }
            NavigationSetMultiTargetApi.this.callBack.error(error);
        }
    };

    @SerialResponse
    ApiCallback serialCallback = new ApiCallback<SerialData>() { // from class: com.keenon.sdk.api.NavigationSetMultiTargetApi.2
        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onSuccess(SerialData result) {
            LogUtils.i(PeanutConstants.TAG_API, "[NavigationSetMultiTargetApi][onSuccess raw: " + ByteUtils.toString(result.getData()) + "]");
            Bean bean = new Bean();
            bean.setBytes(result.getData());
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.d(PeanutConstants.TAG_API, "[NavigationSetMultiTargetApi][onSuccess json: " + jsonResult + "]");
            NavigationSetMultiTargetApi.this.callBack.success(jsonResult);
        }

        @Override // com.keenon.sdk.hedera.model.ApiCallback, com.keenon.sdk.hedera.model.ICallback
        public void onFail(ApiError error) {
            NavigationSetMultiTargetApi.this.callBack.error(error);
        }
    };
    private boolean isv2 = false;

    @CoapParams
    public String CoapParams() {
        if (this.targetList == null) {
            return "";
        }
        String str = "";
        try {
            Bean.DataBean params = new Bean.DataBean();
            params.dst_1st = (this.targetList.size() <= 0 || this.targetList.get(0) == null) ? 0 : this.targetList.get(0).intValue();
            params.dst_2nd = (this.targetList.size() <= 1 || this.targetList.get(1) == null) ? 0 : this.targetList.get(1).intValue();
            params.dst_3rd = (this.targetList.size() <= 2 || this.targetList.get(2) == null) ? 0 : this.targetList.get(2).intValue();
            params.dst_4th = (this.targetList.size() <= 3 || this.targetList.get(3) == null) ? 0 : this.targetList.get(3).intValue();
            params.dst_5th = (this.targetList.size() <= 4 || this.targetList.get(4) == null) ? 0 : this.targetList.get(4).intValue();
            params.dst_6th = (this.targetList.size() <= 5 || this.targetList.get(5) == null) ? 0 : this.targetList.get(5).intValue();
            params.dst_7th = (this.targetList.size() <= 6 || this.targetList.get(6) == null) ? 0 : this.targetList.get(6).intValue();
            params.dst_8th = (this.targetList.size() <= 7 || this.targetList.get(7) == null) ? 0 : this.targetList.get(7).intValue();
            params.dst_9th = (this.targetList.size() <= 8 || this.targetList.get(8) == null) ? 0 : this.targetList.get(8).intValue();
            params.dst_10th = (this.targetList.size() <= 9 || this.targetList.get(9) == null) ? 0 : this.targetList.get(9).intValue();
            if (this.isv2) {
                params.multiPosesArray = this.targetList;
            }
            str = GsonUtil.bean2String(params);
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_API, "[NavigationSetMultiTargetApi]", e);
        }
        return str;
    }

    @SerialParams
    public Byte[] SerialParams() {
        Byte[] bytes = new Byte[this.targetList.size() * 2];
        for (int i = 0; i < this.targetList.size(); i++) {
            int target = this.targetList.get(i).intValue();
            int index = i * 2;
            int count = index + 2;
            for (int j = index; j < count; j++) {
                if (j % 2 == 0) {
                    bytes[j] = Byte.valueOf((byte) (target & 255));
                } else {
                    bytes[j] = Byte.valueOf((byte) ((target & 65280) >> 8));
                }
            }
        }
        return bytes;
    }

    public void send(IDataCallback callBack, List<Integer> targetList, boolean isV2) {
        this.callBack = callBack;
        this.targetList = targetList;
        this.isv2 = isV2;
        SenderManager.getInstance().send(this);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationSetMultiTargetApi$Bean.class */
    public static class Bean extends ApiData {
        private DataBean data;

        public DataBean getData() {
            return this.data;
        }

        public void setData(DataBean data) {
            this.data = data;
        }

        protected void setBytes(Byte[] bytes) {
            DataBean dataBean = new DataBean();
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length).order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.put(ByteUtils.ObjectToByte(bytes, bytes.length));
            byteBuffer.rewind();
            int length = bytes.length / 2;
            for (int i = 0; i < length; i++) {
                int result = byteBuffer.getShort();
                if (i == 0) {
                    dataBean.setDst_1st(result);
                } else if (i == 1) {
                    dataBean.setDst_2nd(result);
                } else if (i == 2) {
                    dataBean.setDst_3rd(result);
                }
            }
            setData(dataBean);
        }

        /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/api/NavigationSetMultiTargetApi$Bean$DataBean.class */
        public static class DataBean {
            private int dst_1st;
            private int dst_2nd;
            private int dst_3rd;
            private int dst_4th;
            private int dst_5th;
            private int dst_6th;
            private int dst_7th;
            private int dst_8th;
            private int dst_9th;
            private int dst_10th;
            private List<Integer> multiPosesArray;

            public int getDst_1st() {
                return this.dst_1st;
            }

            public void setDst_1st(int dst_1st) {
                this.dst_1st = dst_1st;
            }

            public int getDst_2nd() {
                return this.dst_2nd;
            }

            public void setDst_2nd(int dst_2nd) {
                this.dst_2nd = dst_2nd;
            }

            public int getDst_3rd() {
                return this.dst_3rd;
            }

            public void setDst_3rd(int dst_3rd) {
                this.dst_3rd = dst_3rd;
            }

            public int getDst_4th() {
                return this.dst_4th;
            }

            public void setDst_4th(int dst_4th) {
                this.dst_4th = dst_4th;
            }

            public int getDst_5th() {
                return this.dst_5th;
            }

            public void setDst_5th(int dst_5th) {
                this.dst_5th = dst_5th;
            }

            public int getDst_6th() {
                return this.dst_6th;
            }

            public void setDst_6th(int dst_6th) {
                this.dst_6th = dst_6th;
            }

            public int getDst_7th() {
                return this.dst_7th;
            }

            public void setDst_7th(int dst_7th) {
                this.dst_7th = dst_7th;
            }

            public int getDst_8th() {
                return this.dst_8th;
            }

            public void setDst_8th(int dst_8th) {
                this.dst_8th = dst_8th;
            }

            public int getDst_9th() {
                return this.dst_9th;
            }

            public void setDst_9th(int dst_9th) {
                this.dst_9th = dst_9th;
            }

            public int getDst_10th() {
                return this.dst_10th;
            }

            public void setDst_10th(int dst_10th) {
                this.dst_10th = dst_10th;
            }

            public List<Integer> getMultiPosesArray() {
                return this.multiPosesArray;
            }

            public void setMultiPosesArray(List<Integer> multiPosesArray) {
                this.multiPosesArray = multiPosesArray;
            }

            public List<Integer> toList() {
                if (this.multiPosesArray != null && this.multiPosesArray.size() > 0) {
                    return this.multiPosesArray;
                }
                List<Integer> targetList = new ArrayList<>();
                if (this.dst_1st != 0) {
                    targetList.add(Integer.valueOf(this.dst_1st));
                }
                if (this.dst_2nd != 0) {
                    targetList.add(Integer.valueOf(this.dst_2nd));
                }
                if (this.dst_3rd != 0) {
                    targetList.add(Integer.valueOf(this.dst_3rd));
                }
                if (this.dst_4th != 0) {
                    targetList.add(Integer.valueOf(this.dst_4th));
                }
                if (this.dst_5th != 0) {
                    targetList.add(Integer.valueOf(this.dst_5th));
                }
                if (this.dst_6th != 0) {
                    targetList.add(Integer.valueOf(this.dst_6th));
                }
                if (this.dst_7th != 0) {
                    targetList.add(Integer.valueOf(this.dst_7th));
                }
                if (this.dst_8th != 0) {
                    targetList.add(Integer.valueOf(this.dst_8th));
                }
                if (this.dst_9th != 0) {
                    targetList.add(Integer.valueOf(this.dst_9th));
                }
                if (this.dst_10th != 0) {
                    targetList.add(Integer.valueOf(this.dst_10th));
                }
                return targetList;
            }
        }
    }
}
