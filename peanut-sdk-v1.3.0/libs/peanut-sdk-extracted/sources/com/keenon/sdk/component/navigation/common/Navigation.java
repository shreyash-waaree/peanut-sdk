package com.keenon.sdk.component.navigation.common;

import com.keenon.sdk.component.navigation.NavigationImpl;
import com.keenon.sdk.component.navigation.arrival.ArrivalControl;
import com.keenon.sdk.component.navigation.route.RouteLine;
import com.keenon.sdk.component.navigation.route.RouteNode;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/common/Navigation.class */
public interface Navigation {
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARED = 1;
    public static final int STATE_RUNNING = 2;
    public static final int STATE_DESTINATION = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_COLLISION = 5;
    public static final int STATE_BLOCKED = 6;
    public static final int STATE_STOPPED = 7;
    public static final int STATE_ERROR = 8;
    public static final int STATE_BLOCKING = 9;
    public static final int STATE_END = 10;
    public static final int FORWARD = 1;
    public static final int BACKWARD = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;
    public static final int EVENT_SET_TAG_SUCCESS = 100;

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/common/Navigation$Listener.class */
    public interface Listener {
        void onStateChanged(int i, int i2);

        void onRouteNode(int i, RouteNode routeNode);

        void onRoutePrepared(RouteNode... routeNodeArr);

        void onDistanceChanged(float f);

        void onError(int i);

        void onEvent(int i);
    }

    void addListener(Listener listener);

    void removeListener(Listener listener);

    void prepare(RouteLine routeLine);

    void setPilotWhenReady(boolean z);

    void setTaskControl(boolean z);

    void pilotNext();

    void stop();

    void manual(int i);

    void setSpeed(int i);

    void setStableMode(int i);

    void setSportMode(int i);

    void setBlockTimeout(int i);

    RouteNode[] getRouteNodes();

    RouteNode getCurrentNode();

    RouteNode getNextNode();

    int getCurrentPosition();

    void release();

    void skipTo(int i);

    void cancelArriveControl();

    void resetNewTargets();

    boolean isLastRepeat();

    void arrivalControlEnable(boolean z);

    RouteLine getRouteLine();

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/common/Navigation$Factory.class */
    public static final class Factory {
        private Factory() {
        }

        public static Navigation newInstance(ArrivalControl arrivalControl) {
            return NavigationImpl.getInstance(arrivalControl);
        }
    }
}
