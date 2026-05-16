package com.keenon.sdk.component;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.api.RealTimeStatusApi;
import com.keenon.sdk.api.RobotDeviceTreeStatusApi;
import com.keenon.sdk.api.RobotEventApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.constant.TopicName;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IDeviceNodeCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/DeviceNodeComponent.class */
public class DeviceNodeComponent extends BaseComponent implements Component {
    private static final String ROBOT_BOTTOM_EVENT = "robot_bottom_event";
    private static final String ALL_NODES = "allNodes";
    private static final String CHANGE_NODES = "changeNodes";
    private static final int NORMAL = 0;
    private static final int WARNING = 1;
    private static final int ERROR = 2;
    private static final int INVALID = 3;
    private static final int APP_ROBOT_CONNECTION_ERROR_CODE = 202202;
    private static final String APP_ROBOT_CONNECTION_ERROR_INFO = "robot connection failed";
    private static final String TYPE_STAT = "STAT";
    private static final String TYPE_EVENT = "EVENT";
    public String info;
    private final ConcurrentHashMap<String, String> eventMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> deviceNodeMap = new ConcurrentHashMap<>();
    public String type = TYPE_STAT;
    public String id = "FN101";
    public String parent = "FN001";
    public int status = 0;
    public int code = 0;
    private IDeviceNodeCallback mIDeviceNodeCallback = null;
    private IDataCallback postCallBack = null;
    private final PeanutSDK.ErrorListener mErrorListener = errorCode -> {
        if (203101 == errorCode) {
            handleAPPConnectError();
        } else if (203102 == errorCode) {
            handleAPPConnectSuccess();
        }
    };
    private final IDataCallback eventCallback = new IDataCallback() { // from class: com.keenon.sdk.component.DeviceNodeComponent.1
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            try {
                String event = GsonUtil.getData(result);
                if (!event.isEmpty()) {
                    DeviceNodeComponent.this.handleEvent(event);
                }
            } catch (Exception e) {
                LogUtils.e(PeanutConstants.TAG_DEVICE_NODE, e.getMessage());
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            LogUtils.e(PeanutConstants.TAG_DEVICE_NODE, error.msg);
            if (DeviceNodeComponent.this.mIDeviceNodeCallback != null) {
                DeviceNodeComponent.this.mIDeviceNodeCallback.error(error);
            }
        }
    };
    private final IDataCallback getPostCallBack = new IDataCallback() { // from class: com.keenon.sdk.component.DeviceNodeComponent.3
        @Override // com.keenon.sdk.external.IDataCallback
        public void success(String result) {
            RealTimeStatusApi.Bean bean = (RealTimeStatusApi.Bean) GsonUtil.gson2Bean(result, RealTimeStatusApi.Bean.class);
            String str = GsonUtil.bean2String(bean.data);
            LogUtils.d(PeanutConstants.TAG_DEVICE_NODE, "[PostAutoCheck Success Data :" + str + "]");
            if (DeviceNodeComponent.this.postCallBack != null) {
                DeviceNodeComponent.this.postCallBack.success(str);
            }
        }

        @Override // com.keenon.sdk.external.IDataCallback
        public void error(ApiError error) {
            if (DeviceNodeComponent.this.postCallBack != null) {
                DeviceNodeComponent.this.postCallBack.error(error);
            }
        }
    };

    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_DEVICE_NODE.getComponentName();
    }

    public void addDeviceNodeListen(IDeviceNodeCallback callback) {
        this.mIDeviceNodeCallback = callback;
    }

    public void onStartUpload() {
        PeanutSDK.getInstance().subscribe(TopicName.ROBOT_EVENT, this.eventCallback);
        PeanutSDK.getInstance().registerListener(this.mErrorListener);
        RobotEventApi.Bean appEvent = getAPPEvent();
        handleEvent(GsonUtil.bean2String(appEvent.data));
    }

    public void onStartUpload(IDeviceNodeCallback callback) {
        addDeviceNodeListen(callback);
        onStartUpload();
    }

    public void onStopUpload() {
        PeanutSDK.getInstance().unSubscribe(TopicName.ROBOT_EVENT, this.eventCallback);
        PeanutSDK.getInstance().removeListener(this.mErrorListener);
    }

    public void getDeviceNodes(final String hashCode) {
        LogUtils.d(PeanutConstants.TAG_DEVICE_NODE, "[getDeviceNodes Hash Code :" + hashCode + "]");
        PeanutSDK.getInstance().device().getRobotDevicesTreeStatus(new IDataCallback() { // from class: com.keenon.sdk.component.DeviceNodeComponent.2
            @Override // com.keenon.sdk.external.IDataCallback
            public void success(String result) {
                try {
                    RobotDeviceTreeStatusApi.Bean bean = (RobotDeviceTreeStatusApi.Bean) GsonUtil.gson2Bean(result, RobotDeviceTreeStatusApi.Bean.class);
                    if (bean != null && bean.data != null && bean.data.size() > 0) {
                        bean.data.add(DeviceNodeComponent.this.getAppNode());
                        String deviceNodes = GsonUtil.bean2String(bean.data);
                        DeviceNodeComponent.this.handleNodesUpload(hashCode, deviceNodes);
                    }
                } catch (Exception e) {
                    LogUtils.e(PeanutConstants.TAG_DEVICE_NODE, e.getMessage());
                }
            }

            @Override // com.keenon.sdk.external.IDataCallback
            public void error(ApiError error) {
                LogUtils.e(PeanutConstants.TAG_DEVICE_NODE, error.msg);
                DeviceNodeComponent.this.handleNodesUpload(hashCode, "");
                if (DeviceNodeComponent.this.mIDeviceNodeCallback != null) {
                    DeviceNodeComponent.this.mIDeviceNodeCallback.error(error);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void handleEvent(String event) {
        String hashCode = getHashCodeStr(event.hashCode());
        this.eventMap.put(hashCode, event);
        getDeviceNodes(hashCode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void handleNodesUpload(String hashCode, String deviceNodes) {
        LogUtils.d(PeanutConstants.TAG_DEVICE_NODE, "[handleNodesUpl Hash Code :" + hashCode + "]");
        this.deviceNodeMap.put(ALL_NODES, deviceNodes);
        this.deviceNodeMap.put(CHANGE_NODES, this.eventMap.get(hashCode));
        if (this.mIDeviceNodeCallback != null) {
            this.mIDeviceNodeCallback.success(ROBOT_BOTTOM_EVENT, this.deviceNodeMap);
        }
        this.eventMap.remove(hashCode);
    }

    private void handleAPPConnectError() {
        this.status = 2;
        this.code = APP_ROBOT_CONNECTION_ERROR_CODE;
        this.info = APP_ROBOT_CONNECTION_ERROR_INFO;
        RobotDeviceTreeStatusApi.Bean bean = new RobotDeviceTreeStatusApi.Bean();
        bean.data = new ArrayList();
        bean.data.add(getAppNode());
        RobotEventApi.Bean appEvent = getAPPEvent();
        String hashCode = getHashCodeStr(appEvent.data.hashCode());
        this.eventMap.put(hashCode, GsonUtil.bean2String(appEvent.data));
        handleNodesUpload(hashCode, GsonUtil.bean2String(bean.data));
    }

    private void handleAPPConnectSuccess() {
        this.status = 0;
        this.code = 0;
        this.info = "";
        RobotEventApi.Bean appEvent = getAPPEvent();
        handleEvent(GsonUtil.bean2String(appEvent.data));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public RobotDeviceTreeStatusApi.Bean.DataBean getAppNode() {
        RobotDeviceTreeStatusApi.Bean.DataBean appNode = new RobotDeviceTreeStatusApi.Bean.DataBean();
        appNode.id = this.id;
        appNode.status = this.status;
        appNode.type = TYPE_STAT;
        appNode.parent = this.parent;
        return appNode;
    }

    private RobotEventApi.Bean getAPPEvent() {
        RobotEventApi.Bean.DataBean eventBean = new RobotEventApi.Bean.DataBean();
        RobotEventApi.Bean data = new RobotEventApi.Bean();
        data.data = new ArrayList();
        eventBean.code = this.code;
        eventBean.info = this.info;
        eventBean.parent = this.parent;
        eventBean.type = TYPE_EVENT;
        eventBean.id = this.id;
        eventBean.status = this.status;
        data.data.add(eventBean);
        return data;
    }

    private String getHashCodeStr(int hashCode) {
        long key = System.currentTimeMillis() + ((long) hashCode);
        return key + "";
    }

    public void PostAutoCheck(IDataCallback callback, String JsonString) {
        this.postCallBack = callback;
        PeanutSDK.getInstance().device().getAutoCheckStatus(this.getPostCallBack, JsonString);
    }

    public void PostAutoCheck(IDataCallback callback, int id, int sceneId) {
        this.postCallBack = callback;
        PeanutSDK.getInstance().device().getAutoCheckStatus(this.getPostCallBack, id, sceneId);
    }
}
