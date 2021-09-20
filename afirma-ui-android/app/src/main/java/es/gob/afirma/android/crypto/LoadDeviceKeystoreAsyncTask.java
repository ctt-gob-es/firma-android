/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.android.crypto;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

import androidx.fragment.app.FragmentActivity;

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

/**
 * Created by Mariano Mart&icute;nez on 10/2/16.
 * Carga el almac&ecute;n de claves del dispositivo de forma as&icute;ncrona y muestra un di&acute;logo de carga.
 */
public class LoadDeviceKeystoreAsyncTask extends AsyncTask<Void, Void, Void> {

    private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

    private final FragmentActivity activity;
    FragmentActivity getActivity () { return this.activity; }

    private final String pin;
    String getPin () { return this.pin; }

    private final String keystore;
    String getKeystore () { return  this.keystore; }

    private final String provider;
    String getProvider () { return this.provider; }

    private ProgressDialog progressDialog = null;
    ProgressDialog getProgressDialog() {
        return this.progressDialog;
    }
    void setProgressDialog(final ProgressDialog pd) {
        this.progressDialog = pd;
    }

    private KeyStoreManagerListener ksmListener;
    KeyStoreManagerListener getKsmListener() {
        return this.ksmListener;
    }

    /**
     * Constructor de la tarea as&icute;ncrona de carga de almac&ecute;n de claves del dispositivo.
     * @param activity Actividad desde la que se llama a la tarea.
     * @param pin Pin del dispositivo.
     * @param keystore Nombre del almac&ecute;n.
     * @param provider Nombre del proveedor.
     * @param ksml Manejador del resultado de la carga del almac&ecute;n.
     */
    public LoadDeviceKeystoreAsyncTask(FragmentActivity activity,
                                       String pin,
                                       String keystore,
                                       String provider,
                                       KeyStoreManagerListener ksml) {
        this.activity = activity;
        this.pin = pin;
        this.keystore = keystore;
        this.provider = provider;
        this.ksmListener = ksml;
    }
    /**
     * Muestra un di&acute;logo de carga mientras ejecuta la tarea en segundo plano.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        setProgressDialog(ProgressDialog.show(getActivity(), "", "Cargando almac\u00E9n de claves del dispositivo", true)); //$NON-NLS-1$
    }

    /**
     * Tarea de carga del almac&ecute;n de claves del dispositivo en segundo plano.
     *
     * @param params No tiene par&acute;metros.
     * @return No retorna ningun valor.
     */
    @Override
    protected Void doInBackground(Void... params) {

        // Aqui tenemos el PIN, empezamos con la inicializacion del almacen
        final KeyStore ks;
        try {
            ks = KeyStore.getInstance(getKeystore(), getProvider());
            ks.load(null, getPin().toCharArray());
        }
        catch(final Exception e) {
            Logger.e(ES_GOB_AFIRMA, "Error al cargar el almacen de claves del dispositivo: " + e); //$NON-NLS-1$
            if (getKsmListener() != null) {
                getKsmListener().onLoadingKeyStoreError(
                        "Error al cargar el almacen de claves del dispositivo", e); //$NON-NLS-1$
            }
            return null;
        }

        // Obtenemos los elementos para el dialogo de seleccion
        final Enumeration<String> aliases;
        try {
            aliases = ks.aliases();
        }
        catch(final Exception e) {
            Logger.e(ES_GOB_AFIRMA, "Error extrayendo los alias de los certificados del almacen: " + e); //$NON-NLS-1$
            if (getKsmListener() != null) {
                getKsmListener().onLoadingKeyStoreError(
                        "Error extrayendo los alias de los certificados del almacen: ", e); //$NON-NLS-1$
            }
            return null;
        }

        final ArrayList<CertificateInfoForAliasSelect> arrayListCertificate = new ArrayList();

        while(aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();
            X509Certificate cert;
            try {
                cert = (X509Certificate) ks.getCertificate(alias);
            }
            catch (final KeyStoreException e) {
                Logger.w(ES_GOB_AFIRMA, "No se ha podido extraer el certificado '" + alias + "': " + e);  //$NON-NLS-1$//$NON-NLS-2$
                continue;
            }
            catch(final Exception e) {
                // Gestion a medida de un DNIe bloqueado (usando JMultiCard)
                if ("es.gob.jmulticard.card.AuthenticationModeLockedException".equals(e.getClass().getName())) { //$NON-NLS-1$
                    manageLockedDnie(e);
                    return null;
                }
                Logger.e(ES_GOB_AFIRMA,"Error obteniendo el certificado con alias '" + alias + "': " + e); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            // Comprobamos si tiene clave privada o no
            try {
                ks.getEntry(
                        alias,
                        new KeyStore.PasswordProtection(getPin().toCharArray())
                );
            }
            catch(final Exception e) {
                Logger.w(ES_GOB_AFIRMA, "Se omite el certificado '" + AOUtil.getCN(cert) + "' por no tener clave privada: " + e); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
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

//        if(getKsmTask() == null){
//            Logger.e(ES_GOB_AFIRMA, "No se ha establecido la tarea para la obtencion del almacen de certificados con setLoadKeyStoreManagerTask()");  //$NON-NLS-1$
//            if (getKsmListener() != null) {
//                getKsmListener().onLoadingKeyStoreError(
//                        "No se ha establecido la tarea para la obtencion del almacen de certificados con setLoadKeyStoreManagerTask()", null
//                );
//            }
//            return null;
//        }

        final SelectAliasDialog selectAlias = SelectAliasDialog.newInstance(
                arrayListCertificate,
                getKsmListener()
        );
        selectAlias.setKeyStore(ks);
        selectAlias.setPin(getPin().toCharArray());
        getProgressDialog().dismiss();
        selectAlias.show(getActivity().getSupportFragmentManager(), "SelectAliasDialog"); //$NON-NLS-1$
        return null;
    }


    /**
     *Elimina el di&acute;logo de carga cuando termina la tarea en segundo plano.
     */
    @Override
    protected void onPostExecute(Void o) {
        super.onPostExecute(o);
        if (getProgressDialog().isShowing()) {
            getProgressDialog().dismiss();
        }
    }

    private void manageLockedDnie(final Throwable e) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Logger.e(ES_GOB_AFIRMA, "El DNIe esta bloqueado: " + e); //$NON-NLS-1$

                final AlertDialog.Builder dniBloqueado = new AlertDialog.Builder(getActivity());

                dniBloqueado.setTitle(getActivity().getString(R.string.error_title_dni_blocked));
                dniBloqueado.setMessage(getActivity().getString(R.string.error_dni_blocked_dlg));
                dniBloqueado.setPositiveButton(
                        getActivity().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface d, final int id) {
                                d.dismiss();
                            }
                        }
                );
                dniBloqueado.create();
                dniBloqueado.show();

                if (getKsmListener() != null) {
                    getKsmListener().onLoadingKeyStoreError(
                            getActivity().getString(R.string.error_dni_blocked), e
                    );
                }
            }
        });
    }
}
