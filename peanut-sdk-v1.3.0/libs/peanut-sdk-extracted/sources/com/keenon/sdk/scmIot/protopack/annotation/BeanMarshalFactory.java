package com.keenon.sdk.scmIot.protopack.annotation;

import java.util.HashSet;
import java.util.Set;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/annotation/BeanMarshalFactory.class */
public class BeanMarshalFactory {
    public static Set<String> marshallBeanSet = new HashSet();

    public static boolean regist(String clazzName) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(clazzName);
        regist(clazz);
        return true;
    }

    public static boolean regist(Class<?> clazz) {
        BeanMarshalAno bean = (BeanMarshalAno) clazz.getAnnotation(BeanMarshalAno.class);
        if (bean == null) {
            return false;
        }
        marshallBeanSet.add(clazz.getName());
        return true;
    }

    public static boolean ifMarshallBeanCheck(Object marshallBean) {
        if (ifExistMarshallBean(marshallBean.getClass())) {
            return true;
        }
        return regist(marshallBean.getClass());
    }

    public static boolean ifExistMarshallBean(Class<?> clazz) {
        return marshallBeanSet.contains(clazz.getName());
    }
}
