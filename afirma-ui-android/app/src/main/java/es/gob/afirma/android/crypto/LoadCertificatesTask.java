package es.gob.afirma.android.crypto;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.widget.Toast;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.gui.CertificateInfoForAliasSelect;
import es.gob.afirma.android.gui.SelectAliasDialog;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.jmulticard.android.callbacks.CachePasswordCallback;
import es.gob.jmulticard.android.callbacks.DialogDoneChecker;
import es.gob.jmulticard.android.callbacks.DnieNFCCallbackHandler;

/**
 * Created by a621914 on 09/06/2016.
 */
public class LoadCertificatesTask extends AsyncTask<Void, Void, Exception> {

    private static final String ES_GOB_AFIRMA = "es.gob.afirma";

    private final KeyStore ks;
    private final CachePasswordCallback passwordCallback;
    private final Activity activity;
    private KeyStoreManagerListener ksmListener;

    private ProgressDialog progressDialog = null;

    public LoadCertificatesTask(KeyStore ks, KeyStoreManagerListener ksmListener, Activity ac) {
        this.ks = ks;
        this.passwordCallback = null;
        this.activity = ac;
        this.ksmListener = ksmListener;
    }

    public LoadCertificatesTask(KeyStore ks, CachePasswordCallback pc, KeyStoreManagerListener ksmListener, Activity ac) {
        this.ks = ks;
        this.passwordCallback = pc;
        this.activity = ac;
        this.ksmListener = ksmListener;
    }

