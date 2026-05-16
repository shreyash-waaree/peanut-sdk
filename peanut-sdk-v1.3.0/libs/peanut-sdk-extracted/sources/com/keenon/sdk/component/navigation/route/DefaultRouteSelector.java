package com.keenon.sdk.component.navigation.route;

import com.keenon.common.error.PeanutError;
import com.keenon.common.utils.GsonUtil;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.api.NavigationSetMultiTargetApi;
import com.keenon.sdk.component.navigation.ConvertUtils;
import com.keenon.sdk.component.navigation.route.RouteSelector;
import com.keenon.sdk.external.IDataCallback;
import com.keenon.sdk.external.PeanutSDK;
import com.keenon.sdk.hedera.model.ApiError;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/route/DefaultRouteSelector.class */
public class DefaultRouteSelector implements RouteSelector {
    private static final String TAG = DefaultRouteSelector.class.getSimpleName();
    public static final int TYPE_DEFAULT = 0;
    public static final int TYPE_ADAPTIVE = 1;
    public static final int TYPE_FIXED = 2;
    private RouteSelector.Output mOutput;
    private final int type;

    public static DefaultRouteSelector newDefaultInstance() {
        return new DefaultRouteSelector(0);
    }

    public static DefaultRouteSelector newAdaptiveInstance() {
        return new DefaultRouteSelector(1);
    }

    public static DefaultRouteSelector newFixedInstance() {
        return new DefaultRouteSelector(2);
    }

    private DefaultRouteSelector(int type) {
        this.type = type;
    }

    @Override // com.keenon.sdk.component.navigation.route.RouteSelector
    public void selectRoute(RouteSelector.Output output, RouteNode... routeNodes) {
        this.mOutput = output;
        if (this.type == 2) {
            this.mOutput.fixedRoute(routeNodes);
        } else {
            adaptRouteNodes(routeNodes);
        }
    }

    private void adaptRouteNodes(RouteNode... routeNodes) {
        if (routeNodes.length > 10) {
            notifyError(PeanutError.NAVI_ROUTE_LIMIT);
        } else if (routeNodes.length == 1) {
            notifyFixedRoute(routeNodes);
        } else {
            PeanutSDK.getInstance().navigation().setTargetList(new IDataCallback() { // from class: com.keenon.sdk.component.navigation.route.DefaultRouteSelector.1
                @Override // com.keenon.sdk.external.IDataCallback
                public void success(String response) {
                    LogUtils.d(DefaultRouteSelector.TAG, "[adaptRouteNodes][onSuccess][bean = " + response + "]");
                    NavigationSetMultiTargetApi.Bean data = (NavigationSetMultiTargetApi.Bean) GsonUtil.gson2Bean(response, NavigationSetMultiTargetApi.Bean.class);
                    if (null != data) {
                        if (data.getCode() == 0) {
                            if (data.getData() == null) {
                                DefaultRouteSelector.this.notifyError(PeanutError.COAP_RESPONSE_NULL_DATA);
                                return;
                            } else {
                                DefaultRouteSelector.this.notifyAdaptiveRoute(ConvertUtils.intToRoute(data.getData().toList()));
                                return;
                            }
                        }
                        if (data.getCode() == 35) {
                            DefaultRouteSelector.this.notifyError(PeanutError.NAVI_ROUTE_NO_NODE);
                        } else if (data.getCode() == 81) {
                            DefaultRouteSelector.this.notifyError(PeanutError.NAVI_ROUTE_NO_PATH);
                        } else if (data.getCode() == 256) {
                            DefaultRouteSelector.this.notifyError(PeanutError.NAVI_ROUTE_INVALID_POSE);
                        }
                    }
                }

                @Override // com.keenon.sdk.external.IDataCallback
                public void error(ApiError error) {
                    LogUtils.e(DefaultRouteSelector.TAG, "[adaptRouteNodes][onError][" + error.toString() + "]");
                    DefaultRouteSelector.this.notifyError(PeanutError.COAP_RESPONSE_ERROR);
                }
            }, ConvertUtils.routeToInt(routeNodes));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAdaptiveRoute(RouteNode... routeNodes) {
        if (this.mOutput != null) {
            this.mOutput.adaptiveRoute(routeNodes);
        }
    }

    private void notifyFixedRoute(RouteNode... routeNodes) {
        if (this.mOutput != null) {
            this.mOutput.fixedRoute(routeNodes);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyError(int code) {
        if (this.mOutput != null) {
            this.mOutput.onError(code);
        }
    }
}
