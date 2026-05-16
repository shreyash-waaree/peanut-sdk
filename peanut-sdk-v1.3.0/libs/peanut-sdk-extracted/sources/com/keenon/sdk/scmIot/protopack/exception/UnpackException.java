package com.keenon.sdk.scmIot.protopack.exception;

/* JADX INFO: loaded from: peanut-sdk-release.aar:classes.jar:com/keenon/sdk/scmIot/protopack/exception/UnpackException.class */
public class UnpackException extends RuntimeException {
    public static final long serialVersionUID = 12;

    public UnpackException() {
        this("Unpack error");
    }

    public UnpackException(String message) {
        super(message);
    }

    public UnpackException(Throwable cause) {
        super(cause);
    }

    public UnpackException(String message, Throwable cause) {
        super(message, cause);
    }
}
