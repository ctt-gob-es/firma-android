package es.gob.afirma.android.crypto;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.security.auth.callback.CallbackHandler;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.gui.CertificateInfoForAliasSelect;
import es.gob.afirma.android.gui.SelectAliasDialog;
import es.gob.afirma.core.misc.AOUtil;

/**
 * Created by a621914 on 09/06/2016.
 */
public class LoadCertificatesTask extends AsyncTask<Void, Void, Exception> {

    private static final String ES_GOB_AFIRMA = "es.gob.afirma";

    private final KeyStore ks;
    private final CachePasswordCallback ksPasswordCallback;
    private final Activity activity;
    private KeyStoreManagerListener ksmListener;

    private ProgressDialog progressDialog = null;

    public LoadCertificatesTask(KeyStore ks, KeyStoreManagerListener ksmListener, Activity ac) {
        this.ks = ks;
        this.ksPasswordCallback = null;
        this.activity = ac;
        this.ksmListener = ksmListener;
    }

    public LoadCertificatesTask(KeyStore ks, CachePasswordCallback pc, KeyStoreManagerListener ksmListener, Activity ac) {
        this.ks = ks;
        this.ksPasswordCallback = pc;
        this.activity = ac;
        this.ksmListener = ksmListener;
    }

    /**
     * Muestra un di&acute;logo de carga mientras ejecuta la tarea en segundo plano.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        try {
            setProgressDialog(
                    ProgressDialog.show(
                            this.activity,
                            "",
                            this.activity.getString(R.string.loading_certs),
                            true)); //$NON-NLS-1$
        }
        catch (Throwable e) {
            Logger.w(ES_GOB_AFIRMA, "No se pudo mostrar el dialogo de progreso de carga del almacen de certificados", e);
        }
    }

    @Override
    protected Exception doInBackground(Void... params) {

        try {
            loadCertificatesFromKeyStore();
        }
        catch (NullPointerException e) {
            // Esto ocurrira cuando no se haya definido un KeyStore especifico, por lo que se usara
            // el almacen del sistema
            Logger.i(ES_GOB_AFIRMA, "No se ha definido un almacen de claves. Se usara el del sistema");
        }
        catch (Exception e) {
            Logger.e(ES_GOB_AFIRMA, "No se pudieron cargar los certificados del almacen: " + e);
            return e;
        }

        return null;
    }

    private void loadCertificatesFromKeyStore() throws Exception {
        DnieConnectionManager dnieManager = DnieConnectionManager.getInstance();
        try {
            // Si no se ha inicializado el gestor para las solicitudes de claves del DNIe,
            // lo inicializamos ahora
            AndroidDnieNFCCallbackHandler dnieCallbackHandler = dnieManager.getCallbackHandler();
            if (dnieCallbackHandler == null) {
                CachePasswordCallback pin = dnieManager.getPinPasswordCallback();
                dnieCallbackHandler = new AndroidDnieNFCCallbackHandler(LoadCertificatesTask.this.ksPasswordCallback, pin);
                dnieManager.setCallbackHandler(dnieCallbackHandler);
            }

            final CallbackHandler callbackHandler = dnieCallbackHandler;

            this.ks.load(
                    new KeyStore.LoadStoreParameter() {
                        @Override
                        public KeyStore.ProtectionParameter getProtectionParameter() {
                            return new KeyStore.CallbackHandlerProtection(
                                    callbackHandler
                            );
                        }
                    }
            );
        }
        catch (final NullPointerException e) {
            // Se dara esta excepcion cuando no haya un KeyStore definido, lo que ocurrira cuando
            // se deba cargar el almacen del sistema
            Logger.e(ES_GOB_AFIRMA, "Error al cargar el almacen de claves"); //$NON-NLS-1$
            dnieManager.setCallbackHandler(null);
            throw e;
        }
        catch (final Exception e) {
            // Estamos en una conexion NFC y encapsulamos
            // las excepciones para que se procesen adecuadamente
            Logger.e(ES_GOB_AFIRMA, "Error al cargar el almacen de claves del dispositivo. Es posible que CAN o PIN introducido fuese incorrecto: " + e); //$NON-NLS-1$
            dnieManager.clearCan();
            dnieManager.clearPin();
            dnieManager.setCallbackHandler(null);
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
                    manageLockedDnie(e, this.activity, this.ksmListener);
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

        if (isCancelled()) {
            this.ksmListener.onLoadingKeyStoreError("Operacion cancelada", null);
            return;
        }

        final SelectAliasDialog selectAlias = SelectAliasDialog.newInstance(
                arrayListCertificate,
                this.ksmListener
        );
        selectAlias.setKeyStore(ks);

        if (isCancelled()) {
            this.ksmListener.onLoadingKeyStoreError("Operacion cancelada", null);
            return;
        }

        // Firmamos directamente con el certificado de firma
        selectAlias.signWithSignCertificate();
    }

    private static void manageLockedDnie(final Throwable e, final Activity activity, final KeyStoreManagerListener ksListener) {
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

                if (ksListener != null) {
                    ksListener.onLoadingKeyStoreError(
                            activity.getString(R.string.error_dni_blocked), e
                    );
                }
            }
        });
    }
    
    /**
     * Elimina el di&acute;logo de carga cuando termina la tarea en segundo plano.
     * @param e Excepci&oacute;n que hace fallar la operaci&oacute;n o {@code null}
     *          cuando todo ha funcionado correctamente.
     */
    @Override
    protected void onPostExecute(Exception e) {
        super.onPostExecute(e);
        if (getProgressDialog().isShowing()) {
            getProgressDialog().dismiss();
        }
        //Si se pierde la conexion reininciamos el proceso
        if(e != null) {

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
        if (this.ksPasswordCallback != null) {
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
