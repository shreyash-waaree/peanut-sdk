package com.keenon.sdk.proxy.sender;

import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.ArrayUtils;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.coap.adapter.CoapCommond;
import com.keenon.sdk.coap.adapter.CoapParams;
import com.keenon.sdk.coap.adapter.CoapResponse;
import com.keenon.sdk.hedera.base.ILinkAdapter;
import com.keenon.sdk.hedera.model.ICallback;
import com.keenon.sdk.hedera.model.RequestEnum;
import com.keenon.sdk.http.adapter.HttpCommand;
import com.keenon.sdk.http.adapter.HttpLinkAdapter;
import com.keenon.sdk.http.adapter.HttpParams;
import com.keenon.sdk.http.adapter.HttpResponse;
import com.keenon.sdk.proxy.adapter.CoapLinkAdapter;
import com.keenon.sdk.proxy.adapter.LinkAdapterFactory;
import com.keenon.sdk.proxy.sender.anno.LinkAdapter;
import com.keenon.sdk.proxy.sender.anno.RequestType;
import com.keenon.sdk.serial.adapter.SerialCommand;
import com.keenon.sdk.serial.adapter.SerialLinkAdapter;
import com.keenon.sdk.serial.adapter.SerialParams;
import com.keenon.sdk.serial.adapter.SerialResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/proxy/sender/SenderManager.class */
public class SenderManager {
    private static final String TAG = "SenderManager";
    private static SenderManager instance;
    CoapCommond coapCommand;
    SerialCommand serialCommand;
    HttpCommand httpCommand;
    ICallback coapCallback;
    ICallback serialCallback;
    ICallback httpCallback;
    String coapParams;
    String httpParams;
    RequestEnum requestEnum;
    Byte[] serialParams;
    private ILinkAdapter linkAdapter;

    public static SenderManager getInstance() {
        instance = new SenderManager();
        return instance;
    }

    private void prepare() {
        this.linkAdapter = null;
        this.requestEnum = null;
        this.coapCommand = null;
        this.coapParams = null;
        this.coapCallback = null;
        this.serialCommand = null;
        this.serialParams = null;
        this.serialCallback = null;
        this.httpCommand = null;
        this.httpParams = null;
        this.httpCallback = null;
    }

    public void release() {
        LogUtils.i(PeanutConstants.TAG_SDK, "[SenderManager][release]");
        prepare();
    }

    public void send(Object object) {
        prepare();
        startAnnoParser(object);
        startAdapt(false);
    }

    private synchronized void startAnnoParser(Object object) {
        Class<?> cls = object.getClass();
        Class<? super Object> superclass = cls.getSuperclass();
        Annotation[] classAnnotations = cls.getAnnotations();
        Method[] methods = cls.getDeclaredMethods();
        Field[] fields = cls.getDeclaredFields();
        if (superclass != null && superclass.getSuperclass() != null) {
            Annotation[] parentAnnotations = superclass.getAnnotations();
            Annotation[] clearAnnotations = parentAnnotations;
            if (ArrayUtils.isNotEmpty(classAnnotations) && ArrayUtils.isNotEmpty(parentAnnotations)) {
                for (Annotation clzAnno : classAnnotations) {
                    for (Annotation parentAnno : parentAnnotations) {
                        if (clzAnno.annotationType().equals(parentAnno.annotationType())) {
                            clearAnnotations = (Annotation[]) ArrayUtils.removeElement(parentAnnotations, parentAnno);
                        }
                    }
                }
                classAnnotations = (Annotation[]) ArrayUtils.concat(classAnnotations, clearAnnotations);
            }
            Method[] parentMethods = superclass.getDeclaredMethods();
            if (ArrayUtils.isNotEmpty(parentMethods)) {
                methods = (Method[]) ArrayUtils.concat(methods, parentMethods);
            }
            Field[] parentFields = superclass.getDeclaredFields();
            if (ArrayUtils.isNotEmpty(parentFields)) {
                fields = (Field[]) ArrayUtils.concat(fields, parentFields);
            }
        }
        for (Annotation annotation : classAnnotations) {
            parseClassAnnotation(annotation);
        }
        for (Method method : methods) {
            Annotation[] methodAnnotations = method.getAnnotations();
            if (methodAnnotations.length > 0) {
                for (Annotation annotation2 : methodAnnotations) {
                    parseMethodAnnotation(object, method, annotation2);
                }
            }
        }
        for (Field field : fields) {
            Annotation[] fieldAnnotations = field.getAnnotations();
            if (fieldAnnotations.length > 0) {
                for (Annotation annotation3 : fieldAnnotations) {
                    parseFieldAnnotation(object, field, annotation3);
                }
            }
        }
    }

    private void parseFieldAnnotation(Object object, Field field, Annotation annotation) {
        if (annotation instanceof CoapResponse) {
            try {
                field.setAccessible(true);
                Object fieldObject = field.get(object);
                if (fieldObject instanceof ICallback) {
                    this.coapCallback = (ICallback) fieldObject;
                }
                return;
            } catch (IllegalAccessException e) {
                LogUtils.e(PeanutConstants.TAG_SDK, e);
                return;
            }
        }
        if (annotation instanceof SerialResponse) {
            try {
                field.setAccessible(true);
                Object fieldObject2 = field.get(object);
                if (fieldObject2 instanceof ICallback) {
                    this.serialCallback = (ICallback) fieldObject2;
                }
                return;
            } catch (IllegalAccessException e2) {
                LogUtils.e(PeanutConstants.TAG_SDK, e2);
                return;
            }
        }
        if (annotation instanceof HttpResponse) {
            try {
                field.setAccessible(true);
                Object fieldObject3 = field.get(object);
                if (fieldObject3 instanceof ICallback) {
                    this.httpCallback = (ICallback) fieldObject3;
                }
            } catch (IllegalAccessException e3) {
                LogUtils.e(PeanutConstants.TAG_SDK, e3);
            }
        }
    }

