package es.gob.afirma.android.crypto;

/**
 * Excepcion que identifica un error en la extraccion de claves del almacen del sistema
 * de Android 4.1 y 4.1.1.
 * Created by carlos on 14/07/2016.
 */
public class SelectKeyAndroid41BugException extends Exception {

    public SelectKeyAndroid41BugException() {
        super();
    }

    public SelectKeyAndroid41BugException(String msg) {
        super(msg);
    }

    public SelectKeyAndroid41BugException(Throwable cause) {
        super(cause);
    }

    public SelectKeyAndroid41BugException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
