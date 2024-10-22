/* Copyright (C) 2024 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 09/04/24
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
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.crypto.SignTask;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.RuntimePasswordNeededException;
import es.gob.afirma.signers.pades.common.BadPdfPasswordException;

/** Di&acute;logo para introducir la contrasena de un PDF protegido.
 * @author Jose Montero */

public class PDFPasswordDialog extends DialogFragment {

	private final SignTask.SignListener signListener;

	private final SignTask signTask;

	private final Throwable pdfPasswordExc;

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	public PDFPasswordDialog (SignTask signTask, SignTask.SignListener signListener, Throwable pdfPasswordExc) {
		this.signTask = signTask;
		this.signListener = signListener;
		this.pdfPasswordExc = pdfPasswordExc;
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		dialog.dismiss();
		signListener.onSignError(new AOException("Operacion cancelada por el usuario"));
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState){

		final LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
		final View view = layoutInflater.inflate(R.layout.dialog_pdf_password, null);
		final TextView tvPDFPassword = view.findViewById(R.id.tvPDFPassword);

		if (pdfPasswordExc instanceof BadPdfPasswordException) {
			tvPDFPassword.setText(R.string.dialog_pdf_bad_password);
			tvPDFPassword.setTextColor(getResources().getColor(R.color.red));
		} else {
			tvPDFPassword.setText(R.string.dialog_pdf_password);
			tvPDFPassword.setTextColor(getResources().getColor(R.color.black));
		}

		final EditText editTextPassword = view.findViewById(R.id.etPDFPassword);

		final AlertDialog alertDialog = new Builder(getActivity(), R.style.AlertDialog)
				.setView(view)
				.setTitle(R.string.dialog_pdf_password_title)
				.setCancelable(false)
				.setNegativeButton(
						getActivity().getString(R.string.cancel),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int id) {
								dialog.dismiss();
								signListener.onSignError(new AOException("Operacion cancelada por el usuario"));
							}
						}
				)
				.setPositiveButton(R.string.ok, null).setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(final DialogInterface dialog, final int keyCode, final KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_BACK) {
							dialog.dismiss();
							signListener.onSignError(new AOException("Operacion cancelada por el usuario"));
						}
						return false;
					}
				}).create();

		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(final DialogInterface dialog) {
				Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
				b.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {

						if (editTextPassword.getText() == null || "".equals(editTextPassword.getText().toString())) { //$NON-NLS-1$
							Logger.e(ES_GOB_AFIRMA, "La contrasena no puede ser vacia o nula"); //$NON-NLS-1$
							tvPDFPassword.setText(R.string.error_empty_pdf_password);
							tvPDFPassword.setTextColor(getResources().getColor(R.color.red));
							tvPDFPassword.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
						}
						else {
							dialog.dismiss();
							try {
								if (pdfPasswordExc instanceof RuntimePasswordNeededException) {
									((RuntimePasswordNeededException) pdfPasswordExc).configure(signTask.getExtraParams(), editTextPassword.getText().toString().toCharArray());
								}
								signTask.execute();
							} catch (Exception e) {
								Logger.w(ES_GOB_AFIRMA, "Error en la firma: " + e); //$NON-NLS-1$
								dialog.dismiss();
								signListener.onSignError(new AOException("Error en la firma: " + e));
							}
						}
					}
				});
			}
		});

		return alertDialog;

	}
}