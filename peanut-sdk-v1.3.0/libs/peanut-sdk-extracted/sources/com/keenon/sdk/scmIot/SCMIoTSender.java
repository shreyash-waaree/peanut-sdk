package com.keenon.sdk.scmIot;

import android.util.Base64;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ByteUtils;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.common.utils.StringUtils;
import com.keenon.sdk.api.BottomRawDownApi;
import com.keenon.sdk.api.BottomRawDownNonApi;
import com.keenon.sdk.api.BottomRawUpApi;
import com.keenon.sdk.api.DevicesCheckApi;
import com.keenon.sdk.api.SCMThingModeV1Api;
import com.keenon.sdk.api.SCMThingModelV2Api;
import com.keenon.sdk.api.SCMThingUSBModelApi;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.scmIot.bean.SCMRequest;
import com.keenon.sdk.scmIot.bean.SCMResponse;
import com.keenon.sdk.scmIot.bean.response.BaseJsonContent;
import com.keenon.sdk.scmIot.bean.response.BellMacAddress;
import com.keenon.sdk.scmIot.bean.response.MotorStatus;
import com.keenon.sdk.scmIot.bean.response.PlateSensorGravityValue;
import com.keenon.sdk.scmIot.bean.response.PlateSensorLidarValue;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Packet;
import com.keenon.sdk.scmIot.protopack.base.ProtoHeader;
import com.keenon.sdk.scmIot.protopack.base.Unpack;
import com.keenon.sdk.scmIot.protopack.marshal.ObjectMarshal;
import com.keenon.sdk.scmIot.transfer.ISCMThingDataTransfer;
import com.keenon.sdk.scmIot.transfer.ReportFaultDataTransfer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/SCMIoTSender.class */
public class SCMIoTSender {
    private static final String TAG = "[SCMIoTSender]";
    private static final Map<String, IDataCallback> scmCallbackMap = new ConcurrentHashMap();
    private static final Map<Integer, ISCMThingDataTransfer> dataTransferMap = new ConcurrentHashMap();
    private static final Map<String, ISCMThingDataTransfer> dataTransferMapV2 = new ConcurrentHashMap();

    static {
        addSCMThingDataTransfer(1, new ReportFaultDataTransfer());
    }

    public static void addReportDataCallback(IDataCallback callback, int... devs) {
        if (null != devs) {
            for (int dev : devs) {
                scmCallbackMap.put(getCallbackTag(dev, 1, 1), callback);
            }
        }
    }

    public static void removeReportDataCallback(int... devs) {
        if (null != devs) {
            for (int dev : devs) {
                scmCallbackMap.remove(getCallbackTag(dev, 1, 1));
            }
        }
    }

    public static void addSingeReportDataCallback(IDataCallback callback, int dev, int topic, int event) {
        scmCallbackMap.put(getCallbackTag(dev, topic, event), callback);
    }

    public static void removeSingeReportDataCallback(int dev, int topic, int event) {
        scmCallbackMap.remove(getCallbackTag(dev, topic, event));
    }

    public static void addSCMThingDataTransfer(int topic, ISCMThingDataTransfer transfer) {
        dataTransferMap.put(Integer.valueOf(topic), transfer);
    }

    public static void removeSCMThingDataTransfer(int topic) {
        if (dataTransferMap.containsKey(Integer.valueOf(topic))) {
            dataTransferMap.remove(Integer.valueOf(topic));
        }
    }

    public static void addSCMThingDataTransferV2(int dev, int topic, int event, ISCMThingDataTransfer transfer) {
        dataTransferMapV2.put(getCallbackTag(dev, topic, event), transfer);
    }

    public static void removeSCMThingDataTransferV2(int dev, int topic, int event) {
        dataTransferMapV2.remove(getCallbackTag(dev, topic, event));
    }

    public static void sendRequest(SCMRequest request, IDataCallback callBack) {
        if (request == null) {
            LogUtils.d(PeanutConstants.TAG_API, "[SCMIoTSender][sendRequest][null request]");
        } else {
            LogUtils.d(PeanutConstants.TAG_API, "[SCMIoTSender][sendRequest][" + request.toString() + "]");
            sendRequest(request, prepareRequest(request.getDev(), request.getTopic(), request.getType(), request.getCmd(), request.getParams()), callBack);
        }
    }

