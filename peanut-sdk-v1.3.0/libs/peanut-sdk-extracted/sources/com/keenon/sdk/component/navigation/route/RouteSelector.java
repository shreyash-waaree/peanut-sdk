package com.keenon.sdk.component.navigation.route;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/route/RouteSelector.class */
public interface RouteSelector {
    public static final int DEFAULT_LIMIT = 10;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/route/RouteSelector$Output.class */
    public interface Output {
        void adaptiveRoute(RouteNode... routeNodeArr);

        void fixedRoute(RouteNode... routeNodeArr);

        void onError(int i);
    }

    void selectRoute(Output output, RouteNode... routeNodeArr);
}
