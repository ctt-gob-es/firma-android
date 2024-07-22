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

import static android.app.PendingIntent.getActivity;
import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import es.gob.afirma.R;
import es.gob.afirma.android.IntroSignDnieActivity;
import es.gob.afirma.android.LocalSignResultActivity;

/** Di&aacute;logo modal con el que mostrar al usuario un mensaje y un bot&oacute;n para ocultar el
 * di&aacute;logo y, opcionalmente, realizar una acci&oacute;n. */
public class ChooseCertTypeDialog extends BottomSheetDialog {

	public final static int SELECT_CERT_REQUEST_CODE = 1;

	public ChooseCertTypeDialog(final Context context) {
		super(context, R.style.BottomSheetDialogTheme);
		View layout = LayoutInflater.from(context).inflate(R.layout.choose_cert_type_dialog, this.findViewById(R.id.chooseCertTypeDialog));
		this.setContentView(layout);
		this.setCancelable(false);
		this.setCanceledOnTouchOutside(true);

		TextView certificateTv = this.findViewById(R.id.cert_option);
		certificateTv.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
				{
					hide();
					Intent intent = new Intent(getContext(), LocalSignResultActivity.class);
					v.getContext().startActivity(intent);
				}
		});

		TextView signWithDnieTv = this.findViewById(R.id.dnie_option);
		signWithDnieTv.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				hide();
				Intent intent = new Intent(getContext(), IntroSignDnieActivity.class);
				getContext().startActivity(intent);
			}
		});

		Button closeBtn = this.findViewById(R.id.iconCloseDialogButton);
		closeBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				hide();
			}
		});
	}


}
