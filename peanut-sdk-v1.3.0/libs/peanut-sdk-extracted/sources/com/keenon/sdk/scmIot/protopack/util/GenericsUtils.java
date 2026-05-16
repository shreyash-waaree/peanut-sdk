package com.keenon.sdk.scmIot.protopack.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/util/GenericsUtils.class */
public class GenericsUtils {
    public static Class getSuperClassGenricType(Class clazz, int index) {
        Type genType = clazz.getGenericSuperclass();
        if (!(genType instanceof ParameterizedType)) {
            return Object.class;
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        if (index >= params.length || index < 0) {
            throw new RuntimeException("你输入的索引" + (index < 0 ? "不能小于0" : "超出了参数的总数"));
        }
        if (!(params[index] instanceof Class)) {
            return Object.class;
        }
        return (Class) params[index];
    }

    public static Class getSuperClassGenricType(Class clazz) {
        return getSuperClassGenricType(clazz, 0);
    }

    public static Class getMethodGenericReturnType(Method method, int index) {
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) returnType;
            Type[] typeArguments = type.getActualTypeArguments();
            if (index >= typeArguments.length || index < 0) {
                throw new RuntimeException("你输入的索引" + (index < 0 ? "不能小于0" : "超出了参数的总数"));
            }
            if (typeArguments[index] instanceof ParameterizedType) {
                return (Class) ((ParameterizedType) typeArguments[index]).getRawType();
            }
            return (Class) typeArguments[index];
        }
        return Object.class;
    }

    public static Class getMethodGenericReturnType(Method method) {
        return getMethodGenericReturnType(method, 0);
    }

    public static List<Class> getMethodGenericParameterTypes(Method method, int index) {
        List<Class> results = new ArrayList<>();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (index >= genericParameterTypes.length || index < 0) {
            throw new RuntimeException("你输入的索引" + (index < 0 ? "不能小于0" : "超出了参数的总数"));
        }
        Type genericParameterType = genericParameterTypes[index];
        if (genericParameterType instanceof ParameterizedType) {
            ParameterizedType aType = (ParameterizedType) genericParameterType;
            Type[] parameterArgTypes = aType.getActualTypeArguments();
            for (Type parameterArgType : parameterArgTypes) {
                Class parameterArgClass = (Class) parameterArgType;
                results.add(parameterArgClass);
            }
            return results;
        }
        return results;
    }

    public static List<Class> getMethodGenericParameterTypes(Method method) {
        return getMethodGenericParameterTypes(method, 0);
    }

    public static Class getFieldGenericType(Field field, int index) {
        Type genericFieldType = field.getGenericType();
        if (genericFieldType instanceof ParameterizedType) {
            ParameterizedType aType = (ParameterizedType) genericFieldType;
            Type[] fieldArgTypes = aType.getActualTypeArguments();
            if (index >= fieldArgTypes.length || index < 0) {
                throw new RuntimeException("你输入的索引" + (index < 0 ? "不能小于0" : "超出了参数的总数"));
            }
            if (fieldArgTypes[index] instanceof ParameterizedType) {
                return (Class) ((ParameterizedType) fieldArgTypes[index]).getRawType();
            }
            return (Class) fieldArgTypes[index];
        }
        return Object.class;
    }

    public static Class getFieldGenericType(Field field) {
        return getFieldGenericType(field, 0);
    }

    public static GenericsType getGenericType(Type type) {
        return new GenericsType(type);
    }

    public static Class<?> getGenericClass(Class<?> clazz, int index) throws IndexOutOfBoundsException {
        Type genericSuperclass = clazz.getGenericSuperclass();
        while (true) {
            Type genType = genericSuperclass;
            if (!(genType instanceof ParameterizedType)) {
                if (clazz == Object.class) {
                    return Object.class;
                }
                clazz = clazz.getSuperclass();
                genericSuperclass = clazz.getGenericSuperclass();
            } else {
                Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
                if (index >= params.length || index < 0) {
                    throw new IndexOutOfBoundsException("Index: " + index + ", Size of Parameterized Type: " + params.length);
                }
                return (Class) params[index];
            }
        }
    }
}
