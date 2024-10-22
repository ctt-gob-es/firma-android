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

import android.app.ProgressDialog;
import android.os.AsyncTask;

import androidx.fragment.app.FragmentActivity;

import java.security.KeyStore;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;

/** Tarea de carga e inicializaci&oacute;n de los almacenes de claves por conexion NFC. */
public final class LoadNfcKeyStoreManagerTask extends AsyncTask {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private final KeyStoreManagerListener kmListener;
	private final FragmentActivity activity;
	private final CachePasswordCallback passwordCallback;
	private ProgressDialog progressDialog = null;

	/** Crea una tarea de carga e inicializaci&oacute;n de un almacen de claves por NFC.
	 * @param kml Clase a la que hay que notificar cuando se finaliza la
	 * carga e inicializaci&oacute;n del gestor de claves y certificados
	 * @param act Actividad padre
	 * @param passwordCallback Callback con la contrase&ntilde;a cacheada. */
	public LoadNfcKeyStoreManagerTask(final KeyStoreManagerListener kml,
									  final FragmentActivity act,
									  final CachePasswordCallback passwordCallback) {
		this.kmListener = kml;
		this.activity = act;
		this.passwordCallback = passwordCallback;
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
						this.activity.getString(R.string.dialog_msg_loading_keystore),
						true)); //$NON-NLS-1$
	}

	@Override
	protected Object doInBackground(Object[] params) {

		Logger.i(ES_GOB_AFIRMA, "Inicializamos el almacen"); //$NON-NLS-1$
		//Se obtiene el KeyStore

		KeyStore ks;
		try {
			ks = KeyStoreManagerFactory.initNfcKeyStoreManager(this.kmListener);
		}
		catch (UnsupportedNfcCardException e) {
			return e;
		}
		catch (InitializingNfcCardException e) {
			return e;
		}
		return ks;
	}

	/**
	 *Elimina el di&acute;logo de carga cuando termina la tarea en segundo plano.
	 */
	@Override
	protected void onPostExecute(Object o) {
		super.onPostExecute(o);

		if (getProgressDialog().isShowing()) {
			getProgressDialog().dismiss();
		}

		// Si es una excepcion, se notifica un problema en la carga del almacen
		if (o instanceof Exception) {
			Exception e = (Exception) o;
			this.kmListener.onLoadingKeyStoreError("Error en la carga de la tarjeta NFC", e);
		}
		else {
			//Se cargan los certificados del keystore
			new LoadCertificatesTask((KeyStore) o, this.passwordCallback, this.kmListener, this.activity).execute();
		}
  	}

	ProgressDialog getProgressDialog() {
		return this.progressDialog;
	}
	void setProgressDialog(final ProgressDialog pd) {
		this.progressDialog = pd;
	}
}
