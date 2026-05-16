package com.keenon.sdk.proxy.adapter;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.external.PeanutConfig;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.hedera.base.ILinkAdapter;
import com.keenon.sdk.http.adapter.HttpLinkAdapter;
import com.keenon.sdk.serial.adapter.SerialLinkAdapter;
import java.util.HashMap;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/proxy/adapter/LinkAdapterFactory.class */
public class LinkAdapterFactory {
    private static final String TAG = "[LinkAdapterFactory]";
    private static volatile LinkAdapterFactory instance;
    private static HashMap<String, ILinkAdapter> linkAdapterHashMap = new HashMap<>();
    private PeanutConstants.LinkType defaultLinkType;
    private PeanutConstants.LinkType curLinkType;
    private String defaultSerialPath;
    private String curSerialPath;
    private String comRobot;
    private String comEmotion;
    private String comDoor;
    private String comLight;
    private ILinkAdapter linkAdapter;

    LinkAdapterFactory() {
    }

    public static LinkAdapterFactory getInstance() {
        if (instance == null) {
            synchronized (LinkAdapterFactory.class) {
                if (instance == null) {
                    instance = new LinkAdapterFactory();
                }
            }
        }
        return instance;
    }

    public ILinkAdapter create(PeanutConstants.BoardType boardType, PeanutConstants.LinkType linkType, String serialPath, boolean isCustom) {
        ILinkAdapter iLinkAdapter;
        ILinkAdapter linkAdapter;
        synchronized (this) {
            resetConfig();
            String key = applyConfig(boardType, linkType, serialPath, isCustom);
            if (linkAdapterHashMap.get(key) == null && (linkAdapter = build()) != null) {
                LogUtils.d(PeanutConstants.TAG_SDK, "[LinkAdapterFactory][create][boardType=" + boardType.name() + ",linkType=" + linkType.name() + ",serialPath=" + serialPath + ",isCustom=" + isCustom + "]");
                LogUtils.d(PeanutConstants.TAG_SDK, "[LinkAdapterFactory][create][key=" + key + "]");
                linkAdapterHashMap.put(key, linkAdapter);
            }
            iLinkAdapter = linkAdapterHashMap.get(key);
        }
        return iLinkAdapter;
    }

    public synchronized void release() {
        for (ILinkAdapter linkAdapter : linkAdapterHashMap.values()) {
            if (linkAdapter != null) {
                LogUtils.i(PeanutConstants.TAG_SDK, "[LinkAdapterFactory][release][adapter=" + linkAdapter + "]");
                linkAdapter.release();
            }
        }
        linkAdapterHashMap.clear();
    }

    private void resetConfig() {
        this.defaultLinkType = PeanutConfig.getLinkType();
        this.comRobot = PeanutConfig.getLinkCom();
        this.comEmotion = PeanutConfig.getEmotionLinkCOM();
        this.comDoor = PeanutConfig.getDoorLinkCOM();
        this.comLight = PeanutConfig.getLightLinkCOM();
        this.defaultSerialPath = this.comRobot;
        LogUtils.d(PeanutConstants.TAG_SDK, "[LinkAdapterFactory][resetConfig][comRobot=" + this.comRobot + ",comEmotion=" + this.comEmotion + ",comLight=" + this.comLight + ",comDoor=" + this.comDoor + ",defaultLinkType=" + this.defaultLinkType.name() + ",defaultSerialPath=" + this.defaultSerialPath + "]");
        this.curLinkType = this.defaultLinkType;
        this.curSerialPath = this.defaultSerialPath;
    }

    private String applyConfig(PeanutConstants.BoardType boardType, PeanutConstants.LinkType linkType, String serialPath, boolean isCustom) {
        if (isCustom) {
            this.curLinkType = linkType;
            this.curSerialPath = serialPath;
        } else {
            selectSerialPath(boardType);
        }
        checkInvalidateOfPath();
        return boardType.name() + this.curLinkType.name() + this.curSerialPath;
    }

    private void selectSerialPath(PeanutConstants.BoardType boardType) {
        switch (boardType) {
            case ROBOT:
                this.curSerialPath = this.comRobot;
                break;
            case EMOTION:
                this.curSerialPath = this.comEmotion;
                this.curLinkType = PeanutConstants.LinkType.COM;
                break;
            case LIGHT:
                this.curSerialPath = this.comLight;
                this.curLinkType = PeanutConstants.LinkType.COM;
                break;
            case DOOR:
                this.curSerialPath = this.comDoor;
                this.curLinkType = PeanutConstants.LinkType.COM;
                break;
            case DEFAULT:
            default:
                LogUtils.w(PeanutConstants.TAG_SDK, "[LinkAdapterFactory][selectSerialPath][unknown board type]");
                break;
        }
    }

    private void checkInvalidateOfPath() {
        if (null != this.curSerialPath && !this.curSerialPath.isEmpty()) {
            if (!this.curSerialPath.startsWith(PeanutConstants.COM_PREFIX)) {
                this.curSerialPath = PeanutConstants.COM_PREFIX + this.curSerialPath;
                return;
            }
            return;
        }
        this.curSerialPath = PeanutConfig.getLinkCom();
    }

    private ILinkAdapter getLinkAdapter() {
        return this.linkAdapter;
    }

    public String getSerialPath() {
        return this.curSerialPath;
    }

    public ILinkAdapter build() {
        switch (this.curLinkType) {
            case COAP:
                this.linkAdapter = new CoapLinkAdapter();
                break;
            case COM:
                this.linkAdapter = new SerialLinkAdapter(this.curSerialPath);
                break;
            case COM_COAP:
                this.linkAdapter = new Coap4SerialLinkAdapter(this.curSerialPath);
                break;
            case HTTP:
                this.linkAdapter = new HttpLinkAdapter();
                break;
            case DEFAULT:
            default:
                LogUtils.w(PeanutConstants.TAG_SDK, "[LinkAdapterFactory][build][unknown link type]");
                break;
        }
        return this.linkAdapter;
    }
}
