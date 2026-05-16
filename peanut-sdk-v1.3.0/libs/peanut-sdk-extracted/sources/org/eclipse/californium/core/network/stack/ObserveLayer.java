package org.eclipse.californium.core.network.stack;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/ObserveLayer.class */
public class ObserveLayer extends AbstractLayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObserveLayer.class);

    public ObserveLayer(Configuration config) {
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void sendResponse(Exchange exchange, Response response) {
        ObserveRelation relation = exchange.getRelation();
        if (relation != null && relation.isEstablished()) {
            if (response.isSuccess() ^ response.isNotification()) {
                if (response.isNotification()) {
                    LOGGER.warn("Error notification, remove observe-option {}", response);
                    response.getOptions().removeObserve();
                } else {
                    LOGGER.warn("No-notification response with observe-relation {}, drop.", response);
                    response.setSendError(new IllegalArgumentException("Notification must have observe-option!"));
                    return;
                }
            }
            if (exchange.getRequest().isAcknowledged() || exchange.getRequest().getType() == CoAP.Type.NON) {
                if (!response.isSuccess()) {
                    LOGGER.debug("response has error code {} and must be sent as CON", response.getCode());
                    response.setType(CoAP.Type.CON);
                } else if (relation.check()) {
                    LOGGER.debug("observe relation check requires the notification to be sent as CON");
                    response.setType(CoAP.Type.CON);
                } else if (response.getType() == null) {
                    LOGGER.debug("observe relation sent the message as NON (default)");
                    response.setType(CoAP.Type.NON);
                }
            }
            if (response.getType() == CoAP.Type.CON) {
                prepareSelfReplacement(exchange, response);
            }
            if (relation.isPostponedNotification(response)) {
                LOGGER.debug("a former notification is still in transit. Postponing {}", response);
                return;
            }
        } else if (response.isNotification()) {
            LOGGER.warn("Notification without observe-relation, remove observe-option {}", response);
            response.getOptions().removeObserve();
        }
        lower().sendResponse(exchange, response);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveResponse(Exchange exchange, Response response) {
        if (response.isNotification() && exchange.getRequest().isCanceled()) {
            LOGGER.debug("rejecting notification for canceled Exchange");
            EmptyMessage rst = EmptyMessage.newRST(response);
            sendEmptyMessage(exchange, rst);
            return;
        }
        upper().receiveResponse(exchange, response);
    }

    @Override // org.eclipse.californium.core.network.stack.AbstractLayer, org.eclipse.californium.core.network.stack.Layer
    public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
        ObserveRelation relation;
        if (message.getType() == CoAP.Type.RST && exchange.getOrigin() == Exchange.Origin.REMOTE && exchange.getCurrentResponse().isNotification() && (relation = exchange.getRelation()) != null) {
            relation.cleanup();
        }
        upper().receiveEmptyMessage(exchange, message);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareSelfReplacement(Exchange exchange, Response response) {
        response.addMessageObserver(new NotificationController(exchange, response));
    }

    /* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-californium-core.jar:org/eclipse/californium/core/network/stack/ObserveLayer$NotificationController.class */
    private class NotificationController extends MessageObserverAdapter {
        private final Exchange exchange;
        private final Response response;

        public NotificationController(Exchange exchange, Response response) {
            this.exchange = exchange;
            this.response = response;
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onAcknowledgement() {
            this.exchange.execute(new Runnable() { // from class: org.eclipse.californium.core.network.stack.ObserveLayer.NotificationController.1
                @Override // java.lang.Runnable
                public void run() {
                    ObserveRelation relation = NotificationController.this.exchange.getRelation();
                    boolean canceled = relation.isCanceled();
                    Response next = relation.getNextNotification(NotificationController.this.response, true);
                    if (next != null) {
                        ObserveLayer.LOGGER.debug("notification has been acknowledged, send the next one");
                        if (!canceled) {
                            ObserveLayer.super.sendResponse(NotificationController.this.exchange, next);
                        } else {
                            next.cancel();
                        }
                    }
                }
            });
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onRetransmission() {
            ObserveRelation relation = this.exchange.getRelation();
            boolean canceled = relation.isCanceled();
            Response next = relation.getNextNotification(this.response, false);
            if (canceled) {
                this.response.cancel();
                if (next != null) {
                    next.cancel();
                    next = null;
                }
            }
            if (next != null) {
                ObserveLayer.LOGGER.debug("notification has timed out and there is a fresher notification for the retransmission");
                if (next.getType() != CoAP.Type.CON) {
                    next.setType(CoAP.Type.CON);
                    ObserveLayer.this.prepareSelfReplacement(this.exchange, next);
                }
                this.response.cancel();
                ObserveLayer.super.sendResponse(this.exchange, next);
            }
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter, org.eclipse.californium.core.coap.MessageObserver
        public void onTimeout() {
            ObserveRelation relation = this.exchange.getRelation();
            ObserveLayer.LOGGER.info("notification for token [{}] timed out. Canceling all relations with source [{}]", relation.getExchange().getRequest().getToken(), StringUtil.toLog(relation.getSource()));
            relation.cancelAll();
        }

        @Override // org.eclipse.californium.core.coap.MessageObserverAdapter
        protected void failed() {
            ObserveRelation relation = this.exchange.getRelation();
            ObserveLayer.LOGGER.info("notification for token [{}] failed. Source [{}].", relation.getExchange().getRequest().getToken(), StringUtil.toLog(relation.getSource()));
            relation.cancel();
        }
    }
}
