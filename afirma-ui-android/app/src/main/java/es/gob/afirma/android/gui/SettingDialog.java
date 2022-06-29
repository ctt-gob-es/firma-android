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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.gob.afirma.R;
import es.gob.afirma.android.CheckConnectionsHelper;
import es.gob.afirma.android.NfcHelper;

/** Di&acute;logo para la configuraci&oacute;n de la aplicaci&oacute;n. */

public class SettingDialog extends DialogFragment {

	/** Construye un di&aacute;logo para la configuraci&oacute;n de la aplicaci&oacute;n. */
	public SettingDialog() {
		setArguments(new Bundle());
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState){

		final LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		final View view = layoutInflater.inflate(R.layout.dialog_settings, null);

		((CheckBox) view.findViewById(R.id.ckbUseNfc)).setChecked(
				NfcHelper.isNfcPreferredConnection(getContext())
		);

		((CheckBox) view.findViewById(R.id.ckbSSLConnections)).setChecked(
				CheckConnectionsHelper.isValidateSSLConnections(getContext())
		);

		((EditText) view.findViewById(R.id.editTextTrustedDomains)).setText(
				CheckConnectionsHelper.getTrustedDomains(getContext())
		);

		AlertDialog settingDialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialog)
				.setView(view)
				.setTitle(getString(R.string.settings))
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						dialog.dismiss();
					}
				})
				.setPositiveButton(android.R.string.ok, null)
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

		settingDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialogInterface) {

				Button button = ((AlertDialog) settingDialog).getButton(AlertDialog.BUTTON_POSITIVE);
				button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View settingDlgView) {
						final CheckBox ckbUseNfc = view.findViewById(R.id.ckbUseNfc);
						NfcHelper.configureNfcAsPreferredConnection(ckbUseNfc.isChecked());

						final CheckBox ckbValidateSSLConnections = view.findViewById(R.id.ckbSSLConnections);
						CheckConnectionsHelper.configureValidateSSLConnections(ckbValidateSSLConnections.isChecked());

						final EditText editTextTrustedDomains = view.findViewById(R.id.editTextTrustedDomains);
						if (checkCorrectDomainFormat(editTextTrustedDomains.getText().toString())) {
							CheckConnectionsHelper.setTrustedDomains(editTextTrustedDomains.getText().toString());
							settingDialog.dismiss();
						} else {
							AlertDialog formatErrorDlg = new AlertDialog.Builder(getContext(), R.style.AlertDialog).create();
							formatErrorDlg.setTitle(getString(R.string.error));
							formatErrorDlg.setMessage(getString(R.string.error_format_trusted_domains));
							formatErrorDlg.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok),
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											dialog.dismiss();
										}
									});
							formatErrorDlg.show();
						}
					}
				});
			}
		});
		return settingDialog;
	}

	/**
	 * Comprueba que el formato de los dominios indicados sea el correcto.
	 * @param domainsText Texto con todos los dominios.
	 * @return Devuelve true si el formato es correcto y false en caso contrario
	 */
	private static boolean checkCorrectDomainFormat(final String domainsText) {

		final String [] domainsArray = domainsText.split("\n");

		final String regex = "^((?!-)[A-Za-z0-9-*]{1,63}(?<!-)\\.)+[A-Za-z*]{1,6}"; //$NON-NLS-1$

		final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

		for (final String domain : domainsArray) {
			final Matcher matcher = pattern.matcher(domain);
			final boolean correctFormat = matcher.find();
			if (!correctFormat) {
				return false;
			}
		}

		return true;
	}
}