package es.gob.afirma.android.crypto;

/**
 * Excepci&oacute;n que identifica un problema de conexi&oacute;n con una tarjeta inteligente.
 */
public final class SmartCardConnectionException extends Exception {

    public SmartCardConnectionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
