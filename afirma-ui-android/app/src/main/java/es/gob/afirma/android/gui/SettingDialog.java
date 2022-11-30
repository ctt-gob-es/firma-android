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
import android.util.Log;
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

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	/** Construye un di&aacute;logo para la configuraci&oacute;n de la aplicaci&oacute;n. */
	public SettingDialog() {
		setArguments(new Bundle());
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState){

		final LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		final View view = layoutInflater.inflate(R.layout.dialog_settings, null);

		if (!NfcHelper.isNfcServiceAvailable(getContext())) {
			view.findViewById(R.id.ckbUseNfc).setEnabled(false);
		} else {
			((CheckBox) view.findViewById(R.id.ckbUseNfc)).setChecked(
					NfcHelper.isNfcPreferredConnection(getContext())
			);
		}

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
						String domains = editTextTrustedDomains.getText().toString().trim();
						try {
							if (!domains.isEmpty()) {
								checkCorrectDomainFormat(domains);
							}
							CheckConnectionsHelper.setTrustedDomains(domains);
							settingDialog.dismiss();
						}
						catch (DomainFormatException e) {
							Log.w(ES_GOB_AFIRMA, "Se han encontrado entradas no validas en el listado de dominios", e);
							AlertDialog formatErrorDlg = new AlertDialog.Builder(getContext(), R.style.AlertDialog).create();
							formatErrorDlg.setTitle(getString(R.string.error));
							formatErrorDlg.setMessage(getString(R.string.error_format_trusted_domains, e.getMessage()));
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
	 * @throws DomainFormatException Cuando se encuentra un dominio no v&aacute;lido.
	 */
	private static void checkCorrectDomainFormat(final String domainsText) throws  DomainFormatException {

		final String [] domainsArray = domainsText.split("\n");

		final String regex = "^[a-z0-9*][a-z0-9-.:]{1,61}[a-z0-9*]$";

		final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

		for (final String domain : domainsArray) {
			String domainCleaned = domain.trim();
			if (!domainCleaned.isEmpty()) {
				final Matcher matcher = pattern.matcher(domainCleaned);
				if (!matcher.matches()) {
					throw new DomainFormatException(domainCleaned);
				}
			}
		}
	}

	/**
	 * Se&ntilde;ala un error en un patr&oacute; de dominio.
	 */
	private static class DomainFormatException extends Exception {

		/**
		 * Construye la excepci&oacute;n con el patr&oacute; de dominio
		 * inv&aacute;lido.
		 * @param domain Patr&oacute;n de dominio inv&aacute;lido.
		 */
		public DomainFormatException(String domain) {
			super(domain);
		}
	}
}