    /**
     * Muestra un di&acute;logo de carga mientras ejecuta la tarea en segundo plano.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        setProgressDialog(
                ProgressDialog.show(
                        this.activity,
                        "",
                        "Cargando almac\u00E9n de claves del dispositivo",
                        true)); //$NON-NLS-1$
    }

    @Override
    protected Exception doInBackground(Void... params) {

        try {
            loadCertificatesFromKeyStore();
        }
        catch (NullPointerException e) {
            // Esto ocurrira cuando no se haya definido un KeyStore especifico, por lo que se usara
            // el almacen del sistema
        }
        catch (Exception e) {
            Logger.e(ES_GOB_AFIRMA, "No se pudieron cargar los certificados del almacen: " + e);
            return e;
        }

        return null;
    }

    private void loadCertificatesFromKeyStore() throws Exception {
        final DialogDoneChecker ddc = new DialogDoneChecker();
        try {
            this.ks.load(
                    new KeyStore.LoadStoreParameter() {
                        @Override
                        public KeyStore.ProtectionParameter getProtectionParameter() {
                            return new KeyStore.CallbackHandlerProtection(
                                    new DnieNFCCallbackHandler(activity, ddc, LoadCertificatesTask.this.passwordCallback)
                            );
                        }
                    }
            );
        }
        catch (final NullPointerException e) {
            // Se dara esta excepcion cuando no haya un KeyStore definido, lo que ocurrira cuando
            // se deba cargar el almacen del sistema
            Logger.e(ES_GOB_AFIRMA, "Se cargan los almacenes del sistema"); //$NON-NLS-1$
            throw e;
        }
        catch (final Exception e) {
            Logger.e(ES_GOB_AFIRMA, "Error al cargar el almacen de claves del dispositivo: " + e); //$NON-NLS-1$

            // Si tenemos definido un passwordCallback, estamos en una conexion NFC y encapsulamos
            // las excepciones para que se procesen adecuadamente
            throw encapsuleException(e);
        }
        // Obtenemos los elementos para el dialogo de seleccion
        final Enumeration<String> aliases;
        try {
            aliases = this.ks.aliases();
        } catch (final Exception e) {
            Logger.e(ES_GOB_AFIRMA, "Error extrayendo los alias de los certificados del almacen: " + e); //$NON-NLS-1$
            throw encapsuleException(e);
        }

        final ArrayList<CertificateInfoForAliasSelect> arrayListCertificate = new ArrayList();

        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();
            X509Certificate cert;
            try {
                cert = (X509Certificate) this.ks.getCertificate(alias);
            } catch (final KeyStoreException e) {
                Logger.w(ES_GOB_AFIRMA, "No se ha podido extraer el certificado '" + alias + "': " + e);  //$NON-NLS-1$//$NON-NLS-2$
                throw encapsuleException(e);
            } catch (final Exception e) {
                // Gestion a medida de un DNIe bloqueado (usando JMultiCard)
                if ("es.gob.jmulticard.card.AuthenticationModeLockedException".equals(e.getClass().getName())) { //$NON-NLS-1$
                    manageLockedDnie(e, this.activity);
                    return;
                }
                Logger.e(ES_GOB_AFIRMA, "Error obteniendo el certificado con alias '" + alias + "': " + e, e); //$NON-NLS-1$ //$NON-NLS-2$
                throw encapsuleException(e);
            }
            arrayListCertificate.add(
                    new CertificateInfoForAliasSelect(
                            AOUtil.getCN(cert),
                            cert.getNotBefore(), cert.getNotAfter(),
                            alias,
                            AOUtil.getCN(cert.getIssuerX500Principal().toString())
                    )
            );

        }

//        if (KeyStoreManagerFactory.ksflStatic == null) {
//            Logger.e(ES_GOB_AFIRMA, "No se ha establecido la tarea para la obtencion del almacen de certificados con setLoadKeyStoreManagerTask()");  //$NON-NLS-1$
//            if (KeyStoreManagerFactory.ksmlStatic != null) {
//                KeyStoreManagerFactory.ksmlStatic.onLoadingKeyStoreError(
//                        "No se ha establecido la tarea para la obtencion del almacen de certificados con setLoadKeyStoreManagerTask()", null
//                );
//            }
//            return;
//        }

        final SelectAliasDialog selectAlias = SelectAliasDialog.newInstance(
                arrayListCertificate,
                KeyStoreManagerFactory.ksmlStatic
        );
        selectAlias.setKeyStore(ks);
        //No queremos que muestre todos los certificados del DNIe, sino que firme con el certificado de firma
        //selectAlias.show(this.activity.getFragmentManager(), "SelectAliasDialog"); //$NON-NLS-1$
        selectAlias.signWithSignCertificate();
    }

    private static void manageLockedDnie(final Throwable e, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Logger.e(ES_GOB_AFIRMA, "El DNIe esta bloqueado: " + e); //$NON-NLS-1$

                final AlertDialog.Builder dniBloqueado = new AlertDialog.Builder(activity);

                dniBloqueado.setTitle(activity.getString(R.string.error_title_dni_blocked));
                dniBloqueado.setMessage(activity.getString(R.string.error_dni_blocked_dlg));
                dniBloqueado.setPositiveButton(
                        activity.getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface d, final int id) {
                                d.dismiss();
                            }
                        }
                );
                dniBloqueado.create();
                dniBloqueado.show();

                if (KeyStoreManagerFactory.ksmlStatic != null) {
                    KeyStoreManagerFactory.ksmlStatic.onLoadingKeyStoreError(
                            activity.getString(R.string.error_dni_blocked), e
                    );
                }
            }
        });
    }
    
    /**
     *Elimina el di&acute;logo de carga cuando termina la tarea en segundo plano.
     */
    @Override
    protected void onPostExecute(Exception e) {
        super.onPostExecute(e);
        if (getProgressDialog().isShowing()) {
            getProgressDialog().dismiss();
        }
        //Si se pierde la conexion reininciamos el proceso
        if(e != null) {

            Toast.makeText(this.activity, "No se han podido cargar los certificados. Reintentando la conexión.", Toast.LENGTH_SHORT).show();

            this.ksmListener.onLoadingKeyStoreError("Error cargando los certificados. Se reintentara la conexion", e);
        }
    }

    /**
     * Encapsula una excepci&oacute;n para indicar el tipo de error general durante la carga.
     * @param e Excepci&oacute;n a encapsular.
     * @return Excepci&oacute;n general.
     */
    private Exception encapsuleException(final Exception e) {
        Exception ex;
        if (this.passwordCallback != null) {
            ex = new InitializingNfcCardException("Error cargando los certificados del almacen", e);
        }
        else {
            ex = new LoadingCertificateException("Error cargando los certificados del almacen", e);
        }
        return ex;
    }

    ProgressDialog getProgressDialog() {
        return this.progressDialog;
    }

    void setProgressDialog(final ProgressDialog pd) {
        this.progressDialog = pd;
    }

}
