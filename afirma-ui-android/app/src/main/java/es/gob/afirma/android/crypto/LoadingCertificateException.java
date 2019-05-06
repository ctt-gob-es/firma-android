package es.gob.afirma.android.crypto;

/**
 * Excepci&oacute;n que identifica un problema al cargar los certificados de un almac&eacute;n.
 */
public final class LoadingCertificateException extends Exception {

    public LoadingCertificateException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
