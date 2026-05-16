package com.keenon.sdk.scmIot.protopack.marshal;

import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/JSONMarshal.class */
public class JSONMarshal extends StringMarshal {
    public JSONMarshal() {
    }

    public JSONMarshal(JSONObject json) {
        super(json.toString());
    }

    public JSONObject getJSONObject() {
        try {
            return new JSONObject(this.data);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    @Override // com.keenon.sdk.scmIot.protopack.marshal.StringMarshal
    public String toString() {
        return this.data;
    }
}
