package com.keenon.sdk.sensor.callbell;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.annotation.NonNull;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.MessageIdUtil;
import com.keenon.common.utils.PeanutSNManagerUtils;
import com.keenon.common.utils.StringUtils;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import com.keenon.sdk.scmIot.SCMIoTSender;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.sensor.callbell.BellInterface;
import com.keenon.sdk.sensor.common.Sensor;
import com.keenon.sdk.sensor.common.SensorEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.eclipse.californium.elements.util.NamedThreadFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/callbell/SensorCallBell.class */
public final class SensorCallBell extends Sensor {
    private static final String TAG = "SensorCallBell";
    private static volatile SensorCallBell sInstance;
    private static final int RETRY_TIME = 2000;
    private static final int MSG_MAX_SIZE = 1024;
    private static final int HEART_BEAT_TIME = 5;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private byte[] sn;
    private BellMSGBean mBellMSGBean;
    private ScheduledExecutorService scheduledThreadPool;
    private final ConcurrentHashMap<String, BellMSGBean> retryMap = new ConcurrentHashMap<>(16);
    private final List<RepeatMsgBean> receivedMsgList = Collections.synchronizedList(new ArrayList());
    private final ConcurrentHashMap<Integer, String> sendMsgMap = new ConcurrentHashMap<>(16);
    private volatile boolean isStartScheduled = false;
    private volatile boolean isHeartBeatStart = false;
    private final IDataCallback deviceListBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.callbell.SensorCallBell.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            BellDeviceListBean convertDeviceList;
            LogUtils.d(SensorCallBell.TAG, "deviceListBack :" + result);
            BellDeviceListResult deviceListResult = (BellDeviceListResult) GsonUtil.gson2Bean(result, BellDeviceListResult.class);
            if (deviceListResult != null && (convertDeviceList = SensorCallBell.this.getConvertDeviceList(deviceListResult)) != null) {
                SensorCallBell.this.notifyEvent(SensorEvent.RECEIVED_CALL_BELL_DEVICE_LIST, convertDeviceList);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorCallBell.TAG, "deviceListBack :" + error.toString());
            SensorCallBell.this.notifyEvent(SensorEvent.RECEIVED_CALL_BELL_DEVICE_LIST, error);
        }
    };
    private final IDataCallback receivedTaskCallBellBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.callbell.SensorCallBell.4
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(SensorCallBell.TAG, "receivedTaskCallBellBack :" + result);
            BellReceivedTaskResult bellReceivedTaskResult = (BellReceivedTaskResult) GsonUtil.gson2Bean(result, BellReceivedTaskResult.class);
            if (bellReceivedTaskResult != null && SensorCallBell.this.isMatchingSN(bellReceivedTaskResult.getSn())) {
                LogUtils.d(SensorCallBell.TAG, "SN Matching Success");
                SensorCallBell.this.replyTaskAck(bellReceivedTaskResult.getMsgId());
                if (!SensorCallBell.this.isContains(bellReceivedTaskResult.getMsgId(), bellReceivedTaskResult.getTaskId())) {
                    SensorCallBell.this.notifyEvent(SensorEvent.RECEIVED_CALL_BELL_TASK, bellReceivedTaskResult);
                    return;
                }
                return;
            }
            LogUtils.d(SensorCallBell.TAG, "SN Matching Fail");
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorCallBell.TAG, "receivedTaskCallBellBack :" + error.toString());
            SensorCallBell.this.notifyEvent(SensorEvent.RECEIVED_CALL_BELL_TASK, error);
        }
    };
    private final IDataCallback receivedMsgAck = new IDataCallback() { // from class: com.keenon.sdk.sensor.callbell.SensorCallBell.5
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            LogUtils.d(SensorCallBell.TAG, "receivedMsgAck :" + result);
            MessageParam message = (MessageParam) GsonUtil.gson2Bean(result, MessageParam.class);
            if (message != null && SensorCallBell.this.isMatchingSN(message.getSn())) {
                SensorCallBell.this.handleAck(message);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(SensorCallBell.TAG, "receivedMsgAck :" + error.toString());
            SensorCallBell.this.notifyEvent(SensorEvent.RECEIVED_CALL_BELL_TASK, error);
        }
    };
    private final IDataCallback mBottomRwaCallBack = new IDataCallback() { // from class: com.keenon.sdk.sensor.callbell.SensorCallBell.6
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
        }
    };

    public static SensorCallBell getInstance() {
        if (sInstance == null) {
            synchronized (SensorCallBell.class) {
                if (sInstance == null) {
                    sInstance = new SensorCallBell();
                }
            }
        }
        return sInstance;
    }

    private SensorCallBell() {
        MessageIdUtil.init();
        this.sn = getSn();
        initMsgHandler();
        PeanutSDK.getInstance().subscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
        SCMIoTSender.addSingeReportDataCallback(this.deviceListBack, 65, 32, 0);
        SCMIoTSender.addSingeReportDataCallback(this.receivedTaskCallBellBack, 65, 29, 1);
        SCMIoTSender.addSingeReportDataCallback(this.receivedMsgAck, 65, 30, 1);
        SCMIoTSender.addSCMThingDataTransfer(30, new BellMsgAckTransfer());
        SCMIoTSender.addSCMThingDataTransfer(29, new BellReceivedDataTransfer());
        SCMIoTSender.addSCMThingDataTransfer(32, new BellDeviceListDataTransfer());
        this.scheduledThreadPool = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("CallBellHeartBeat#"));
        onStartHeartBeat();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public String name() {
        return getClass().getSimpleName();
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void mount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void unMount() {
    }

    @Override // com.keenon.sdk.sensor.common.Sensor
    public void release() {
        this.isStartScheduled = false;
        this.isHeartBeatStart = false;
        SCMIoTSender.removeSCMThingDataTransfer(32);
        SCMIoTSender.removeSCMThingDataTransfer(29);
        SCMIoTSender.removeSCMThingDataTransfer(30);
        SCMIoTSender.removeSingeReportDataCallback(65, 29, 1);
        SCMIoTSender.removeSingeReportDataCallback(65, 30, 1);
        SCMIoTSender.removeSingeReportDataCallback(65, 32, 0);
        PeanutSDK.getInstance().unSubscribe(TopicName.BOTTOM_RAW, this.mBottomRwaCallBack);
        if (this.mHandler != null) {
            this.mHandler.removeCallbacks(null);
        }
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quit();
        }
        if (this.scheduledThreadPool != null) {
            this.scheduledThreadPool.shutdown();
        }
        this.mHandlerThread = null;
        this.mHandler = null;
        this.scheduledThreadPool = null;
        sInstance = null;
    }

    private void initMsgHandler() {
        this.mHandlerThread = new HandlerThread("#CallBell");
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.keenon.sdk.sensor.callbell.SensorCallBell.1
            @Override // android.os.Handler
            public void handleMessage(@NonNull Message msg) {
                BellMSGBean retryBean;
                BellMSGBean bean = (BellMSGBean) msg.obj;
                if (bean != null && (retryBean = (BellMSGBean) SensorCallBell.this.retryMap.get(bean.msgType)) != null && bean.equals(retryBean)) {
                    SensorCallBell.this.sendRequest(bean.request, bean);
                }
            }
        };
    }

    public void getDeviceList() {
        LogUtils.d(TAG, "getDeviceList()");
        SCMRequest request = new SCMRequest();
        request.setDev(65);
        request.setTopic(32);
        request.setType(0);
        request.setCmd(0);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        request.setChannel(1);
        DeviceListParam param = new DeviceListParam();
        request.setParams(param);
        SCMIoTSender.sendRequest(request, null);
    }

    public void onPlayBellHeartBeat() {
        SCMRequest request = new SCMRequest();
        request.setDev(65);
        request.setTopic(23);
        request.setType(1);
        request.setCmd(0);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        request.setChannel(1);
        HeartBeatParam param = new HeartBeatParam();
        param.setSn(this.sn);
        request.setParams(param);
        SCMIoTSender.sendRequest(request, null);
    }

    public void initBell(BellConfig config) {
        LogUtils.d(TAG, "initBell Config :" + config.toString());
        SCMRequest request = new SCMRequest();
        request.setDev(65);
        request.setTopic(29);
        request.setType(0);
        request.setCmd(1);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        request.setChannel(1);
        BellInitParam prams = new BellInitParam();
        prams.setSn(getSn());
        prams.setMsgId(MessageIdUtil.getMessAgeId());
        prams.setMsgType((byte) 0);
        prams.setStayTime((short) config.getStayTime());
        prams.setKeyEvent((byte) config.getKeyEvent().code);
        prams.setRobotState((byte) config.getRobotState().code);
        prams.setEnable((byte) (config.isEnable() ? 1 : 0));
        request.setParams(prams);
        this.mBellMSGBean = new BellMSGBean(prams.getMsgId(), BellInterface.MSG_TYPE_INIT_CONFIG, request);
        this.retryMap.put(BellInterface.MSG_TYPE_INIT_CONFIG, this.mBellMSGBean);
        putSendMsg(prams.getMsgId(), BellInterface.MSG_TYPE_INIT_CONFIG);
        sendRequest(request, this.mBellMSGBean);
    }

    public void updateBellState(BellConfig config) {
        LogUtils.d(TAG, "updateBellState Config :" + config.toString());
        SCMRequest request = new SCMRequest();
        request.setDev(65);
        request.setTopic(29);
        request.setType(0);
        request.setCmd(1);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        request.setChannel(1);
        BellInitParam prams = new BellInitParam();
        prams.setSn(getSn());
        prams.setMsgId(MessageIdUtil.getMessAgeId());
        prams.setMsgType((byte) 1);
        prams.setStayTime((short) config.getStayTime());
        prams.setKeyEvent((byte) config.getKeyEvent().code);
        prams.setRobotState((byte) config.getRobotState().code);
        prams.setEnable((byte) (config.isEnable() ? 1 : 0));
        request.setParams(prams);
        this.mBellMSGBean = new BellMSGBean(prams.getMsgId(), BellInterface.MSG_TYPE_UPDATE_ROBOT_STATE, request);
        this.retryMap.put(BellInterface.MSG_TYPE_UPDATE_ROBOT_STATE, this.mBellMSGBean);
        putSendMsg(prams.getMsgId(), BellInterface.MSG_TYPE_UPDATE_ROBOT_STATE);
        sendRequest(request, this.mBellMSGBean);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendRequest(SCMRequest request, BellMSGBean bean) {
        LogUtils.d(TAG, "senRequest msgID ：" + bean.msgId);
        SCMIoTSender.sendRequest(request, null);
        Message message = new Message();
        message.what = 1;
        message.obj = bean;
        if (this.mHandler != null) {
            this.mHandler.sendMessageDelayed(message, 2000L);
        }
    }

    public void replyTaskState(int taskId, BellInterface.TaskState state) {
        LogUtils.d(TAG, "replyTaskState taskId :" + taskId + "  TaskState :" + state.getCode());
        SCMRequest request = new SCMRequest();
        request.setDev(65);
        request.setTopic(29);
        request.setType(2);
        request.setCmd(6);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        request.setChannel(1);
        BellReplyTaskParam prams = new BellReplyTaskParam();
        prams.setSn(getSn());
        prams.setTaskID(taskId);
        prams.setMsgId(MessageIdUtil.getMessAgeId());
        prams.setState((byte) state.code);
        request.setParams(prams);
        this.mBellMSGBean = new BellMSGBean(prams.getMsgId(), BellInterface.MSG_TYPE_REPLY_TASK_STATE, request);
        this.retryMap.put(BellInterface.MSG_TYPE_REPLY_TASK_STATE, this.mBellMSGBean);
        putSendMsg(prams.getMsgId(), BellInterface.MSG_TYPE_REPLY_TASK_STATE);
        sendRequest(request, this.mBellMSGBean);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void replyTaskAck(int msgId) {
        LogUtils.d(TAG, "replyTaskAck msgID ：" + msgId);
        SCMRequest request = new SCMRequest();
        request.setDev(65);
        request.setTopic(30);
        request.setType(1);
        request.setCmd(2);
        request.setSerialDirect(isSerialDirect());
        request.setUSBDirect(isUsbDirect());
        request.setTrans(isTrans());
        request.setChannel(1);
        BellMsgAckResult param = new BellMsgAckResult();
        param.setSn(getSn());
        param.setMsgId(msgId);
        request.setParams(param);
        SCMIoTSender.sendRequest(request, null);
    }

    public synchronized void onStartHeartBeat() {
        this.isHeartBeatStart = true;
        if (this.isStartScheduled) {
            return;
        }
        this.isStartScheduled = true;
        this.scheduledThreadPool.scheduleWithFixedDelay(new Runnable() { // from class: com.keenon.sdk.sensor.callbell.SensorCallBell.2
            @Override // java.lang.Runnable
            public void run() {
                if (SensorCallBell.this.isHeartBeatStart) {
                    SensorCallBell.this.onPlayBellHeartBeat();
                }
            }
        }, 0L, 5L, TimeUnit.SECONDS);
    }

    public synchronized void onStopHeartBeat() {
        this.isHeartBeatStart = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void handleAck(MessageParam messageParam) {
        LogUtils.d(TAG, "received messageParam :" + messageParam.toString());
        int msgId = messageParam.getMsgId();
        String msgType = this.sendMsgMap.get(Integer.valueOf(msgId));
        if (msgType != null) {
            handlerRetryMap(msgType, msgId);
            this.sendMsgMap.remove(Integer.valueOf(msgId));
            switch (msgType) {
                case "initConfig":
                    notifyEvent(SensorEvent.REPLAY_CALL_BELL_INIT_ACK, messageParam);
                    break;
                case "updateRobotState":
                    notifyEvent(SensorEvent.REPLAY_CALL_BELL_UPDATE_STATE_ACK, messageParam);
                    break;
                case "replyTaskState":
                    notifyEvent(SensorEvent.REPLAY_CALL_BELL_TASK_STATE_ACK, messageParam);
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized boolean isContains(int msgId, int taskId) {
        RepeatMsgBean bean = new RepeatMsgBean(msgId, taskId);
        if (this.receivedMsgList.contains(bean)) {
            LogUtils.d(TAG, "received Msg Repeat : msgId = " + msgId + " taskId = " + taskId + "receivedMsgList size = " + this.receivedMsgList.size());
            return true;
        }
        if (this.receivedMsgList.size() >= 1024) {
            this.receivedMsgList.subList(0, 512).clear();
        }
        this.receivedMsgList.add(bean);
        return false;
    }

    private synchronized void putSendMsg(int msgKey, String Value) {
        if (this.sendMsgMap.size() >= 1024) {
            this.sendMsgMap.clear();
        }
        this.sendMsgMap.put(Integer.valueOf(msgKey), Value);
        LogUtils.d(TAG, "msgMap Size :" + this.sendMsgMap.size());
    }

    private void handlerRetryMap(String key, int msgID) {
        BellMSGBean bellMSGBean = this.retryMap.get(key);
        if (bellMSGBean != null && msgID == bellMSGBean.msgId) {
            this.retryMap.remove(key);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isMatchingSN(byte[] receivedSn) {
        return Arrays.equals(receivedSn, getSn());
    }

    private byte[] getSn() {
        if (this.sn == null || this.sn.length < 6) {
            this.sn = StringUtils.SNToByteArray(PeanutSNManagerUtils.getSN());
        }
        return this.sn;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized BellDeviceListBean getConvertDeviceList(BellDeviceListResult deviceListResult) {
        BellDeviceListBean listBean = new BellDeviceListBean();
        List<String> gatewaySnList = new ArrayList<>();
        List<String> relaySnList = new ArrayList<>();
        List<String> ScheduleSnList = new ArrayList<>();
        List<byte[]> list = getList(deviceListResult.getDataValue());
        LogUtils.d(TAG, "getConvertDeviceList Size:" + list.size());
        if (list.size() < 2) {
            return null;
        }
        for (int i = 0; i < list.size(); i++) {
            byte[] bytes = list.get(i);
            if (bytes.length == 7) {
                LogUtils.d(TAG, "getConvertDeviceList bytes :" + ByteUtils.bytesToHexStr(bytes));
                switch (bytes[0]) {
                    case 0:
                        gatewaySnList.add(StringUtils.StringToSN(ByteUtils.bytesToHexStr(bytes).substring(2)));
                        break;
                    case 1:
                        relaySnList.add(StringUtils.StringToSN(ByteUtils.bytesToHexStr(bytes).substring(2)));
                        break;
                    case 2:
                        ScheduleSnList.add(StringUtils.StringToSN(ByteUtils.bytesToHexStr(bytes).substring(2)));
                        break;
                }
            }
        }
        listBean.setGatewaySnList(gatewaySnList);
        listBean.setRelaySnList(relaySnList);
        listBean.setScheduleSnList(ScheduleSnList);
        return listBean;
    }

    public List<byte[]> getList(byte[] values) {
        List<byte[]> list = new ArrayList<>();
        int size = values.length / 7;
        for (int i = 0; i < size; i++) {
            byte[] tmp = new byte[7];
            System.arraycopy(values, i * 7, tmp, 0, 7);
            list.add(tmp);
        }
        return list;
    }
}
