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

import es.gob.afirma.R;
import es.gob.afirma.android.NfcHelper;

/** Di&acute;logo para avisar al usuario de que su dispositivo dispone de NFC y preguntarle si
 * desea habilitar el uso del DNIe 3.0 por NFC.
 */
public class ConfigNfcDialog extends DialogFragment {

	/** Construye un di&acute;logo para preguntar si se desea habilitar el uso de NFC. */
	public ConfigNfcDialog() {
		setArguments(new Bundle());
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState){

		return new AlertDialog.Builder(getActivity())
				.setMessage(R.string.enable_nfc_question)
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						dialog.dismiss();
					}
				})
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						NfcHelper.configureNfcAsPreferredConnection(getContext(), true);
						dialog.dismiss();
					}
				})
				.create();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {

		// Cuando se cierre el dialogo dejara de estar configurado que era la primera ejecucion
		AppConfig.setFirstExecution(getActivity(), false);

		super.onDismiss(dialog);
	}
}