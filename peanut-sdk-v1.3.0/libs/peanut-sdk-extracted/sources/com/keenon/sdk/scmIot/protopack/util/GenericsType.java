package com.keenon.sdk.scmIot.protopack.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/util/GenericsType.class */
public class GenericsType {
    private Type[] argTypes;

    public GenericsType(Type genericFieldType) {
        if (genericFieldType instanceof ParameterizedType) {
            ParameterizedType aType = (ParameterizedType) genericFieldType;
            this.argTypes = aType.getActualTypeArguments();
        }
    }

    public TypeVo getType(int index) throws Exception {
        TypeVo vo = new TypeVo();
        if (this.argTypes.length > 0) {
            if (index >= this.argTypes.length || index < 0) {
                throw new RuntimeException("你输入的索引" + (index < 0 ? "不能小于0" : "超出了参数的总数"));
            }
            if (this.argTypes[index] instanceof ParameterizedType) {
                vo.setNextType(this.argTypes[index]);
                vo.setCls((Class) ((ParameterizedType) this.argTypes[index]).getRawType());
            } else {
                vo.setCls((Class) this.argTypes[index]);
            }
        } else {
            vo.setCls(Object.class);
        }
        return vo;
    }
}
