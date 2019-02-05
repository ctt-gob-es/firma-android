package es.gob.afirma.android.crypto;

/**
 * Excepci&oacute;n que identifica un error el proceso de inicializaci&oacute;n de una tarjeta
 * inteligente.
 */
public class InitializingNfcCardException extends Exception {

    /**
     * Crea la excepcion con un texto descriptivo del problema y la causa interna del mismo.
     * @param msg Mensaje descriptivo.
     * @param cause Causa del error.
     */
    public InitializingNfcCardException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
