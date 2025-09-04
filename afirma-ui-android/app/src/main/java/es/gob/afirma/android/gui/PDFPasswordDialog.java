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

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.crypto.SignTask;
import es.gob.afirma.core.RuntimePasswordNeededException;
import es.gob.afirma.signers.pades.common.BadPdfPasswordException;

/** Di&acute;logo para introducir la contrasena de un PDF protegido.
 * @author Jose Montero */

public class PDFPasswordDialog extends BottomSheetDialog {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	public PDFPasswordDialog (Context context, SignTask signTask, SignTask.SignListener signListener, Throwable pdfPasswordExc) {
		super(context, R.style.BottomSheetDialogTheme);
		View view = LayoutInflater.from(context).inflate(R.layout.dialog_pdf_password, this.findViewById(R.id.pdfPasswordDialog));
		this.setContentView(view);
		this.setCancelable(false);

		if (pdfPasswordExc instanceof BadPdfPasswordException) {
			TextView errorTv = PDFPasswordDialog.this.findViewById(R.id.errorTextView);
			errorTv.setText(context.getString(R.string.dialog_pdf_bad_password));
			errorTv.setVisibility(View.VISIBLE);
		}

		final EditText editTextPassword = view.findViewById(R.id.pwdEtx);
		final ImageView eyeIcon = view.findViewById(R.id.eyeIcon);

		eyeIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (editTextPassword.getInputType() == 129 || editTextPassword.getInputType() == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) {
					editTextPassword.setTransformationMethod(null);
					editTextPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
					eyeIcon.setImageResource(R.drawable.ic_eye_on);
				} else {
					editTextPassword.setTransformationMethod(new android.text.method.PasswordTransformationMethod());
					editTextPassword.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
					eyeIcon.setImageResource(R.drawable.ic_eye_off);
				}
				editTextPassword.setSelection(editTextPassword.getText().length());
			}
		});

		Button acceptButton = this.findViewById(R.id.acceptButton);
		acceptButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				if (editTextPassword.getText() == null || "".equals(editTextPassword.getText().toString())) { //$NON-NLS-1$
					TextView errorTv = PDFPasswordDialog.this.findViewById(R.id.errorTextView);
					errorTv.setText(context.getString(R.string.error_empty_pdf_password));
					errorTv.setVisibility(View.VISIBLE);
				}
				else {
					hide();
					try {
						if (pdfPasswordExc instanceof RuntimePasswordNeededException) {
							((RuntimePasswordNeededException) pdfPasswordExc).configure(signTask.getExtraParams(), editTextPassword.getText().toString().toCharArray());
						}
						signTask.execute();
					} catch (Exception e) {
						Logger.w(ES_GOB_AFIRMA, "Error en la firma: " + e); //$NON-NLS-1$
						signListener.onSignError(e);
					}
				}
			}
		});

		Button cancelButton = this.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				hide();
				signListener.onSignError(new PendingIntent.CanceledException("Operacion cancelada por el usuario"));
			}
		});

		this.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(final DialogInterface dialog) {
				BottomSheetDialog d = (BottomSheetDialog) dialog;
				FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

				behavior.setHideable(false);
				behavior.setSkipCollapsed(true);
				behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

				behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
					@Override
					public void onStateChanged(@NonNull View bottomSheet, int newState) {
						if (newState != BottomSheetBehavior.STATE_EXPANDED) {
							behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
						}
					}

					@Override
					public void onSlide(@NonNull View bottomSheet, float slideOffset) {
					}
				});
			}
		});

	}

}