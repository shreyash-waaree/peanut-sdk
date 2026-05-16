package org.eclipse.californium.core;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.eclipse.californium.core.coap.ClientObserveRelation;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.observe.NotificationListener;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/CoapObserveRelation.class */
public class CoapObserveRelation extends ClientObserveRelation {
    private volatile CoapResponse current;
    private volatile NotificationListener notificationListener;

    protected CoapObserveRelation(Request request, Endpoint endpoint, ScheduledThreadPoolExecutor executor) {
        super(request, endpoint, executor);
        this.current = null;
    }

    public synchronized CoapResponse waitForResponse(long timeoutMillis) {
        if (this.current == null) {
            try {
                wait(timeoutMillis);
            } catch (InterruptedException e) {
            }
        }
        return this.current;
    }

    public CoapResponse getCurrent() {
        return this.current;
    }

    @Override // org.eclipse.californium.core.coap.ClientObserveRelation
    protected void setCanceled(boolean canceled) {
        super.setCanceled(canceled);
        if (canceled && this.notificationListener != null) {
            this.endpoint.removeNotificationListener(this.notificationListener);
        }
    }

    public void setNotificationListener(NotificationListener listener) {
        this.notificationListener = listener;
    }

    protected boolean onResponse(CoapResponse response) {
        boolean isNew = false;
        if (null != response) {
            isNew = super.onResponse(response.advanced());
            if (isNew) {
                synchronized (this) {
                    this.current = response;
                    notifyAll();
                }
            }
        }
        return isNew;
    }

    @Override // org.eclipse.californium.core.coap.ClientObserveRelation
    public boolean onResponse(Response response) {
        if (super.onResponse(response)) {
            synchronized (this) {
                this.current = new CoapResponse(response);
                notifyAll();
            }
            return true;
        }
        return false;
    }
}
