package com.keenon.sdk.scmIot.protopack.marshal;

import com.keenon.sdk.scmIot.protopack.annotation.BeanFieldAno;
import com.keenon.sdk.scmIot.protopack.base.Marshallable;
import com.keenon.sdk.scmIot.protopack.base.Pack;
import com.keenon.sdk.scmIot.protopack.base.Unpack;
import com.keenon.sdk.scmIot.protopack.exception.PackException;
import com.keenon.sdk.scmIot.protopack.exception.UnpackException;
import com.keenon.sdk.scmIot.protopack.util.GenericsType;
import com.keenon.sdk.scmIot.protopack.util.GenericsUtils;
import com.keenon.sdk.scmIot.protopack.util.TypeVo;
import com.keenon.sdk.scmIot.protopack.util.Uint;
import com.keenon.sdk.scmIot.protopack.util.Ushort;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/marshal/BeanMarshal.class */
public abstract class BeanMarshal implements Marshallable {
    private static Map<Object, Field[]> classFields = new HashMap();

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void marshal(Pack pack) {
        marshalObject(pack, this);
    }

    private short getShortFiled(Object obj, Field field) throws Exception {
        Object result = field.get(obj);
        if (result == null) {
            return (short) 0;
        }
        return Short.parseShort(result.toString());
    }

    private int getIntFiled(Object obj, Field field) throws Exception {
        Object result = field.get(obj);
        if (result == null) {
            return 0;
        }
        return Integer.parseInt(result.toString());
    }

    private long getLongFiled(Object obj, Field field) throws Exception {
        Object result = field.get(obj);
        if (result == null) {
            return 0L;
        }
        return Long.parseLong(result.toString());
    }

    private float getFloatFiled(Object obj, Field field) throws Exception {
        Object result = field.get(obj);
        if (result == null) {
            return 0.0f;
        }
        return Float.parseFloat(result.toString());
    }

    private String getStringFiled(Object obj, Field field) throws Exception {
        Object result = field.get(obj);
        if (result == null) {
            return "";
        }
        return result.toString();
    }

    private boolean getBooleanFiled(Object obj, Field field) throws Exception {
        Object result = field.get(obj);
        if (result == null) {
            return false;
        }
        return Boolean.parseBoolean(result.toString());
    }

    protected void marshalObject(Pack pack, Object obj) {
        Field[] fields;
        if (obj == null || (fields = getFields(obj)) == null || fields.length == 0) {
            return;
        }
        for (Field field : fields) {
            try {
                if (field.getType().isPrimitive()) {
                    if (Short.TYPE.isAssignableFrom(field.getType())) {
                        pack.putShort(field.getShort(obj));
                    } else if (Integer.TYPE.isAssignableFrom(field.getType())) {
                        pack.putInt(field.getInt(obj));
                    } else if (Long.TYPE.isAssignableFrom(field.getType())) {
                        pack.putLong(field.getLong(obj));
                    } else if (Float.TYPE.isAssignableFrom(field.getType())) {
                        pack.putFloat(field.getFloat(obj));
                    } else if (Boolean.TYPE.isAssignableFrom(field.getType())) {
                        pack.putBoolean(field.getBoolean(obj));
                    } else if (Byte.TYPE.isAssignableFrom(field.getType())) {
                        pack.putByte(field.getByte(obj));
                    }
                } else if (String.class.isAssignableFrom(field.getType())) {
                    pack.putVarstr(getStringFiled(obj, field));
                } else if (Short.class.isAssignableFrom(field.getType())) {
                    pack.putShort(getShortFiled(obj, field));
                } else if (Integer.class.isAssignableFrom(field.getType())) {
                    pack.putInt(getIntFiled(obj, field));
                } else if (Long.class.isAssignableFrom(field.getType())) {
                    pack.putLong(getLongFiled(obj, field));
                } else if (Float.class.isAssignableFrom(field.getType())) {
                    pack.putFloat(getFloatFiled(obj, field));
                } else if (Boolean.class.isAssignableFrom(field.getType())) {
                    pack.putBoolean(getBooleanFiled(obj, field));
                } else if (Byte.class.isAssignableFrom(field.getType())) {
                    Object result = field.get(obj);
                    if (result != null) {
                        pack.putByte(Byte.parseByte(result.toString()));
                    }
                } else if (Uint.class.isAssignableFrom(field.getType())) {
                    Object result2 = field.get(obj);
                    if (result2 != null) {
                        result2 = new Uint(0);
                    }
                    pack.putUInt(new Uint(result2.toString()));
                } else if (Ushort.class.isAssignableFrom(field.getType())) {
                    Object result3 = field.get(obj);
                    if (result3 == null) {
                        result3 = new Ushort(0);
                    }
                    pack.putUshort(new Ushort(result3.toString()));
                } else if (List.class.isAssignableFrom(field.getType())) {
                    List<?> list = (List) field.get(obj);
                    marshalList(pack, list, GenericsUtils.getGenericType(field.getGenericType()));
                } else if (Map.class.isAssignableFrom(field.getType())) {
                    marshalMap(pack, (Map) field.get(obj), GenericsUtils.getGenericType(field.getGenericType()));
                } else if (Marshallable.class.isAssignableFrom(field.getType())) {
                    Object marObj = field.get(obj);
                    if (marObj != null) {
                        ((Marshallable) marObj).marshal(pack);
                    }
                } else if (byte[].class.isAssignableFrom(field.getType())) {
                    byte[] marObj2 = (byte[]) field.get(obj);
                    if (marObj2 != null) {
                        pack.putBytes(marObj2);
                    }
                } else if (Object.class.isAssignableFrom(field.getType())) {
                    marshalObject(pack, field.get(obj));
                } else {
                    throw new PackException("unkown type to marshalObject : " + field.getType());
                }
            } catch (Exception e) {
                throw new PackException("unkown type to marshalObject : " + field.getType());
            }
        }
    }

