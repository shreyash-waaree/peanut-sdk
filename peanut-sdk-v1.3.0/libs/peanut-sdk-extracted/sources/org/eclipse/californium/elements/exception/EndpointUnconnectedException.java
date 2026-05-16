package org.eclipse.californium.elements.exception;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/exception/EndpointUnconnectedException.class */
public class EndpointUnconnectedException extends ConnectorException {
    private static final long serialVersionUID = 1;

    public EndpointUnconnectedException() {
    }

    public EndpointUnconnectedException(String message) {
        super(message);
    }
}
