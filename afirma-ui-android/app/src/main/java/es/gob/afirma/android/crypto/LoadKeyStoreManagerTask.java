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
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;

import androidx.fragment.app.FragmentActivity;

import java.security.KeyStore;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;

/** Tarea de carga e inicializaci&oacute;n del gestor de claves y certificados en Android. */
public final class LoadKeyStoreManagerTask extends AsyncTask<Void, Void, KeyStore> {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private final KeyStoreManagerListener kmListener;
	private final FragmentActivity activity;
	private final UsbDevice usbDevice;
	private final UsbManager usbManager;
	private ProgressDialog progressDialog = null;

	/** Crea una tarea de carga e inicializaci&oacute;n del gestor de claves y certificados en Android.
	 * @param kml Clase a la que hay que notificar cuando se finaliza la
	 * carga e inicializaci&oacute;n del gestor de claves y certificados
	 * @param act Actividad padre */
	public LoadKeyStoreManagerTask(final KeyStoreManagerListener kml, final FragmentActivity act) {
		this.kmListener = kml;
		this.activity = act;
		this.usbDevice = null;
		this.usbManager = null;
	}

	/** Crea una tarea de carga e inicializaci&oacute;n del gestor de claves y certificados en Android.
	 * @param kml Clase a la que hay que notificar cuando se finaliza la
	 * carga e inicializaci&oacute;n del gestor de claves y certificados
	 * @param act Actividad padre
	 * @param usbDev Dispositivo USB en el caso de almacenes de claves externos
	 * @param usbMgr Gestor de dispositivos USB en el caso de almacenes de claves externos*/
	public LoadKeyStoreManagerTask(final KeyStoreManagerListener kml,
								   final FragmentActivity act,
								   final UsbDevice usbDev,
								   final UsbManager usbMgr) {
		this.kmListener = kml;
		this.activity = act;
		this.usbDevice = usbDev;
		this.usbManager = usbMgr;
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
	protected KeyStore doInBackground(Void[] params) {
		Logger.i(ES_GOB_AFIRMA, "Inicializamos el almacen"); //$NON-NLS-1$
		// Se obtiene el KeyStore
		return KeyStoreManagerFactory.initKeyStoreManager(this.activity, this.kmListener, this.usbDevice, this.usbManager);
	}

	/**
	 * Elimina el di&acute;logo de carga cuando termina la tarea en segundo plano.
	 */
	@Override
	protected void onPostExecute(KeyStore o) {
		super.onPostExecute(o);
		// Se cargan los certificados del keystore
		if (o != null) {
			new LoadCertificatesTask(o, this.kmListener, this.activity).execute();
		}
		if (getProgressDialog().isShowing()) {
			getProgressDialog().dismiss();
		}
	}

	private ProgressDialog getProgressDialog() {
		return this.progressDialog;
	}
	private void setProgressDialog(final ProgressDialog pd) {
		this.progressDialog = pd;
	}
}
