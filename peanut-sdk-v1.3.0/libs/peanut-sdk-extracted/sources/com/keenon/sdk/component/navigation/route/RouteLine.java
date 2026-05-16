package com.keenon.sdk.component.navigation.route;

import com.keenon.sdk.component.navigation.route.RouteSelector;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/route/RouteLine.class */
public class RouteLine implements RouteSelector.Output {
    private static final String TAG = RouteLine.class.getSimpleName();
    private RouteSelector routeSelector;
    private RouteNode[] routeNodes;
    private Listener listener;
    private boolean autoRepeat;
    private int repeatCount;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/route/RouteLine$Listener.class */
    public interface Listener {
        void onRoutePrepared();

        void onRouteError(int i);
    }

    public RouteLine(RouteNode... routeNodes) {
        this(0, routeNodes);
    }

    public RouteLine(int policy, RouteNode... routeNodes) {
        this(policy, false, 1, routeNodes);
    }

    public RouteLine(int policy, boolean autoRepeat, int repeatCount, RouteNode... routeNodes) {
        this.routeSelector = buildRouteSelector(policy);
        this.autoRepeat = autoRepeat;
        this.repeatCount = repeatCount;
        this.routeNodes = routeNodes;
    }

    public RouteLine(RouteSelector routeSelector, boolean autoRepeat, int repeatCount, RouteNode... routeNodes) {
        this.routeSelector = routeSelector;
        this.autoRepeat = autoRepeat;
        this.repeatCount = repeatCount;
        this.routeNodes = routeNodes;
    }

    private RouteSelector buildRouteSelector(int policy) {
        if (policy == 1) {
            return DefaultRouteSelector.newAdaptiveInstance();
        }
        if (policy == 2) {
            return DefaultRouteSelector.newFixedInstance();
        }
        return DefaultRouteSelector.newDefaultInstance();
    }

    public void addListener(Listener listener) {
        this.listener = listener;
    }

    public void removeListener(Listener listener) {
        this.listener = null;
    }

    public void prepare() {
        this.routeSelector.selectRoute(this, this.routeNodes);
    }

    public RouteNode[] getRouteNodes() {
        return this.routeNodes;
    }

    public void setRouteNodes(RouteNode... routeNodes) {
        this.routeNodes = routeNodes;
    }

    public int getLength() {
        return this.routeNodes.length;
    }

    public boolean isAutoRepeat() {
        return this.autoRepeat;
    }

    public void setAutoRepeat(boolean autoRepeat) {
        this.autoRepeat = autoRepeat;
    }

    public int getRepeatCount() {
        return this.repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    @Override // com.keenon.sdk.component.navigation.route.RouteSelector.Output
    public void adaptiveRoute(RouteNode... routeNodes) {
        setRouteNodes(routeNodes);
        if (this.listener != null) {
            this.listener.onRoutePrepared();
        }
    }

    @Override // com.keenon.sdk.component.navigation.route.RouteSelector.Output
    public void fixedRoute(RouteNode... routeNodes) {
        if (this.listener != null) {
            this.listener.onRoutePrepared();
        }
    }

    @Override // com.keenon.sdk.component.navigation.route.RouteSelector.Output
    public void onError(int errorCode) {
        if (this.listener != null) {
            this.listener.onRouteError(errorCode);
        }
    }
}
