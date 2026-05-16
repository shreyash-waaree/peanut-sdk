package com.keenon.sdk.http.adapter;

import com.keenon.sdk.hedera.model.RequestEnum;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/http/adapter/HttpCommand.class */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpCommand {
    String path();

    boolean shouldCache() default false;

    RequestEnum requestType() default RequestEnum.GET;

    String contentType() default "default";

    long interval() default 5000;
}
