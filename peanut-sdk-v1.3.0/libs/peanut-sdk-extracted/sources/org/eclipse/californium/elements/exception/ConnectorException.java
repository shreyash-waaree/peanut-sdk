package org.eclipse.californium.elements.exception;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/exception/ConnectorException.class */
public class ConnectorException extends Exception {
    private static final long serialVersionUID = 1;

    public ConnectorException() {
    }

    public ConnectorException(String message) {
        super(message);
    }

    @Override // java.lang.Throwable
    public String getMessage() {
        String msg = super.getMessage();
        if (msg == null) {
            msg = getClass().getSimpleName();
        }
        return msg;
    }
}
