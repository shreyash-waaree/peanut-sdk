package com.keenon.sdk.component.navigation;

import androidx.annotation.NonNull;
import com.keenon.common.constant.PeanutConstants;
import com.keenon.common.utils.LogUtils;
import com.keenon.sdk.component.navigation.arrival.ArrivalControl;
import com.keenon.sdk.component.navigation.arrival.DefaultArrivalControl;
import com.keenon.sdk.component.navigation.common.Navigation;
import com.keenon.sdk.component.navigation.route.RouteLine;
import com.keenon.sdk.component.navigation.route.RouteNode;
import com.keenon.sdk.component.navigation.route.RouteSelector;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/PeanutNavigation.class */
public class PeanutNavigation {
    public static final int POLICY_ADAPTIVE = 1;
    public static final int POLICY_FIXED = 2;
    private Navigation navigation;
    private RouteLine routeLine;

    public PeanutNavigation(ArrivalControl arrivalControl, RouteLine routeLine, Navigation.Listener listener, int blockingTimeout) {
        this.routeLine = routeLine;
        this.navigation = Navigation.Factory.newInstance(arrivalControl);
        this.navigation.addListener(listener);
        this.navigation.setBlockTimeout(blockingTimeout);
    }

    public void setTargets(Integer... targets) {
        LogUtils.d(PeanutConstants.TAG_NAVI, "[PeanutNavigation] length :  " + targets.length);
        this.navigation.resetNewTargets();
        this.routeLine.setRouteNodes(ConvertUtils.intToRoute(targets));
    }

    public void setTargets(List<RouteNode> targets) {
        this.navigation.resetNewTargets();
        RouteNode[] routeNodes = new RouteNode[targets.size()];
        this.routeLine.setRouteNodes((RouteNode[]) targets.toArray(routeNodes));
    }

    public void prepare() {
        this.navigation.prepare(this.routeLine);
    }

    public void setPilotWhenReady(boolean pilotWhenReady) {
        this.navigation.setPilotWhenReady(pilotWhenReady);
    }

    public void setPilotWhenReady(boolean pilotWhenReady, boolean isTaskControl) {
        this.navigation.setTaskControl(isTaskControl);
        this.navigation.setPilotWhenReady(pilotWhenReady);
    }

    public void setSpeed(int speed) {
        this.navigation.setSpeed(speed);
    }

    public void setStableMode(int mode) {
        this.navigation.setStableMode(mode);
    }

    public void setSportMode(int mode) {
        this.navigation.setSportMode(mode);
    }

    public void pilotNext() {
        this.navigation.pilotNext();
    }

    public void stop() {
        this.navigation.stop();
    }

    public void release() {
        this.navigation.release();
    }

    public void manual(int direction) {
        this.navigation.manual(direction);
    }

    public RouteNode[] getRouteNodes() {
        return this.navigation.getRouteNodes();
    }

    public RouteNode getCurrentNode() {
        return this.navigation.getCurrentNode();
    }

    public RouteNode getNextNode() {
        return this.navigation.getNextNode();
    }

    public boolean isLastNode() {
        return this.navigation.getCurrentPosition() >= this.navigation.getRouteNodes().length - 1;
    }

    public int getCurrentPosition() {
        return this.navigation.getCurrentPosition();
    }

    public void skipTo(@NonNull int node) {
        this.navigation.skipTo(node);
    }

    public void cancelArrivalControl() {
        this.navigation.cancelArriveControl();
    }

    public void setArrivalControlEnable(boolean enable) {
        this.navigation.arrivalControlEnable(enable);
    }

    public boolean isLastRepeat() {
        return this.navigation.isLastRepeat();
    }

    public void addListener(Navigation.Listener listener) {
        this.navigation.addListener(listener);
    }

    public void removeListerner(Navigation.Listener listener) {
        this.navigation.removeListener(listener);
    }

    public void setLoopNum(int num) {
        this.navigation.getRouteLine().setRepeatCount(num);
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/PeanutNavigation$Builder.class */
    public static class Builder {
        private Navigation.Listener listener;
        private RouteNode[] routeNodes;
        private ArrivalControl arrivalControl = null;
        private RouteSelector routeSelector = null;
        private boolean arrivalEnable = true;
        private float marginScope = 1.0f;
        private int blockDelay = ArrivalControl.DEFAULT_BLOCK_DELAY;
        private int scopeDelay = 10000;
        private int routePolicy = 1;
        private boolean autoRepeat = false;
        private int repeatCount = 1;
        private int blockingTimeOut = 0;

        public Builder setTargets(Integer... targets) {
            this.routeNodes = ConvertUtils.intToRoute(targets);
            return this;
        }

        public Builder enableDefaultArrival(boolean enable) {
            this.arrivalEnable = enable;
            return this;
        }

        public Builder setRoutePolicy(int policy) {
            this.routePolicy = policy;
            return this;
        }

        public Builder enableAutoRepeat(boolean autoRepeat) {
            this.autoRepeat = autoRepeat;
            return this;
        }

        public Builder setRepeatCount(int count) {
            this.repeatCount = count;
            return this;
        }

        public Builder setArrivalControl(ArrivalControl arrivalControl) {
            this.arrivalControl = arrivalControl;
            return this;
        }

        public Builder setArrivalControl(boolean enable, float marginScope, int blockDelay, int scopeDelay) {
            this.arrivalEnable = enable;
            this.marginScope = marginScope;
            this.blockDelay = blockDelay;
            this.scopeDelay = scopeDelay;
            return this;
        }

        public Builder setRouteSelector(RouteSelector routeSelector) {
            this.routeSelector = routeSelector;
            return this;
        }

        public Builder setListener(Navigation.Listener listener) {
            this.listener = listener;
            return this;
        }

        public Builder setBlockingTimeOut(int timeout) {
            this.blockingTimeOut = timeout;
            return this;
        }

        public PeanutNavigation build() {
            RouteLine routeLine;
            if (this.routeSelector == null) {
                routeLine = new RouteLine(this.routePolicy, this.autoRepeat, this.repeatCount, this.routeNodes);
            } else {
                routeLine = new RouteLine(this.routeSelector, this.autoRepeat, this.repeatCount, this.routeNodes);
            }
            if (this.arrivalControl == null) {
                this.arrivalControl = new DefaultArrivalControl(this.arrivalEnable, this.marginScope, this.blockDelay, this.scopeDelay);
            }
            return new PeanutNavigation(this.arrivalControl, routeLine, this.listener, this.blockingTimeOut);
        }
    }
}
