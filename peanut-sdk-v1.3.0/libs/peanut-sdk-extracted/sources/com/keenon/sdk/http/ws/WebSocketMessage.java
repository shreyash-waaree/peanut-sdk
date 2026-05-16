package com.keenon.sdk.http.ws;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/WebSocketMessage.class */
public class WebSocketMessage<T> {
    private String action;
    private WebSocketMessage<T>.DataBean<T> data;
    private String msgId;

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public WebSocketMessage<T>.DataBean<T> getData() {
        return this.data;
    }

    public void setData(WebSocketMessage<T>.DataBean<T> data) {
        this.data = data;
    }

    public String getMsgId() {
        return this.msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/ws/WebSocketMessage$DataBean.class */
    public class DataBean<T> {
        private String api;
        private T value;

        public DataBean() {
        }

        public String getApi() {
            return this.api;
        }

        public void setApi(String api) {
            this.api = api;
        }

        public T getValue() {
            return this.value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }

    public String toString() {
        return "WebSocketMessage{action='" + this.action + "', data=" + this.data.toString() + ", msgId='" + this.msgId + "'}";
    }
}
