package es.gob.afirma.android.exceptions;

import es.gob.afirma.core.ErrorCode;

public final class IncompatibleFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    private ErrorCode errorCode;

    public IncompatibleFormatException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}