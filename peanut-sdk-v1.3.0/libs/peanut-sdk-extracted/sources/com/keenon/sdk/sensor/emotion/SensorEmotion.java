package com.keenon.sdk.sensor.emotion;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Base64;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.api.EmotionRunApi;
import com.keenon.sdk.api.EmotionRunLocalApi;
import com.keenon.sdk.api.EmotionSendDataApi;
import com.keenon.sdk.api.EmotionSendEndApi;
import com.keenon.sdk.api.EmotionSendStartApi;
import com.keenon.sdk.api.EmotionSetIdleApi;
import com.keenon.sdk.api.EmotionSetStartUpApi;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiData;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.sensor.common.Sensor;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/emotion/SensorEmotion.class */
public class SensorEmotion extends Sensor implements Handler.Callback {
    private static final String TAG = "SensorEmotion";
    public static final int BOARD_V1 = 1;
    public static final int BOARD_V2 = 2;
    private static final int MSG_PREPARE = 1;
    private static final int MSG_SEND_START = 2;
    private static final int MSG_SEND_DATA = 3;
    private static final int MSG_SEND_END = 4;
    private static final int MSG_STOP = 5;
    public static final int STATE_IDLE = 0;
    public static final int STATE_SEND_START = 1;
    public static final int STATE_SEND_START_ACK = 2;
    public static final int STATE_SEND_DATA = 3;
    public static final int STATE_SEND_DATA_ACK = 4;
    public static final int STATE_SEND_END = 4;
    public static final int STATE_SEND_END_ACK = 5;
    private static final int ERROR_INTERNAL = 0;
    private static final int ERROR_CRC = 1;
    private static final int ERROR_ALREADY_RUN = 2;
    private static final int ERROR_DATA_DROPPED = 3;
    private static final int ERROR_PICTURE_LIMIT = 4;
    private static final int ERROR_PICTURE_MORE = 5;
    private static final int ERROR_PICTURE_LESS = 6;
    private static final int ERROR_OVER = 7;
    private static final int ERROR_TIMEOUT = 8;
    public static final int SIZE_EMOTION_CONFIG = 4;
    public static final int SIZE_EMOTION_PICTURE = 192;
    public static final int SIZE_MAX_PICTURES_ONE_PACKET = 10;
    public static final int SIZE_ONE_PACKET = 1920;
    private Handler handler;
    private HandlerThread emotionThread;
    private byte[] emotionBytes;
    private Boolean force;
    private Integer repeat;
    private volatile int state;
    private volatile boolean isCanceled;
    private int emotionDataLength;
    private int packetIndex;
    private IDataCallback runFaceV2Callback;
    private IDataCallback emotionStatusCallback = new IDataCallback() { // from class: com.keenon.sdk.sensor.emotion.SensorEmotion.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.i(PeanutConstants.TAG_API, "SensorEmotion[subscribe status callback: " + result + "]");
            ApiData data = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
            if (data == null) {
                SensorEmotion.this.notifyStateChanged(0);
                return;
            }
            if (data.isSuccess()) {
                if (data.getCode() == 1) {
                    SensorEmotion.this.notifyStateChanged(8);
                } else if (data.getCode() == 2) {
                    SensorEmotion.this.notifyStateChanged(7);
                }
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };
    private static volatile SensorEmotion sInstance;

    public static SensorEmotion getInstance() {
        if (sInstance == null) {
            synchronized (SensorEmotion.class) {
                if (sInstance == null) {
                    sInstance = new SensorEmotion();
                }
            }
        }
        return sInstance;
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return SensorEmotion.class.getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    public void runEmotion(IDataCallback callBack, int version, String emotion, Boolean force, Integer repeat) {
        if (version == 1) {
            runEmotionV1(callBack, emotion, force, repeat);
        } else if (version == 2) {
            runEmotionV2(callBack, emotion, force, repeat);
        }
    }

    public void runLocalEmotion(IDataCallback callBack, int emotionId) {
        new EmotionRunLocalApi().send(callBack, Integer.valueOf(emotionId));
    }

    private void runEmotionV1(IDataCallback callBack, String emotion, Boolean force, Integer repeat) {
        new EmotionRunApi().send(callBack, emotion, force, repeat);
    }

    public void setIdleEmotion(IDataCallback callBack, String emotion) {
        new EmotionSetIdleApi().send(callBack, emotion);
    }

    public void setStartupEmotion(IDataCallback callBack, String emotion) {
        new EmotionSetStartUpApi().send(callBack, emotion);
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
        if (this.handler != null) {
            this.handler.removeCallbacksAndMessages(null);
        }
        if (this.emotionThread != null) {
            this.emotionThread.quit();
        }
    }

    private void runEmotionV2(IDataCallback callBack, String emotion, Boolean force, Integer repeat) {
        this.runFaceV2Callback = callBack;
        this.emotionBytes = Base64.decode(emotion, 0);
        this.emotionDataLength = this.emotionBytes.length - 4;
        this.force = force;
        this.repeat = repeat;
        this.packetIndex = 0;
        prepareRunEmotionV2();
    }

    private void prepareRunEmotionV2() {
        LogUtils.i(PeanutConstants.TAG_API, "SensorEmotion[runFaceV2 state: " + getState() + "]");
        if (this.emotionThread == null) {
            this.emotionThread = new HandlerThread("EmotionHandler#", 0);
            this.emotionThread.start();
            this.handler = new Handler(this.emotionThread.getLooper(), this);
        }
        PeanutSDK.getInstance().unSubscribe(TopicName.EMOTION_STATUS, this.emotionStatusCallback);
        PeanutSDK.getInstance().subscribe(TopicName.EMOTION_STATUS, this.emotionStatusCallback);
        if (getState() != 0 && getState() != 4 && getState() != 5) {
            this.handler.sendEmptyMessage(5);
        } else {
            doNewTask();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doNewTask() {
        LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[doNewTask]");
        this.isCanceled = false;
        setState(0);
        this.handler.sendEmptyMessage(2);
    }

    private void sendStartPacket() {
        LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send start]");
        if (isTaskCanceled()) {
            LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send start, break the flow with new task]");
            return;
        }
        byte[] paramsBytes = new byte[4];
        System.arraycopy(this.emotionBytes, 0, paramsBytes, 0, 4);
        if (null != this.force) {
            paramsBytes[0] = this.force.booleanValue() ? (byte) 1 : (byte) 0;
        }
        if (null != this.repeat) {
            paramsBytes[1] = (byte) this.repeat.intValue();
        }
        new EmotionSendStartApi().send(new IDataCallback() { // from class: com.keenon.sdk.sensor.emotion.SensorEmotion.2
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send start callback: " + result + "]");
                if (SensorEmotion.this.isTaskCanceled()) {
                    LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send start ack, break the flow with new task]");
                    return;
                }
                ApiData data = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
                if (data == null) {
                    SensorEmotion.this.notifyStateChanged(0);
                    return;
                }
                if (data.isSuccess()) {
                    SensorEmotion.this.setState(2);
                    SensorEmotion.this.handler.sendEmptyMessage(3);
                } else if (data.getCode() == 1) {
                    SensorEmotion.this.notifyStateChanged(1);
                } else if (data.getCode() == 2) {
                    SensorEmotion.this.notifyStateChanged(2);
                } else if (data.getCode() == 3) {
                    SensorEmotion.this.notifyStateChanged(4);
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorEmotion.this.notifyError(error);
            }
        }, paramsBytes);
        setState(1);
    }