    protected void unmarshalObject(Unpack unpack, Object obj) {
        Field[] fields = getFields(obj);
        if (fields == null || fields.length == 0) {
            return;
        }
        for (Field field : fields) {
            try {
                if (field.getType().isPrimitive()) {
                    if (Short.TYPE.isAssignableFrom(field.getType())) {
                        field.setShort(obj, unpack.popShort().shortValue());
                    } else if (Integer.TYPE.isAssignableFrom(field.getType())) {
                        field.setInt(obj, unpack.popInt().intValue());
                    } else if (Long.TYPE.isAssignableFrom(field.getType())) {
                        field.setLong(obj, unpack.popLong().longValue());
                    } else if (Float.TYPE.isAssignableFrom(field.getType())) {
                        field.setFloat(obj, unpack.popFloat());
                    } else if (Boolean.TYPE.isAssignableFrom(field.getType())) {
                        field.setBoolean(obj, unpack.popBoolean().booleanValue());
                    } else if (Byte.TYPE.isAssignableFrom(field.getType())) {
                        field.setByte(obj, unpack.popByte().byteValue());
                    }
                } else if (String.class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popVarstr());
                } else if (Short.class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popShort());
                } else if (Integer.class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popInt());
                } else if (Long.class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popLong());
                } else if (Float.class.isAssignableFrom(field.getType())) {
                    field.set(obj, Float.valueOf(unpack.popFloat()));
                } else if (Boolean.class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popBoolean());
                } else if (Byte.class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popByte());
                } else if (Uint.class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popUInt());
                } else if (List.class.isAssignableFrom(field.getType())) {
                    List<Object> objValue = (List) field.get(obj);
                    if (objValue == null) {
                        objValue = new ArrayList<>();
                        field.set(obj, objValue);
                    }
                    unmarshalList(unpack, objValue, GenericsUtils.getGenericType(field.getGenericType()));
                } else if (Map.class.isAssignableFrom(field.getType())) {
                    Map<Object, Object> objValue2 = (Map) field.get(obj);
                    if (objValue2 == null) {
                        objValue2 = new HashMap<>();
                        field.set(obj, objValue2);
                    }
                    unmarshalMap(unpack, objValue2, GenericsUtils.getGenericType(field.getGenericType()));
                } else if (byte[].class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popBytes());
                } else if (short[].class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popShorts());
                } else if (float[].class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popFloats());
                } else if (int[].class.isAssignableFrom(field.getType())) {
                    field.set(obj, unpack.popInts());
                } else if (Marshallable.class.isAssignableFrom(field.getType())) {
                    Object objValue3 = field.get(obj);
                    if (objValue3 == null) {
                        objValue3 = field.getType().newInstance();
                        field.set(obj, objValue3);
                    }
                    ((Marshallable) objValue3).unmarshal(unpack);
                } else if (Object.class.isAssignableFrom(field.getType())) {
                    Object objValue4 = field.get(obj);
                    if (objValue4 == null) {
                        objValue4 = field.getType().newInstance();
                        field.set(obj, objValue4);
                    }
                    unmarshalObject(unpack, objValue4);
                }
            } catch (Exception e) {
                throw new PackException("unkown type to unmarshalObject : " + field.getType());
            }
        }
    }

    private void marshalList(Pack pack, List<?> list, GenericsType genericsType) throws Exception {
        if (list == null) {
            pack.putInt(0);
            return;
        }
        pack.putInt(list.size());
        if (list.isEmpty()) {
            return;
        }
        TypeVo vo = genericsType.getType(0);
        Class<?> genericClazz = vo.getCls();
        for (int i = 0; i < list.size(); i++) {
            String value = list.get(i).toString();
            if (genericClazz == String.class) {
                pack.putVarstr(value);
            } else if (genericClazz == Short.class) {
                pack.putShort(Short.valueOf(value).shortValue());
            } else if (genericClazz == Integer.class) {
                pack.putInt(Integer.parseInt(value));
            } else if (genericClazz == Uint.class) {
                pack.putUInt(new Uint(value));
            } else if (genericClazz == Long.class) {
                pack.putLong(Long.parseLong(value));
            } else if (genericClazz == Float.class) {
                pack.putFloat(Float.parseFloat(value));
            } else if (genericClazz == Boolean.class) {
                pack.putBoolean(Boolean.parseBoolean(value));
            } else if (Byte.class.isAssignableFrom(genericClazz)) {
                pack.putByte(Byte.parseByte(value));
            } else if (List.class.isAssignableFrom(genericClazz)) {
                marshalList(pack, (List) list.get(i), GenericsUtils.getGenericType(vo.getNextType()));
            } else if (Map.class.isAssignableFrom(genericClazz)) {
                marshalMap(pack, (Map) list.get(i), GenericsUtils.getGenericType(vo.getNextType()));
            } else if (Marshallable.class.isAssignableFrom(genericClazz)) {
                ((Marshallable) list.get(i)).marshal(pack);
            } else if (Object.class.isAssignableFrom(genericClazz)) {
                marshalObject(pack, list.get(i));
            }
        }
    }

    private void unmarshalList(Unpack unpack, List<Object> list, GenericsType genericsType) throws Exception {
        int size = unpack.popInt().intValue();
        if (size == 0) {
            return;
        }
        TypeVo vo = genericsType.getType(0);
        Class<?> genericClazz = vo.getCls();
        for (int i = 0; i < size; i++) {
            if (genericClazz == String.class) {
                list.add(unpack.popVarstr());
            } else if (genericClazz == Short.class) {
                list.add(unpack.popShort());
            } else if (genericClazz == Integer.class) {
                list.add(unpack.popInt());
            } else if (genericClazz == Uint.class) {
                list.add(unpack.popUInt());
            } else if (genericClazz == Long.class) {
                list.add(unpack.popLong());
            } else if (genericClazz == Float.class) {
                list.add(Float.valueOf(unpack.popFloat()));
            } else if (genericClazz == Boolean.class) {
                list.add(unpack.popBoolean());
            } else if (Byte.class.isAssignableFrom(genericClazz)) {
                list.add(unpack.popByte());
            } else if (List.class.isAssignableFrom(genericClazz)) {
                List<Object> l = new ArrayList<>();
                list.add(l);
                unmarshalList(unpack, l, GenericsUtils.getGenericType(vo.getNextType()));
            } else if (Map.class.isAssignableFrom(genericClazz)) {
                Map<Object, Object> chMap = new HashMap<>();
                list.add(chMap);
                unmarshalMap(unpack, chMap, GenericsUtils.getGenericType(vo.getNextType()));
            } else if (Marshallable.class.isAssignableFrom(genericClazz)) {
                Marshallable mar = (Marshallable) genericClazz.newInstance();
                list.add(mar);
                mar.unmarshal(unpack);
            } else if (Object.class.isAssignableFrom(genericClazz)) {
                Object obj = genericClazz.newInstance();
                list.add(obj);
                unmarshalObject(unpack, obj);
            }
        }
    }

    private void marshalMap(Pack pack, Map<?, ?> map, GenericsType genericsType) throws Exception {
        if (map == null) {
            pack.putInt(0);
            return;
        }
        pack.putInt(map.size());
        if (map.size() == 0) {
            return;
        }
        TypeVo voKey = genericsType.getType(0);
        TypeVo voValue = genericsType.getType(1);
        Class<?> genericClazzKey = voKey.getCls();
        Class<?> genericClazzValue = voValue.getCls();
        Set<?> keys = map.keySet();
        for (Object key : keys) {
            String keyStr = key.toString();
            if (genericClazzKey == String.class) {
                pack.putVarstr(keyStr);
            } else if (genericClazzKey == Short.class) {
                pack.putShort(Short.valueOf(keyStr).shortValue());
            } else if (genericClazzKey == Integer.class) {
                pack.putInt(Integer.parseInt(keyStr));
            } else if (genericClazzKey == Uint.class) {
                pack.putUInt(new Uint(keyStr));
            } else if (genericClazzKey == Long.class) {
                pack.putLong(Long.parseLong(keyStr));
            } else if (genericClazzKey == Float.class) {
                pack.putFloat(Float.parseFloat(keyStr));
            } else if (genericClazzKey == Boolean.class) {
                pack.putBoolean(Boolean.parseBoolean(keyStr));
            } else if (Byte.class.isAssignableFrom(genericClazzKey)) {
                pack.putByte(Byte.parseByte(keyStr));
            } else if (List.class.isAssignableFrom(genericClazzKey)) {
                marshalList(pack, (List) key, GenericsUtils.getGenericType(voKey.getNextType()));
            } else if (Map.class.isAssignableFrom(genericClazzKey)) {
                marshalMap(pack, (Map) key, GenericsUtils.getGenericType(voKey.getNextType()));
            } else if (Marshallable.class.isAssignableFrom(genericClazzKey)) {
                ((Marshallable) key).marshal(pack);
            } else if (Object.class.isAssignableFrom(genericClazzKey)) {
                marshalObject(pack, key);
            } else {
                throw new PackException("key type in a map should not be complicated type :" + genericClazzKey);
            }
            String value = map.get(key).toString();
            if (genericClazzValue == String.class) {
                pack.putVarstr(value);
            } else if (genericClazzValue == Short.class) {
                pack.putShort(Short.valueOf(value).shortValue());
            } else if (genericClazzValue == Integer.class) {
                pack.putInt(Integer.parseInt(value));
            } else if (genericClazzValue == Uint.class) {
                pack.putUInt(new Uint(value));
            } else if (genericClazzValue == Long.class) {
                pack.putLong(Long.parseLong(value));
            } else if (genericClazzValue == Float.class) {
                pack.putFloat(Float.parseFloat(value));
            } else if (genericClazzValue == Boolean.class) {
                pack.putBoolean(Boolean.parseBoolean(value));
            } else if (Byte.class.isAssignableFrom(genericClazzValue)) {
                pack.putByte(Byte.parseByte(value));
            } else if (List.class.isAssignableFrom(genericClazzValue)) {
                marshalList(pack, (List) map.get(key), GenericsUtils.getGenericType(voValue.getNextType()));
            } else if (Map.class.isAssignableFrom(genericClazzValue)) {
                marshalMap(pack, (Map) map.get(key), GenericsUtils.getGenericType(voValue.getNextType()));
            } else if (Marshallable.class.isAssignableFrom(genericClazzValue)) {
                ((Marshallable) map.get(key)).marshal(pack);
            } else if (Object.class.isAssignableFrom(genericClazzValue)) {
                marshalObject(pack, map.get(key));
            }
        }
    }

    private void unmarshalMap(Unpack unpack, Map<Object, Object> map, GenericsType genericsType) throws Exception {
        Object key;
        int size = unpack.popInt().intValue();
        if (size == 0) {
            return;
        }
        TypeVo keyVo = genericsType.getType(0);
        Class<?> keyValueType = keyVo.getCls();
        TypeVo vo = genericsType.getType(1);
        Class<?> valueType = vo.getCls();
        for (int i = 0; i < size; i++) {
            if (keyValueType == String.class) {
                key = unpack.popVarstr();
            } else if (keyValueType == Short.class) {
                key = unpack.popShort();
            } else if (keyValueType == Integer.class) {
                key = unpack.popInt();
            } else if (keyValueType == Uint.class) {
                key = unpack.popUInt();
            } else if (keyValueType == Long.class) {
                key = unpack.popLong();
            } else if (keyValueType == Float.class) {
                key = Float.valueOf(unpack.popFloat());
            } else if (keyValueType == Boolean.class) {
                key = unpack.popBoolean();
            } else if (Byte.class.isAssignableFrom(keyValueType)) {
                key = unpack.popByte();
            } else if (List.class.isAssignableFrom(keyValueType)) {
                List<Object> list = new ArrayList<>();
                unmarshalList(unpack, list, GenericsUtils.getGenericType(vo.getNextType()));
                key = list;
            } else if (Map.class.isAssignableFrom(keyValueType)) {
                Map<Object, Object> chMap = (Map) null;
                unmarshalMap(unpack, chMap, GenericsUtils.getGenericType(vo.getNextType()));
                key = chMap;
            } else if (Marshallable.class.isAssignableFrom(keyValueType)) {
                if (keyValueType.isInterface()) {
                    throw new UnpackException("create object error , only specify interface : " + keyValueType);
                }
                Marshallable mar = (Marshallable) keyValueType.newInstance();
                mar.unmarshal(unpack);
                key = mar;
            } else if (Object.class.isAssignableFrom(keyValueType)) {
                key = unpack.popObject(unpack);
            } else {
                throw new PackException("key type in a map should not be complicated type :" + keyValueType);
            }
            if (valueType == String.class) {
                map.put(key, unpack.popVarstr());
            } else if (valueType == Short.class) {
                map.put(key, unpack.popShort());
            } else if (valueType == Integer.class) {
                map.put(key, unpack.popInt());
            } else if (valueType == Uint.class) {
                map.put(key, unpack.popUInt());
            } else if (valueType == Long.class) {
                map.put(key, unpack.popLong());
            } else if (valueType == Float.class) {
                map.put(key, Float.valueOf(unpack.popFloat()));
            } else if (valueType == Boolean.class) {
                map.put(key, unpack.popBoolean());
            } else if (Byte.class.isAssignableFrom(valueType)) {
                map.put(key, unpack.popByte());
            } else if (List.class.isAssignableFrom(valueType)) {
                List<Object> list2 = (List) map.get(key);
                if (list2 == null) {
                    list2 = new ArrayList();
                    map.put(key, list2);
                }
                unmarshalList(unpack, list2, GenericsUtils.getGenericType(vo.getNextType()));
            } else if (Map.class.isAssignableFrom(valueType)) {
                Map<Object, Object> chMap2 = (Map) map.get(key);
                if (chMap2 == null) {
                    chMap2 = new HashMap();
                    map.put(key, chMap2);
                }
                unmarshalMap(unpack, chMap2, GenericsUtils.getGenericType(vo.getNextType()));
            } else if (Marshallable.class.isAssignableFrom(valueType)) {
                if (valueType.isInterface()) {
                    throw new UnpackException("create object error , only specify interface : " + valueType);
                }
                Marshallable mar2 = (Marshallable) valueType.newInstance();
                map.put(key, mar2);
                mar2.unmarshal(unpack);
            } else if (Object.class.isAssignableFrom(valueType)) {
                unmarshalObject(unpack, map.get(key));
            }
        }
    }

    @Override // com.keenon.sdk.scmIot.protopack.base.Marshallable
    public void unmarshal(Unpack unpack) {
        unmarshalObject(unpack, this);
    }

    public Field[] getFields(Object vo) {
        Field[] fields = classFields.get(vo.getClass());
        if (fields == null) {
            List<Field> tmpFields = getOrderedField(vo.getClass().getDeclaredFields());
            List<Field> mshFields = new ArrayList<>();
            for (Field field : tmpFields) {
                if (!Modifier.isTransient(field.getModifiers())) {
                    field.setAccessible(true);
                    mshFields.add(field);
                }
            }
            fields = (Field[]) mshFields.toArray(new Field[0]);
            classFields.put(vo.getClass(), fields);
        }
        return fields;
    }

    private List<Field> getOrderedField(Field[] fields) {
        List<Field> fieldList = new ArrayList<>();
        for (Field f : fields) {
            if (f.getAnnotation(BeanFieldAno.class) != null) {
                fieldList.add(f);
            }
        }
        Collections.sort(fieldList, new Comparator<Field>() { // from class: com.keenon.sdk.scmIot.protopack.marshal.BeanMarshal.1
            @Override // java.util.Comparator
            public int compare(Field o1, Field o2) {
                int diff = ((BeanFieldAno) o1.getAnnotation(BeanFieldAno.class)).order() - ((BeanFieldAno) o2.getAnnotation(BeanFieldAno.class)).order();
                if (diff > 0) {
                    return 1;
                }
                if (diff < 0) {
                    return -1;
                }
                return 0;
            }
        });
        return fieldList;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        Field[] fields = getFields(this);
        sb.append("{");
        for (int i = 0; i < fields.length; i++) {
            try {
                Field field = fields[i];
                sb.append(field.getName() + "=" + field.get(this));
                if (i != fields.length - 1) {
                    sb.append(", ");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
