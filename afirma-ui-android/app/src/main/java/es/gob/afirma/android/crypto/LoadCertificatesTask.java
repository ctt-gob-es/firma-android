package es.gob.afirma.android.crypto;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.security.auth.callback.CallbackHandler;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.errors.AppErrorCode;
import es.gob.afirma.android.errors.ErrorMapper;
import es.gob.afirma.android.gui.CertificateInfoForAliasSelect;
import es.gob.afirma.android.gui.SelectAliasDialog;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.jmulticard.card.dnie.InvalidAccessCodeException;
import es.gob.jmulticard.connection.ApduConnectionException;

/**
 * Created by a621914 on 09/06/2016.
 */
public class LoadCertificatesTask extends AsyncTask<Void, Void, Exception> {

    private static final String ES_GOB_AFIRMA = "es.gob.afirma";

    private final KeyStore ks;
    private final Activity activity;
    private KeyStoreManagerListener ksmListener;

    private ProgressDialog progressDialog = null;

    public LoadCertificatesTask(KeyStore ks, KeyStoreManagerListener ksmListener, Activity ac) {
        this.ks = ks;
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
            Logger.w(ES_GOB_AFIRMA, "No se ha definido un almacen de claves. Se usara el del sistema", e);
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
            final CachePasswordCallback pin = dnieManager.getPinPasswordCallback();
            final CachePasswordCallback can = dnieManager.getCanPasswordCallback();

            final CallbackHandler callbackHandler = new AndroidDnieNFCCallbackHandler(can, pin);
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
        catch (final InvalidAccessCodeException e) {
            // Se dara esta excepcion cuando el CAN sea incorrecto
            Logger.e(ES_GOB_AFIRMA, "El CAN es incorrecto: " + e); //$NON-NLS-1$
            dnieManager.clearCan();
            dnieManager.clearPin();
            throw e;
        }
        catch (final NullPointerException e) {
            // Se dara esta excepcion cuando no haya un KeyStore definido, lo que ocurrira cuando
            // se deba cargar el almacen del sistema
            Logger.e(ES_GOB_AFIRMA, "Error al cargar el almacen de claves"); //$NON-NLS-1$
            throw e;
        }
        catch (final ApduConnectionException e) {
            Logger.e(ES_GOB_AFIRMA, "Error de conexion con la tarjeta"); //$NON-NLS-1$
            throw new SmartCardConnectionException("Error de conexion con la tarjeta", e);
        }
        catch (final Exception e) {
            // Estamos en una conexion NFC y encapsulamos
            // las excepciones para que se procesen adecuadamente
            Logger.e(ES_GOB_AFIRMA, "Error al cargar el almacen de claves del dispositivo. Es posible que CAN o PIN introducido fuese incorrecto"); //$NON-NLS-1$
            throw new LoadingCertificateException("Error cargando los certificados del almacen", e);
        }

        // Obtenemos los elementos para el dialogo de seleccion
        final Enumeration<String> aliases;
        try {
            aliases = this.ks.aliases();
        } catch (final Exception e) {
            Logger.e(ES_GOB_AFIRMA, "Error extrayendo los alias de los certificados del almacen"); //$NON-NLS-1$
            throw new LoadingCertificateException("Error extrayendo los alias de los certificados del almacen", e);
        }

        final ArrayList<CertificateInfoForAliasSelect> arrayListCertificate = new ArrayList();

        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();
            X509Certificate cert;
            try {
                cert = (X509Certificate) this.ks.getCertificate(alias);
            } catch (final Exception e) {
                Logger.e(ES_GOB_AFIRMA, "Error obteniendo el certificado con alias: " + alias, e); //$NON-NLS-1$
                // Gestion a medida de un DNIe bloqueado (usando JMultiCard)
                if ("es.gob.jmulticard.card.AuthenticationModeLockedException".equals(e.getClass().getName())) { //$NON-NLS-1$
                    manageLockedDnie(e, this.activity, this.ksmListener);
                    return;
                }
                throw new LoadingCertificateException("Error accediendo al certificado", e);
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
            this.ksmListener.onLoadingKeyStoreError(new PendingIntent.CanceledException("Operacion cancelada"));
            return;
        }

        final SelectAliasDialog selectAlias = SelectAliasDialog.newInstance(
                arrayListCertificate,
                this.ksmListener
        );
        selectAlias.setKeyStore(ks);

        if (isCancelled()) {
            this.ksmListener.onLoadingKeyStoreError(new PendingIntent.CanceledException("Operacion cancelada"));
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

                dniBloqueado.setTitle(activity.getString(R.string.error_reading_dnie));
                dniBloqueado.setMessage(ErrorMapper.getErrorMsgFormatted(activity.getBaseContext(), AppErrorCode.ThirdParty.BLOCKED_CARD));
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
                    ksListener.onLoadingKeyStoreError(e
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
        //Si se pierde la conexion reiniciamos el proceso
        if (e != null) {
            this.ksmListener.onLoadingKeyStoreError(e);
        }
    }

    ProgressDialog getProgressDialog() {
        return this.progressDialog;
    }

    void setProgressDialog(final ProgressDialog pd) {
        this.progressDialog = pd;
    }

}
