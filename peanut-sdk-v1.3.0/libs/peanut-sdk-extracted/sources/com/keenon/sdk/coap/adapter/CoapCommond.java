package com.keenon.sdk.coap.adapter;

import com.keenon.sdk.hedera.model.MsgType;
import com.keenon.sdk.hedera.model.RequestEnum;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/coap/adapter/CoapCommond.class */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CoapCommond {
    String path();

    RequestEnum requestType() default RequestEnum.GET;

    MsgType msgType() default MsgType.CON;

    long timeout() default 60000;
}
