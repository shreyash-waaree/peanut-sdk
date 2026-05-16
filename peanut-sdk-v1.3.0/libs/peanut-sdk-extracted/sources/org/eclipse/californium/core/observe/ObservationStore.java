package org.eclipse.californium.core.observe;

import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.elements.EndpointContext;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/observe/ObservationStore.class */
public interface ObservationStore {
    Observation putIfAbsent(Token token, Observation observation);

    Observation put(Token token, Observation observation);

    void remove(Token token);

    Observation get(Token token);

    void setContext(Token token, EndpointContext endpointContext);

    void setExecutor(ScheduledExecutorService scheduledExecutorService);

    void start();

    void stop();
}
