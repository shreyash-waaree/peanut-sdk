package com.keenon.sdk.sensor.common;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/sensor/common/Event.class */
public class Event {
    private String name;
    private String scope;
    private String src;
    private Object data;

    public Event() {
    }

    public Event(String name, String scope, String src, Object data) {
        this.name = name;
        this.scope = scope;
        this.src = src;
        this.data = data;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return this.scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSrc() {
        return this.src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public Object getData() {
        return this.data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String toString() {
        return "Event{name='" + this.name + "', scope='" + this.scope + "', src='" + this.src + "', data=" + this.data + '}';
    }
}
