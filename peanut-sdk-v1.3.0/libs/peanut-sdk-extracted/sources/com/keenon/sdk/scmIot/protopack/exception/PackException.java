package com.keenon.sdk.scmIot.protopack.exception;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/exception/PackException.class */
public class PackException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public PackException() {
        this("PackError");
    }

    public PackException(String message) {
        super(message);
    }

    public PackException(String message, Throwable cause) {
        super(message, cause);
    }

    public PackException(Throwable cause) {
        super(cause);
    }
}
