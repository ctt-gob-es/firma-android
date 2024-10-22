/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.android.gui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.crypto.KeyStoreManagerListener;
import es.gob.afirma.android.crypto.LoadDeviceKeystoreAsyncTask;

/**
 * Di&acute;logo para introducir el PIN.
 * Se usa en almacenes distintos al del propio sistema operativo Android.
 * @author Astrid Idoate
 */
public class PinDialog extends DialogFragment {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private String provider;
	String getProviderName() {
		return this.provider;
	}

	private String keyStoreName;
	String getKeyStoreName() {
		return this.keyStoreName;
	}

	private KeyStoreManagerListener ksmListener;
	KeyStoreManagerListener getKsmListener() {
		return this.ksmListener;
	}

	/** Construye un di&acute;logo para introducir el PIN. */
	public PinDialog() {
		this.ksmListener = null;
	}

	/** Obtiene una nueva instancia de un di&acute;logo para introducir el PIN.
	 * @param provider proveedor Proveedor de seguridad para obtener el almac&eacute;n de claves.
	 * @param keyStoreName Nombre del almac&eacute;n de claves.
	 * @param ksml Clase a la que se establece el gestor de almacenes de claves y certificados.
	 * @return pinDialog el di&acute;logo creado. */
	public static PinDialog newInstance(final String provider, final String keyStoreName, final KeyStoreManagerListener ksml) {

		final PinDialog pinDialog = new PinDialog();
		pinDialog.setKeyStoreManagerListener(ksml);
		final Bundle args = new Bundle();
		args.putString("provider", provider); //$NON-NLS-1$
		args.putString("keyStoreName", keyStoreName); //$NON-NLS-1$
		pinDialog.setArguments(args);
		return pinDialog;
	}

	/** Establece la clase que manejara el resultado de la carga del almacen de claves del dispositivo.
	 * @param ksml Manejador de la carga. */
	public void setKeyStoreManagerListener(final KeyStoreManagerListener ksml) {
		this.ksmListener = ksml;
	}


	@NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState){

		if(getArguments() != null) {
			this.provider = getArguments().getString("provider"); //$NON-NLS-1$
			this.keyStoreName = getArguments().getString("keyStoreName"); //$NON-NLS-1$

			Logger.i(ES_GOB_AFIRMA, "PinDialog recibe los argumentos provider: " + this.provider + " y keyStoreName: " + this.keyStoreName);   //$NON-NLS-1$//$NON-NLS-2$
		}

		final LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		final View view = layoutInflater.inflate(R.layout.dialog_pin, null);

		final EditText editTextPin = (EditText) view.findViewById(R.id.etPin);

		final AlertDialog alertDialog = new Builder(getActivity(), R.style.AlertDialog)
				.setView(view)
				.setNegativeButton(
						getActivity().getString(R.string.cancel_nfc),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int id) {
								dialog.dismiss();
								//Cancelamos el proceso
								if (PinDialog.this.getKsmListener() != null) {
									PinDialog.this.getKsmListener().onLoadingKeyStoreSuccess(null);
								}
							}
						}
				)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {

						//TODO: El PIN no puede ser cadena vacia?
						if(editTextPin.getText() != null && !"".equals(editTextPin.getText().toString())) { //$NON-NLS-1$

							dialog.dismiss();
							try {
								if (PinDialog.this.getKsmListener() != null) {
									new LoadDeviceKeystoreAsyncTask(getActivity(),
											editTextPin.getText().toString(),
											PinDialog.this.getKeyStoreName(),
											PinDialog.this.getProviderName(),
											PinDialog.this.getKsmListener()).execute();
								}
							} catch (Exception e) {
								Logger.w(ES_GOB_AFIRMA, "Error cargando el almacen almacen de claves del dispositivo: " + e); //$NON-NLS-1$
							}
						}
						else {
							//TODO: Gestionar este caso
							Logger.e(ES_GOB_AFIRMA, "El pin no puede ser vacio o nulo"); //$NON-NLS-1$
							if (PinDialog.this.getKsmListener() != null) {
								PinDialog.this.getKsmListener().onLoadingKeyStoreError(
										getActivity().getString(R.string.error_pin_nulo), null
								);
							}
						}
					}
				}).setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(final DialogInterface dialog, final int keyCode, final KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_BACK) {
							dialog.dismiss();
							//Cancelamos el proceso
							if (PinDialog.this.getKsmListener() != null) {
								PinDialog.this.getKsmListener().onLoadingKeyStoreSuccess(null);
							}
							return true;
						}
						return false;
					}
				}).create();

		return alertDialog;

	}
}