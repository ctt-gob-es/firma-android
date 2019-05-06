package es.gob.afirma.android.crypto;

/**
 * Excepci&oacute;n que identifica que se encontr&oacute; una tarjeta inteligente no soportada.
 */
public final class UnsupportedNfcCardException extends Exception {

    UnsupportedNfcCardException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
