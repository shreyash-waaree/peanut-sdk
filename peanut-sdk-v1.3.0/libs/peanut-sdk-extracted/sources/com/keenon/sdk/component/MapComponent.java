package com.keenon.sdk.component;

import com.keenon.sdk.api.MapCmdRequestApi;
import com.keenon.sdk.api.MapDownLoadActionV2Api;
import com.keenon.sdk.api.MapDownloadApi;
import com.keenon.sdk.api.MapDownloadNewApi;
import com.keenon.sdk.api.MapDownloadOptApi;
import com.keenon.sdk.api.MapDownloadV2Api;
import com.keenon.sdk.api.MapInfoApi;
import com.keenon.sdk.api.MapUploadApi;
import com.keenon.sdk.api.MapUploadObsApi;
import com.keenon.sdk.api.MapUploadOptApi;
import com.keenon.sdk.component.Component;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.IProgressCallback;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/MapComponent.class */
public class MapComponent extends BaseComponent implements Component {
    @Override // com.keenon.sdk.component.BaseComponent, com.keenon.sdk.component.Component
    public String getComponentName() {
        return Component.RobotComponentName.COMPONENT_MAP.getComponentName();
    }

    public void request(IDataCallback callBack, byte[] cmd) {
        new MapCmdRequestApi().send(callBack, cmd);
    }

    public void upload(IProgressCallback callBack, byte[] file) {
        new MapDownloadApi().send(callBack, file);
    }

    public void uploadNew(IProgressCallback callBack, byte[] file) {
        new MapDownloadNewApi().send(callBack, file);
    }

    public void uploadOpt(IProgressCallback callBack, byte[] file) {
        new MapDownloadOptApi().send(callBack, file);
    }

    public void download(IProgressCallback callBack) {
        new MapUploadApi().send(callBack);
    }

    public void downloadOpt(IDataCallback callBack) {
        new MapUploadOptApi().send(callBack);
    }

    public void uploadObs(IDataCallback callBack, byte[] file) {
        new MapUploadObsApi().send(callBack, file);
    }

    public void getMapInfo(IDataCallback callBack) {
        new MapInfoApi().send(callBack);
    }

    public void sendDownloadInfo(IDataCallback callBack, String md5, String url, String type, String mapUuid) {
        new MapDownloadV2Api().send(new MapDownloadV2Api.ParamSendDownloadInfo(md5, url, type, mapUuid), callBack);
    }

    public void sendMapDownLoadAction(IDataCallback callBack, int action) {
        new MapDownLoadActionV2Api().send(callBack, action);
    }
}
