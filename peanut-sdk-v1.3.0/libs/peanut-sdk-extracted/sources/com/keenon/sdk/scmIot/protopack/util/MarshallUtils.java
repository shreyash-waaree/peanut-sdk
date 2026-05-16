package com.keenon.sdk.scmIot.protopack.util;

import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;
import com.keenon.sdk.scmIot.protopack.exception.PackException;
import com.keenon.sdk.scmIot.protopack.exception.UnpackException;
import com.keenon.sdk.scmIot.protopack.marshal.BooleanMarshal;
import com.keenon.sdk.scmIot.protopack.marshal.ByteMarshal;
import com.keenon.sdk.scmIot.protopack.marshal.FloatMarshal;
import com.keenon.sdk.scmIot.protopack.marshal.IntegerMarshal;
import com.keenon.sdk.scmIot.protopack.marshal.LongMarshal;
import com.keenon.sdk.scmIot.protopack.marshal.ShortMarshal;
import com.keenon.sdk.scmIot.protopack.marshal.StringMarshal;
import com.keenon.sdk.scmIot.protopack.marshal.UintMarshal;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/util/MarshallUtils.class */
public class MarshallUtils {
    private static Set<Type> supported = new HashSet();

    static {
        supported.add(Integer.TYPE);
        supported.add(Short.TYPE);
        supported.add(Long.TYPE);
        supported.add(Float.TYPE);
        supported.add(Byte.TYPE);
        supported.add(Boolean.TYPE);
        supported.add(String.class);
        supported.add(Integer.class);
        supported.add(Short.class);
        supported.add(Long.class);
        supported.add(Float.class);
        supported.add(Byte.class);
        supported.add(Boolean.class);
        supported.add(Uint.class);
        supported.add(Ulong.class);
        supported.add(Ushort.class);
        supported.add(List.class);
        supported.add(Map.class);
    }

    public static Set<Type> getSupportedTypes() {
        return supported;
    }

