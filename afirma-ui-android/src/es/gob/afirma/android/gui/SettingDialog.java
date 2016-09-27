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
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import es.gob.afirma.R;
import es.gob.afirma.android.NfcHelper;

/** Di&acute;logo para la configuraci&oacute;n de la aplicaci&oacute;n. */

public class SettingDialog extends DialogFragment {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	/** Construye un di&acute;logo para la configuraci&oacute;n de la aplicaci&oacute;n. */
	public SettingDialog() {
		setArguments(new Bundle());
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState){

		final LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		final View view = layoutInflater.inflate(R.layout.dialog_settings, null);

		((CheckBox) view.findViewById(R.id.ckbUseNfc)).setChecked(
				NfcHelper.isNfcPreferredConnection(getContext())
		);

		return new AlertDialog.Builder(getActivity())
				.setView(view)
				.setTitle(R.string.settings)
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						dialog.dismiss();
					}
				})
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {

						final CheckBox ckbUseNfc = (CheckBox) view.findViewById(R.id.ckbUseNfc);
						NfcHelper.configureNfcAsPreferredConnection(getContext(), ckbUseNfc.isChecked());
						dialog.dismiss();
					}
				})
				.setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(final DialogInterface dialog, final int keyCode, final KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_BACK) {
							dialog.dismiss();
							return true;
						}
						return false;
					}
				})
				.create();
	}
}