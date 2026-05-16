package com.keenon.sdk.proxy.sender.anno;

import com.keenon.common.constant.PeanutConstants;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/proxy/sender/anno/LinkAdapter.class */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LinkAdapter {
    PeanutConstants.BoardType board() default PeanutConstants.BoardType.ROBOT;

    PeanutConstants.LinkType link() default PeanutConstants.LinkType.COM_COAP;

    String com() default "/dev/ttyS1";

    boolean custom() default false;
}
