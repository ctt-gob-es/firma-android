package es.gob.afirma.android.exceptions;

public final class DecryptionException extends Exception {

    private static final long serialVersionUID = 1L;

    public DecryptionException(final String msg, final Throwable e) {
        super(msg, e);
    }
}