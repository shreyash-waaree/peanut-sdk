package org.eclipse.californium.core.observe;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/NotificationListener.class */
public interface NotificationListener {
    void onNotification(Request request, Response response);
}
