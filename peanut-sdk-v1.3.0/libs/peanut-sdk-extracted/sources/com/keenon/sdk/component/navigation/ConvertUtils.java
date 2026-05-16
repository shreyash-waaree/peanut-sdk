package com.keenon.sdk.component.navigation;

import com.keenon.sdk.component.navigation.route.RouteNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/component/navigation/ConvertUtils.class */
public class ConvertUtils {
    public static List<Integer> routeToInt(RouteNode... routeNodes) {
        List<Integer> dstList = new ArrayList<>();
        for (RouteNode routeNode : routeNodes) {
            dstList.add(routeNode.getId());
        }
        return dstList;
    }

    public static RouteNode[] intToRoute(Integer... dstList) {
        return intToRoute((List<Integer>) Arrays.asList(dstList));
    }

    public static RouteNode[] intToRoute(List<Integer> dstList) {
        int size = dstList.size();
        RouteNode[] routeNodes = new RouteNode[size];
        if (size == 0) {
            return routeNodes;
        }
        for (int i = 0; i < size; i++) {
            RouteNode routeNode = new RouteNode();
            routeNode.setId(dstList.get(i));
            routeNodes[i] = routeNode;
        }
        return routeNodes;
    }
}