    private static Packet prepareRequest(int dev, int topic, int type, int cmd, Object params) {
        ObjectMarshal payloadMarshal = null;
        int length = 0;
        if (params != null) {
            payloadMarshal = new ObjectMarshal(params);
            Pack headerPack = new Pack();
            payloadMarshal.marshal(headerPack);
            length = headerPack.getBytes().length;
        }
        ProtoHeader header = new ProtoHeader();
        header.setDev(dev);
        header.setTopic(topic);
        header.setType(type);
        header.setCmd(cmd);
        header.setDataLength(length);
        Packet packet = new Packet();
        packet.setHeader(header);
        if (payloadMarshal != null) {
            packet.putContent(payloadMarshal);
        }
        packet.marshal();
        return packet;
    }

    private static void sendRequest(SCMRequest request, Packet packet, IDataCallback callBack) {
        LogUtils.d(PeanutConstants.TAG_API, "[SCMIoTSender][sendRequest][request in hex: " + ByteUtils.bytesToHexStr(packet.getBytes()) + "]");
        if (request.isUSBDirect()) {
            scmCallbackMap.put(getCallbackTag(request), callBack);
            new SCMThingUSBModelApi().send(callBack, ByteUtils.ByteToObject(packet.getBytes()));
            return;
        }
        if (request.isSerialDirect()) {
            scmCallbackMap.put(getCallbackTag(request), callBack);
            if ("v1.0".equals(request.getSerialProtocolVer())) {
                new SCMThingModeV1Api().send(callBack, ByteUtils.ByteToObject(packet.getBytes()));
                return;
            } else {
                new SCMThingModelV2Api().send(callBack, ByteUtils.ByteToObject(packet.getBytes()));
                return;
            }
        }
        String payload = Base64.encodeToString(packet.getBytes(), 2);
        LogUtils.d(PeanutConstants.TAG_API, "[SCMIoTSender][sendRequest][request base64: " + payload + ", length: " + payload.length() + "]");
        HashMap<String, Object> params = new HashMap<>();
        params.put("trans", payload);
        params.put("type", Integer.valueOf(request.getChannel()));
        if (request.isCon()) {
            new BottomRawDownApi().send(callBack, params);
        } else {
            new BottomRawDownNonApi().send(callBack, params);
        }
    }