    public static void validateClass(Class<?> clazz) {
        if (!getSupportedTypes().contains(clazz) && !Marshallable.class.isAssignableFrom(clazz)) {
            throw new PackException("pack type not supported : " + clazz);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static <T> void packSimpleType(Pack pack, T t, Class<T> clazz) {
        if (t == 0) {
            return;
        }
        validateClass(clazz);
        if (clazz.isPrimitive()) {
            if (Short.TYPE.isAssignableFrom(clazz)) {
                pack.putShort(((Short) t).shortValue());
                return;
            }
            if (Integer.TYPE.isAssignableFrom(clazz)) {
                pack.putInt(((Integer) t).intValue());
                return;
            }
            if (Long.TYPE.isAssignableFrom(clazz)) {
                pack.putLong(((Long) t).longValue());
                return;
            } else if (Boolean.TYPE.isAssignableFrom(clazz)) {
                pack.putBoolean(((Boolean) t).booleanValue());
                return;
            } else {
                if (Byte.TYPE.isAssignableFrom(clazz)) {
                    pack.putByte(((Byte) t).byteValue());
                    return;
                }
                return;
            }
        }
        if (String.class.isAssignableFrom(clazz)) {
            pack.putVarstr((String) t);
        } else if (Short.class.isAssignableFrom(clazz)) {
            pack.putShort(((Short) t).shortValue());
        } else if (Integer.class.isAssignableFrom(clazz)) {
            pack.putInt(((Integer) t).intValue());
        } else if (Long.class.isAssignableFrom(clazz)) {
            pack.putLong(((Long) t).longValue());
        } else if (Boolean.class.isAssignableFrom(clazz)) {
            pack.putBoolean(((Boolean) t).booleanValue());
        } else if (Byte.class.isAssignableFrom(clazz)) {
            pack.putByte(((Byte) t).byteValue());
        } else if (Uint.class.isAssignableFrom(clazz)) {
            pack.putUInt(new Uint(t.toString()));
        } else if (Ulong.class.isAssignableFrom(clazz)) {
            pack.putUlong(new Ulong(t.toString()));
        } else if (Ushort.class.isAssignableFrom(clazz)) {
            pack.putUshort(new Ushort(t.toString()));
        }
        if (Marshallable.class.isAssignableFrom(clazz)) {
            try {
                pack.putMarshallable((Marshallable) t);
            } catch (Exception e) {
                throw new PackException(e);
            }
        }
    }

    public static <T> void packList(Pack pack, List<T> list, Class<T> clazz) {
        validateClass(clazz);
        if (list == null || list.isEmpty()) {
            pack.putInt(0);
            return;
        }
        pack.putInt(list.size());
        for (int i = 0; i < list.size(); i++) {
            packSimpleType(pack, list.get(i), clazz);
        }
    }

    public static <K, V> void packMap(Pack pack, Map<K, V> map, Class<K> primitiveClazzK, Class<V> primitiveClazzV) {
        validateClass(primitiveClazzK);
        validateClass(primitiveClazzV);
        if (map == null || map.size() == 0) {
            pack.putInt(0);
            return;
        }
        pack.putInt(map.size());
        Set<K> keys = map.keySet();
        for (K key : keys) {
            packSimpleType(pack, key, primitiveClazzK);
            packSimpleType(pack, map.get(key), primitiveClazzV);
        }
    }

    public static void packMarshallable(Pack pack, Marshallable mar) {
        if (mar != null) {
            try {
                pack.putMarshallable(mar);
            } catch (Exception e) {
                throw new PackException(e);
            }
        }
    }

    public static <T> T unpackSimpleType(Unpack unpack, Class<T> cls) {
        validateClass(cls);
        Object objPopUshort = null;
        if (cls.isPrimitive()) {
            if (Short.TYPE.isAssignableFrom(cls)) {
                objPopUshort = unpack.popShort();
            } else if (Integer.TYPE.isAssignableFrom(cls)) {
                objPopUshort = unpack.popInt();
            } else if (Long.TYPE.isAssignableFrom(cls)) {
                objPopUshort = unpack.popLong();
            } else if (Float.TYPE.isAssignableFrom(cls)) {
                objPopUshort = Float.valueOf(unpack.popFloat());
            } else if (Byte.TYPE.isAssignableFrom(cls)) {
                objPopUshort = unpack.popByte();
            } else if (Boolean.TYPE.isAssignableFrom(cls)) {
                objPopUshort = unpack.popBoolean();
            }
        } else {
            if (String.class.isAssignableFrom(cls)) {
                objPopUshort = cls.cast(unpack.popVarstr());
            }
            if (Short.class.isAssignableFrom(cls)) {
                objPopUshort = cls.cast(unpack.popShort());
            } else if (Integer.class.isAssignableFrom(cls)) {
                objPopUshort = cls.cast(unpack.popInt());
            } else if (Long.class.isAssignableFrom(cls)) {
                objPopUshort = cls.cast(unpack.popLong());
            } else if (Float.class.isAssignableFrom(cls)) {
                objPopUshort = cls.cast(Float.valueOf(unpack.popFloat()));
            } else if (Byte.class.isAssignableFrom(cls)) {
                objPopUshort = cls.cast(unpack.popByte());
            } else if (Boolean.class.isAssignableFrom(cls)) {
                objPopUshort = cls.cast(unpack.popBoolean());
            } else if (Uint.class.isAssignableFrom(cls)) {
                objPopUshort = cls.cast(unpack.popUInt());
            } else if (Ulong.class.isAssignableFrom(cls)) {
                objPopUshort = unpack.popUlong();
            } else if (Ushort.class.isAssignableFrom(cls)) {
                objPopUshort = unpack.popUshort();
            }
            if (Marshallable.class.isAssignableFrom(cls)) {
                try {
                    Marshallable marshallable = (Marshallable) cls.newInstance();
                    unpack.popMarshallable(marshallable);
                    return cls.cast(marshallable);
                } catch (IllegalAccessException e) {
                    throw new UnpackException(e);
                } catch (InstantiationException e2) {
                    throw new UnpackException(e2);
                } catch (Exception e3) {
                    throw new UnpackException(e3);
                }
            }
        }
        return (T) objPopUshort;
    }

    public static <T> List<T> unpackList(Unpack unpack, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        int size = unpack.popInt().intValue();
        if (size == 0) {
            return list;
        }
        for (int i = 0; i < size; i++) {
            list.add(clazz.cast(unpackSimpleType(unpack, clazz)));
        }
        return list;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static <K, V> Map<K, V> unpackMap(Unpack unpack, Class<K> primitiveClazzK, Class<V> primitiveClazzV, boolean ordered) {
        Map<K, V> map;
        if (ordered) {
            map = new LinkedHashMap<>();
        } else {
            map = new HashMap<>();
        }
        int size = unpack.popInt().intValue();
        if (size == 0) {
            return map;
        }
        for (int i = 0; i < size; i++) {
            map.put(unpackSimpleType(unpack, primitiveClazzK), unpackSimpleType(unpack, primitiveClazzV));
        }
        return map;
    }

    public static <T extends Marshallable> T unpackMarshall(Unpack unpack, Class<T> marClazz) {
        try {
            Marshallable mar = marClazz.newInstance();
            unpack.popMarshallable(mar);
            return marClazz.cast(mar);
        } catch (IllegalAccessException e) {
            throw new PackException(e);
        } catch (InstantiationException e2) {
            throw new PackException(e2);
        } catch (Exception e3) {
            throw new PackException(e3);
        }
    }

    public static List<Marshallable> toMarshallable(List<Class<?>> clazzs) {
        List<Marshallable> list = new ArrayList<>();
        for (Class<?> clazz : clazzs) {
            if (!getSupportedTypes().contains(clazz) && !Marshallable.class.isAssignableFrom(clazz)) {
                throw new PackException("MarshllUtils toMarshallable type not supported : " + clazz);
            }
            try {
                if (clazz.isPrimitive()) {
                    if (Short.TYPE.isAssignableFrom(clazz)) {
                        list.add((Marshallable) ShortMarshal.class.newInstance());
                    } else if (Integer.TYPE.isAssignableFrom(clazz)) {
                        list.add((Marshallable) IntegerMarshal.class.newInstance());
                    } else if (Long.TYPE.isAssignableFrom(clazz)) {
                        list.add((Marshallable) LongMarshal.class.newInstance());
                    } else if (Float.TYPE.isAssignableFrom(clazz)) {
                        list.add((Marshallable) FloatMarshal.class.newInstance());
                    } else if (Boolean.TYPE.isAssignableFrom(clazz)) {
                        list.add((Marshallable) BooleanMarshal.class.newInstance());
                    } else if (Byte.TYPE.isAssignableFrom(clazz)) {
                        list.add((Marshallable) ByteMarshal.class.newInstance());
                    }
                } else if (String.class.isAssignableFrom(clazz)) {
                    list.add((Marshallable) StringMarshal.class.newInstance());
                } else if (Short.class.isAssignableFrom(clazz)) {
                    list.add((Marshallable) ShortMarshal.class.newInstance());
                } else if (Integer.class.isAssignableFrom(clazz)) {
                    list.add((Marshallable) IntegerMarshal.class.newInstance());
                } else if (Long.class.isAssignableFrom(clazz)) {
                    list.add((Marshallable) LongMarshal.class.newInstance());
                } else if (Float.class.isAssignableFrom(clazz)) {
                    list.add((Marshallable) FloatMarshal.class.newInstance());
                } else if (Boolean.class.isAssignableFrom(clazz)) {
                    list.add((Marshallable) BooleanMarshal.class.newInstance());
                } else if (Byte.class.isAssignableFrom(clazz)) {
                    list.add((Marshallable) ByteMarshal.class.newInstance());
                } else if (Uint.class.isAssignableFrom(clazz)) {
                    list.add((Marshallable) UintMarshal.class.newInstance());
                } else if (Marshallable.class.isAssignableFrom(clazz)) {
                    Marshallable mar = (Marshallable) clazz.newInstance();
                    list.add(mar);
                } else {
                    throw new PackException("MarshllUtils toMarshallable clazz is not simple type or marshallable");
                }
            } catch (Exception e) {
                throw new PackException("MarshllUtils toMarshallable error!", e);
            }
        }
        return list;
    }

    public static <T> T unpackObject(Unpack unpack, Type type) {
        Class cls;
        Type[] actualTypeArguments = null;
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            actualTypeArguments = parameterizedType.getActualTypeArguments();
            cls = (Class) parameterizedType.getRawType();
        } else {
            cls = (Class) type;
        }
        Object objUnpackSimpleType = null;
        try {
            if (List.class.isAssignableFrom(cls)) {
                Class cls2 = cls;
                if (actualTypeArguments != null && actualTypeArguments.length > 0) {
                    cls2 = (Class) actualTypeArguments[0];
                }
                objUnpackSimpleType = unpackList(unpack, cls2);
            } else if (Map.class.isAssignableFrom(cls)) {
                objUnpackSimpleType = unpackMap(unpack, (Class) actualTypeArguments[0], (Class) actualTypeArguments[1], false);
            } else {
                objUnpackSimpleType = unpackSimpleType(unpack, cls);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (T) objUnpackSimpleType;
    }
}
