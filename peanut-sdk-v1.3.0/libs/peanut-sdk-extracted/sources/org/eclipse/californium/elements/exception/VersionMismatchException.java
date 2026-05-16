package org.eclipse.californium.elements.exception;

/* JADX INFO: loaded from: peanut-sdk-release.aar:libs/coap-element-connector.jar:org/eclipse/californium/elements/exception/VersionMismatchException.class */
public class VersionMismatchException extends IllegalArgumentException {
    private static final long serialVersionUID = 1;
    private final int readVersion;

    public VersionMismatchException(int readVersion) {
        this.readVersion = readVersion;
    }

    public VersionMismatchException(String message, int readVersion) {
        super(message);
        this.readVersion = readVersion;
    }

    public int getReadVersion() {
        return this.readVersion;
    }
}
