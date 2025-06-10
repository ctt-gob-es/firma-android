package es.gob.afirma.android.crypto;

import es.gob.afirma.android.LoadKeyStoreFragmentActivity;

/** Interfaz para la notificaci&oacute;n de finalizaci&oacute;n de la
 * carga e inicializaci&oacute;n del gestor de claves y certificados. */
public interface KeyStoreManagerListener {

    /** Establece un gestor de claves y certificados ya inicializado.
     * @param msm Gestor de claves y certificados */
    void onLoadingKeyStoreSuccess(MobileKeyStoreManager msm);

    /** Establece el error que hizo fallar la carga del almac&eacute;n de certificados.
     * @param t Error capturado. */
    void onLoadingKeyStoreError(Throwable t);

    void onKeyStoreError(LoadKeyStoreFragmentActivity.KeyStoreOperation op, Throwable t);
}
