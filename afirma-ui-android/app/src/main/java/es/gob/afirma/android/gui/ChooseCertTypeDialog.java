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

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import es.gob.afirma.R;

/** Di&aacute;logo modal con el que mostrar al usuario un mensaje y un bot&oacute;n para ocultar el
 * di&aacute;logo y, opcionalmente, realizar una acci&oacute;n. */
public class ChooseCertTypeDialog extends BottomSheetDialog {

	public static final int CERT_TYPE_LOCAL = 1;
	public static final int CERT_TYPE_DNIE = 2;

	private TextView title;

	private ChooseCertTypeListener listener;

	public ChooseCertTypeDialog(final Context context, final ChooseCertTypeListener listener) {
		super(context, R.style.BottomSheetDialogTheme);
		View layout = LayoutInflater.from(context).inflate(R.layout.choose_cert_type_dialog, this.findViewById(R.id.customDialog));

		this.listener = listener;

		title = layout.findViewById(R.id.selectKeystoreTitle);

		this.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				BottomSheetDialog d = (BottomSheetDialog) dialog;

				FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
				behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
			}
		});
		this.setContentView(layout);

		TextView certificateTv = this.findViewById(R.id.cert_option);
		certificateTv.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (ChooseCertTypeDialog.this.listener != null) {
					ChooseCertTypeDialog.this.listener.certTypeChoosed(CERT_TYPE_LOCAL);
				}
				dismiss();
			}
		});

		TextView signWithDnieTv = this.findViewById(R.id.dnie_option);
		signWithDnieTv.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (ChooseCertTypeDialog.this.listener != null) {
					ChooseCertTypeDialog.this.listener.certTypeChoosed(CERT_TYPE_DNIE);
				}
				dismiss();
			}
		});

		Button closeBtn = this.findViewById(R.id.iconCloseDialogButton);
		closeBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (ChooseCertTypeDialog.this.listener != null) {
					ChooseCertTypeDialog.this.listener.certTypeChoosed(-1);
				}
				dismiss();
			}
		});
	}
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if (this.listener != null) {
			this.listener.certTypeChoosed(-1);
		}
		dismiss();
	}

	public void setModeAuthentication(boolean modeAuthentication) {
		title.setText(getContext().getString(modeAuthentication
				? R.string.choose_cert_type_auth_title
				: R.string.choose_cert_type_sign_title));
	}

	public static interface ChooseCertTypeListener {

		void certTypeChoosed(int certType);
	}
}
