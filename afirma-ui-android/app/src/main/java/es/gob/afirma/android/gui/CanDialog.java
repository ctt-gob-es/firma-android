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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;
import es.gob.jmulticard.android.callbacks.CachePasswordCallback;

/** Di&acute;logo para introducir el PIN.
 * Se usa en almacenes distintos al del propio sistema operativo Android.
 * @author Astrid Idoate */

public final class CanDialog extends DialogFragment {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	//public static CachePasswordCallback passwordCallback;
	private CanResult canResult;

	/** Construye un di&acute;logo para introducir el CAN. */
	public CanDialog() {
	}

	private static CanDialog instance = null;

	/** Obtiene una nueva instancia de un di&acute;logo para introducir el CAN.
	 * @param canResult Objeto en el que almacenar el valor establecido en el di&oacute;logo.
	 * @return pinDialog el di&acute;logo creado. */
	public static CanDialog newInstance(final CanResult canResult) {
		if (instance == null) {
			instance = new CanDialog();
		}
		instance.setCanResult(canResult);
		instance.setArguments(new Bundle());
		return instance;
	}

	private void setCanResult(CanResult canResult) {
		this.canResult = canResult;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState){

		final LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		final View view = layoutInflater.inflate(R.layout.dialog_can, null);

		final AlertDialog alertDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialog)
				.setView(view)
				.setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
				.setNegativeButton(
						R.string.cancel_nfc,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int id) {
								if (CanDialog.this.canResult != null) {
									CanDialog.this.canResult.setCanObtained(false);
								}
								dialog.dismiss();
								getActivity().setResult(Activity.RESULT_CANCELED);
								getActivity().finish();
							}
						}
				)
				.create();

		final TextView tvPinError = view.findViewById(R.id.tvPinError);
		final EditText editTextPin = view.findViewById(R.id.etPin);

		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

			@Override
			public void onShow(final DialogInterface dialog) {
				Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
				b.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {

						if(editTextPin.getText() == null || "".equals(editTextPin.getText().toString())) { //$NON-NLS-1$
							Logger.e(ES_GOB_AFIRMA, "El CAN no puede ser vacio o nulo"); //$NON-NLS-1$
							tvPinError.setVisibility(View.VISIBLE);
							tvPinError.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
						}
						else {
							tvPinError.setVisibility(View.INVISIBLE);
							dialog.dismiss();
							if (CanDialog.this.canResult != null) {
								CanDialog.this.canResult.setCanObtained(true);
								CanDialog.this.canResult.setPasswordCallback(new CachePasswordCallback(editTextPin.getText().toString().toCharArray()));
							}
						}
					}
				});
			}
		});
		alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(final DialogInterface dialog, final int keyCode, final KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (CanDialog.this.canResult != null) {
						CanDialog.this.canResult.setCanObtained(false);
					}
					dialog.dismiss();
					getActivity().setResult(Activity.RESULT_CANCELED);
					getActivity().finish();
					return true;
				}
				return false;
			}
		});

		return alertDialog;
	}
}