    private byte[] preparePacketData() {
        int remainDataLength = this.emotionDataLength - (this.packetIndex * SIZE_ONE_PACKET);
        int paramsLength = remainDataLength / SIZE_EMOTION_PICTURE > 10 ? SIZE_ONE_PACKET : remainDataLength;
        byte[] paramsBytes = new byte[paramsLength];
        LogUtils.i(PeanutConstants.TAG_API, "SensorEmotion[getPictureDataToSend][remainDataLength: " + remainDataLength + "]");
        LogUtils.i(PeanutConstants.TAG_API, "SensorEmotion[getPictureDataToSend][packetIndex: " + this.packetIndex + "]");
        System.arraycopy(this.emotionBytes, 4 + (this.packetIndex * paramsLength), paramsBytes, 0, paramsLength);
        this.packetIndex++;
        return paramsBytes;
    }

    private void sendDataPacket() {
        LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send data]");
        if (isTaskCanceled()) {
            LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send data, break the flow with new task]");
        } else {
            new EmotionSendDataApi().send(new IDataCallback() { // from class: com.keenon.sdk.sensor.emotion.SensorEmotion.3
                @Override // com.keenon.sdk.external.IDataCallback
                public void success(String result) {
                    LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send data callback: " + result + "]");
                    if (SensorEmotion.this.isTaskCanceled()) {
                        LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send data ack, break the flow with new task]");
                        return;
                    }
                    ApiData data = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
                    if (data == null) {
                        SensorEmotion.this.notifyStateChanged(0);
                        return;
                    }
                    if (data.isSuccess()) {
                        SensorEmotion.this.setState(4);
                        if (SensorEmotion.this.packetIndex * SensorEmotion.SIZE_ONE_PACKET >= SensorEmotion.this.emotionDataLength) {
                            SensorEmotion.this.handler.sendEmptyMessage(4);
                            return;
                        } else {
                            SensorEmotion.this.handler.sendEmptyMessage(3);
                            return;
                        }
                    }
                    if (data.getCode() == 1) {
                        SensorEmotion.this.notifyStateChanged(1);
                    } else if (data.getCode() == 2) {
                        SensorEmotion.this.notifyStateChanged(5);
                    } else if (data.getCode() == 3) {
                        SensorEmotion.this.notifyStateChanged(3);
                    }
                }

                @Override // com.keenon.sdk.external.IDataCallback
                public void error(ApiError error) {
                    SensorEmotion.this.notifyError(error);
                }
            }, preparePacketData());
            setState(3);
        }
    }

    private void sendEndPacket() {
        LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send end]");
        new EmotionSendEndApi().send(new IDataCallback() { // from class: com.keenon.sdk.sensor.emotion.SensorEmotion.4
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send end callback: " + result + "]");
                if (SensorEmotion.this.isTaskCanceled()) {
                    LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[send end ack, reset idle and start new]");
                    SensorEmotion.this.doNewTask();
                    return;
                }
                ApiData data = (ApiData) GsonUtil.gson2Bean(result, ApiData.class);
                if (data == null) {
                    SensorEmotion.this.notifyStateChanged(0);
                    return;
                }
                if (data.isSuccess()) {
                    SensorEmotion.this.setState(5);
                } else if (data.getCode() == 1) {
                    SensorEmotion.this.notifyStateChanged(1);
                } else if (data.getCode() == 2) {
                    SensorEmotion.this.notifyStateChanged(6);
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                SensorEmotion.this.notifyError(error);
            }
        });
        setState(4);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTaskCanceled() {
        LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[isFlowEffected][canceled: " + this.isCanceled + "]");
        return this.isCanceled;
    }

    private void stopInternal() {
        this.isCanceled = true;
        this.handler.removeMessages(3);
        this.handler.sendEmptyMessage(4);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyStateChanged(int status) {
        setState(0);
        if (this.runFaceV2Callback != null) {
            ApiData bean = new ApiData();
            bean.setStatus(status);
            String jsonResult = GsonUtil.bean2String(bean);
            LogUtils.i(PeanutConstants.TAG_API, "SensorEmotion[notifyStateChanged: " + jsonResult + "]");
            this.runFaceV2Callback.success(jsonResult);
            return;
        }
        LogUtils.w(PeanutConstants.TAG_API, "SensorEmotion[notifyStateChanged null callback]");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyError(ApiError error) {
        if (this.runFaceV2Callback != null) {
            this.runFaceV2Callback.error(error);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void setState(int state) {
        LogUtils.i(PeanutConstants.TAG_API, "SensorEmotion[state: " + this.state + " => " + state + "]");
        this.state = state;
    }

    private int getState() {
        return this.state;
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        LogUtils.d(PeanutConstants.TAG_API, "SensorEmotion[handleMessage: " + msg.what + "]");
        try {
            switch (msg.what) {
                case 1:
                    prepareRunEmotionV2();
                    break;
                case 2:
                    sendStartPacket();
                    break;
                case 3:
                    sendDataPacket();
                    break;
                case 4:
                    sendEndPacket();
                    break;
                case 5:
                    stopInternal();
                    break;
            }
            return true;
        } catch (RuntimeException e) {
            LogUtils.e(PeanutConstants.TAG_API, TAG, (Exception) e);
            return true;
        }
    }
}
