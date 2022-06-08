package es.gob.afirma.android.exceptions;

public final class InvalidEncryptedDataLengthException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidEncryptedDataLengthException(final String msg) {
        super(msg);
    }
}
