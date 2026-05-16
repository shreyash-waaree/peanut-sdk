package com.keenon.sdk.hedera.model;

import com.keenon.common.error.PeanutError;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/hedera/model/ApiCode.class */
public enum ApiCode {
    COAP_RESPONSE_ERROR(PeanutError.COAP_RESPONSE_ERROR, "coap client or server error"),
    COAP_RESPONSE_EMPTY(PeanutError.COAP_RESPONSE_NULL_DATA, "coap response empty payload"),
    COAP_RESPONSE_TIMEOUT(PeanutError.COAP_RESPONSE_TIMEOUT, "coap response timeout"),
    HTTP_RESPONSE_ERROR(PeanutError.HTTP_RESPONSE_ERROR, "http error"),
    HTTP_RESPONSE_INVALID(PeanutError.HTTP_RESPONSE_PARSE_EXCEPTION, "http parse exception"),
    SCM_RESPONSE_EMPTY(PeanutError.SERIAL_RESPONSE_NULL_DATA, "serial response empty payload");

    public int code;
    public String msg;

    ApiCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
