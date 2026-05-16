package com.keenon.common.utils;

import com.keenon.common.constant.PeanutConstants;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/common/utils/ReflectUtil.class */
public class ReflectUtil {
    private static final String TAG = "[ReflectUtil]";

    public static String getClassName(Class clz) {
        String tag;
        String clzName = clz.getName();
        if (clzName.contains("$")) {
            tag = clzName.substring(clzName.lastIndexOf(".") + 1, clzName.lastIndexOf("$"));
        } else {
            tag = clzName.substring(clzName.lastIndexOf(".") + 1);
        }
        return tag;
    }

    public static Object invokeStaticMethod(String classname, String methodName, Class[] pTypes, Object[] values) {
        try {
            Class<?> cls = Class.forName(classname);
            Method mthd = cls.getMethod(methodName, pTypes);
            return mthd.invoke(cls, values);
        } catch (Exception e) {
            LogUtils.e(PeanutConstants.TAG_UTIL, "[ReflectUtil][invokeStaticMethod" + methodName + "]{exception:" + e.toString());
            return null;
        }
    }

    public static Object executeObjectMethod(Object object, Class<?> srcClass, Class<?>[] paramClasses, String methodName, Object... args) {
        Object result = null;
        try {
            Method method = srcClass.getMethod(methodName, paramClasses);
            result = method.invoke(object, args);
        } catch (IllegalAccessException e) {
            log(e);
        } catch (NoSuchMethodException e2) {
            log(e2);
        } catch (InvocationTargetException e3) {
            log(e3);
        }
        return result;
    }

    public static Object getInstance(String clsName, Class[] types, Object[] pVaules) {
        Object obj = null;
        try {
            Class<?> cls = Class.forName(clsName);
            if ((pVaules == null || pVaules.length == 0) && (types == null || types.length == 0)) {
                Constructor<?> declaredConstructor = cls.getDeclaredConstructor(new Class[0]);
                declaredConstructor.setAccessible(true);
                obj = declaredConstructor.newInstance(new Object[0]);
            } else {
                Constructor<?> declaredConstructor2 = cls.getDeclaredConstructor(types);
                declaredConstructor2.setAccessible(true);
                obj = declaredConstructor2.newInstance(pVaules);
            }
        } catch (ClassNotFoundException e) {
            log(e);
        } catch (IllegalAccessException e2) {
            log(e2);
        } catch (InstantiationException e3) {
            log(e3);
        } catch (NoSuchMethodException e4) {
            log(e4);
        } catch (InvocationTargetException e5) {
            log(e5);
        }
        return obj;
    }

    private static void log(Exception e) {
        LogUtils.e(PeanutConstants.TAG_UTIL, TAG, e);
    }
}