    private void parseMethodAnnotation(Object object, Method method, Annotation annotation) {
        if (annotation instanceof RequestType) {
            try {
                this.requestEnum = (RequestEnum) method.invoke(object, new Object[0]);
                return;
            } catch (IllegalAccessException e) {
                LogUtils.e(PeanutConstants.TAG_SDK, e);
                return;
            } catch (InvocationTargetException e2) {
                LogUtils.e(PeanutConstants.TAG_SDK, e2);
                return;
            }
        }
        if (annotation instanceof CoapParams) {
            try {
                this.coapParams = (String) method.invoke(object, new Object[0]);
                return;
            } catch (IllegalAccessException e3) {
                LogUtils.e(PeanutConstants.TAG_SDK, e3);
                return;
            } catch (InvocationTargetException e4) {
                LogUtils.e(PeanutConstants.TAG_SDK, e4);
                return;
            }
        }
        if (annotation instanceof SerialParams) {
            try {
                this.serialParams = (Byte[]) method.invoke(object, new Object[0]);
                return;
            } catch (IllegalAccessException e5) {
                LogUtils.e(PeanutConstants.TAG_SDK, e5);
                return;
            } catch (InvocationTargetException e6) {
                LogUtils.e(PeanutConstants.TAG_SDK, e6);
                return;
            }
        }
        if (annotation instanceof HttpParams) {
            try {
                this.httpParams = (String) method.invoke(object, new Object[0]);
            } catch (IllegalAccessException e7) {
                LogUtils.e(PeanutConstants.TAG_SDK, e7);
            } catch (InvocationTargetException e8) {
                LogUtils.e(PeanutConstants.TAG_SDK, e8);
            }
        }
    }

    private void parseClassAnnotation(Annotation annotation) {
        if (annotation instanceof LinkAdapter) {
            LinkAdapter linkAdapterAnno = (LinkAdapter) annotation;
            this.linkAdapter = LinkAdapterFactory.getInstance().create(linkAdapterAnno.board(), linkAdapterAnno.link(), linkAdapterAnno.com(), linkAdapterAnno.custom());
        } else if (annotation instanceof CoapCommond) {
            this.coapCommand = (CoapCommond) annotation;
        } else if (annotation instanceof SerialCommand) {
            this.serialCommand = (SerialCommand) annotation;
        } else if (annotation instanceof HttpCommand) {
            this.httpCommand = (HttpCommand) annotation;
        }
    }

    private void startAdapt(boolean cancel) {
        if (this.linkAdapter != null) {
            synchronized (this.linkAdapter) {
                this.linkAdapter.reset();
                feedLinkAdapter();
                this.linkAdapter.adapt(cancel);
            }
        }
    }

    private void feedLinkAdapter() {
        if (this.linkAdapter instanceof CoapLinkAdapter) {
            if (this.coapCommand != null) {
                ((CoapLinkAdapter) this.linkAdapter).setRequest(this.coapCommand);
            }
            if (this.requestEnum != null) {
                ((CoapLinkAdapter) this.linkAdapter).setRequestEnum(this.requestEnum);
            }
            if (this.coapParams != null && !this.coapParams.isEmpty()) {
                ((CoapLinkAdapter) this.linkAdapter).setRequestParams(this.coapParams);
            }
            if (this.coapCallback != null) {
                ((CoapLinkAdapter) this.linkAdapter).setCallBack(this.coapCallback);
                return;
            }
            return;
        }
        if (this.linkAdapter instanceof SerialLinkAdapter) {
            if (this.serialCommand != null) {
                ((SerialLinkAdapter) this.linkAdapter).setRequest(this.serialCommand);
            }
            if (this.requestEnum != null) {
                ((SerialLinkAdapter) this.linkAdapter).setRequestEnum(this.requestEnum);
            }
            if (this.serialParams != null && this.serialParams.length > 0) {
                ((SerialLinkAdapter) this.linkAdapter).setRequestParams(this.serialParams);
            }
            if (this.serialCallback != null) {
                ((SerialLinkAdapter) this.linkAdapter).setCallBack(this.serialCallback);
                return;
            }
            return;
        }
        if (this.linkAdapter instanceof HttpLinkAdapter) {
            if (this.httpCommand != null) {
                ((HttpLinkAdapter) this.linkAdapter).setRequest(this.httpCommand);
            }
            if (this.requestEnum != null) {
                ((HttpLinkAdapter) this.linkAdapter).setRequestType(this.requestEnum);
            }
            if (this.httpParams != null && !this.httpParams.isEmpty()) {
                ((HttpLinkAdapter) this.linkAdapter).setRequestParams(this.httpParams);
            }
            if (this.httpCallback != null) {
                ((HttpLinkAdapter) this.linkAdapter).setCallBack(this.httpCallback);
            }
        }
    }

    public void cancel(Object object) {
        prepare();
        startAnnoParser(object);
        startAdapt(true);
    }
}