    public static String receiveResponse(String rawData) {
        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][receiveResponse][packet base64: " + rawData + "]");
        try {
            byte[] raw = Base64.decode(rawData, 2);
            LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][receiveResponse][packet in hex: " + ByteUtils.bytesToHexStr(raw) + "]");
            LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][receiveResponse][packet data l: " + raw.length + "]");
            String responseString = receiveResponse(raw);
            LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][receiveResponse][packet in json: " + responseString + "]");
            return responseString;
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, "[SCMIoTSender][receiveResponse][exception:" + e.toString() + "]");
            return "";
        }
    }

    public static String receiveResponse(byte[] raw) {
        String response = "";
        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][receiveResponse][raw size :" + raw.length + "]");
        if (raw.length < 8) {
            LogUtils.e(PeanutConstants.TAG_UTIL, "[SCMIoTSender][parseResponse][invalid header]");
            return response;
        }
        try {
            ProtoHeader header = new ProtoHeader();
            Unpack unpack = new Unpack(raw);
            Packet packet = new Packet(header, unpack);
            packet.unmarshal();
            SCMResponse bean = new SCMResponse();
            bean.setCode(0);
            bean.setStatus(0);
            bean.setMsg(PeanutConstants.API_SUCCESS_MESSAGE);
            bean.setTopic(BottomRawUpApi.class);
            if (header.getDataLength() > 0) {
                byte[] data = Arrays.copyOfRange(raw, 8, raw.length);
                if (header.getTopic() == 6) {
                    LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][parseResponse motion][data in hex: " + ByteUtils.bytesToHexStr(data) + "]");
                    Unpack dataUnpack = new Unpack(data);
                    MotorStatus outputBean = new MotorStatus();
                    ObjectMarshal outputMarshal = new ObjectMarshal(outputBean);
                    outputMarshal.unmarshal(dataUnpack);
                    SCMResponse.DataBean<MotorStatus> dataBean = new SCMResponse.DataBean<>(header);
                    dataBean.setData(outputBean);
                    bean.setData(dataBean);
                } else {
                    if (header.getTopic() == 8) {
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][parseResponse check][data in hex: " + ByteUtils.bytesToHexStr(data) + "]");
                        Unpack dataUnpack2 = new Unpack(data);
                        BaseJsonContent outputBean2 = new BaseJsonContent();
                        ObjectMarshal outputMarshal2 = new ObjectMarshal(outputBean2);
                        outputMarshal2.unmarshal(dataUnpack2);
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][parseResponse check][" + outputBean2.getJson() + "][length=" + outputBean2.getJson().length() + "]");
                        String json = outputBean2.getJson();
                        DevicesCheckApi.Bean deviceBean = (DevicesCheckApi.Bean) GsonUtil.gson2Bean(json, DevicesCheckApi.Bean.class);
                        deviceBean.setTopic(DevicesCheckApi.class);
                        return GsonUtil.bean2String(deviceBean);
                    }
                    if (header.getTopic() == 12) {
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][parseResponse gravity][data in hex: " + ByteUtils.bytesToHexStr(data) + "]");
                        Unpack dataUnpack3 = new Unpack(data);
                        PlateSensorGravityValue outputBean3 = new PlateSensorGravityValue();
                        ObjectMarshal outputMarshal3 = new ObjectMarshal(outputBean3);
                        outputMarshal3.unmarshal(dataUnpack3);
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][parseResponse gravity][" + outputBean3.toString() + "]");
                        bean.setTopic(SCMThingModelV2Api.class);
                        onResponse(GsonUtil.bean2String(outputBean3), header);
                    } else if (header.getTopic() == 13 && header.getDev() == 15) {
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][parseResponse lidar][data in hex: " + ByteUtils.bytesToHexStr(data) + "]");
                        Unpack dataUnpack4 = new Unpack(data);
                        PlateSensorLidarValue outputBean4 = new PlateSensorLidarValue();
                        ObjectMarshal outputMarshal4 = new ObjectMarshal(outputBean4);
                        outputMarshal4.unmarshal(dataUnpack4);
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][parseResponse lidar][" + outputBean4.toString() + "]");
                        bean.setTopic(SCMThingModelV2Api.class);
                        onResponse(GsonUtil.bean2String(outputBean4), header);
                    } else if (header.getTopic() == 9 || header.getTopic() == 10) {
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][parseResponse bell][data in hex: " + ByteUtils.bytesToHexStr(data) + "]");
                        Unpack dataUnpack5 = new Unpack(data);
                        BellMacAddress bellMacAddress = new BellMacAddress();
                        ObjectMarshal outputMarshal5 = new ObjectMarshal(bellMacAddress);
                        outputMarshal5.unmarshal(dataUnpack5);
                        SCMResponse.DataBean<BellMacAddress> dataBean2 = new SCMResponse.DataBean<>(header);
                        dataBean2.setData(bellMacAddress);
                        bean.setData(dataBean2);
                    } else {
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][common parseResponse][data in hex: " + ByteUtils.bytesToHexStr(data) + "]");
                        ISCMThingDataTransfer transfer = dataTransferMapV2.get(getCallbackTag(header));
                        if (transfer == null) {
                            transfer = dataTransferMap.get(Integer.valueOf(header.getTopic()));
                        }
                        Object outputBean5 = null;
                        if (null != transfer) {
                            outputBean5 = transfer.dataTransfer(header, data);
                        }
                        bean.setTopic(SCMThingModelV2Api.class);
                        String resultJson = GsonUtil.bean2String(outputBean5);
                        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][common parseResponse][" + resultJson + "]");
                        onResponse(resultJson, header);
                    }
                }
            } else {
                bean.setData(new SCMResponse.DataBean(header));
                String resultJson2 = GsonUtil.bean2String(bean);
                LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][common parseResponse][" + resultJson2 + "]");
                onResponse(resultJson2, header);
            }
            response = GsonUtil.bean2String(bean);
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, "[SCMIoTSender][parseResponse][exception: " + e.toString() + "]");
        }
        LogUtils.d(PeanutConstants.TAG_UTIL, "[SCMIoTSender][receiveResponse][" + response + "]");
        return response;
    }

    private static void onResponse(String response, ProtoHeader header) {
        IDataCallback callback = matchResponse(header);
        if (callback == null || StringUtils.isEmpty(response)) {
            LogUtils.e(PeanutConstants.TAG_UTIL, "[SCMIoTSender][onResponse][null callback or empty response]");
        } else {
            callback.success(response);
        }
    }

    private static IDataCallback matchResponse(ProtoHeader header) {
        for (String key : scmCallbackMap.keySet()) {
            if (getCallbackTag(header.getDev(), header.getTopic(), header.getType()).equals(key)) {
                IDataCallback callback = scmCallbackMap.get(key);
                return callback;
            }
        }
        return null;
    }

    private static String getCallbackTag(SCMRequest request) {
        return getCallbackTag(request.getDev(), request.getTopic(), request.getType());
    }

    private static String getCallbackTag(int dev, int topic, int type) {
        return dev + String.valueOf(topic) + type;
    }

    private static String getCallbackTag(ProtoHeader header) {
        return getCallbackTag(header.getDev(), header.getTopic(), header.getType());
    }
}
