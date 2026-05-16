package org.eclipse.californium.core.network;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.Token;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/MessageExchangeStore.class */
public interface MessageExchangeStore {
    void start();

    void stop();

    int assignMessageId(Message message);

    boolean registerOutboundRequest(Exchange exchange);

    boolean registerOutboundRequestWithTokenOnly(Exchange exchange);

    boolean registerOutboundResponse(Exchange exchange);

    void remove(KeyToken keyToken, Exchange exchange);

    Exchange remove(KeyMID keyMID, Exchange exchange);

    Exchange get(KeyToken keyToken);

    Exchange get(KeyMID keyMID);

    Exchange findPrevious(KeyMID keyMID, Exchange exchange);

    boolean replacePrevious(KeyMID keyMID, Exchange exchange, Exchange exchange2);

    Exchange find(KeyMID keyMID);

    boolean isEmpty();

    List<Exchange> findByToken(Token token);

    void setExecutor(ScheduledExecutorService scheduledExecutorService);
}
