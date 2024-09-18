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

import static es.gob.afirma.android.LoadKeyStoreFragmentActivity.REQUEST_NFC_KEYSTORE;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import es.gob.afirma.R;

/** Di&aacute;logo modal con el que mostrar al usuario un mensaje y un bot&oacute;n para ocultar el
 * di&aacute;logo y, opcionalmente, realizar una acci&oacute;n. */
public class ChooseCertTypeDialog extends FragmentActivity {

	@Override
	public void onCreate(final Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.choose_cert_type_dialog);

		TextView certificateTv = this.findViewById(R.id.cert_option);
		certificateTv.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent dataIntent = new Intent();
				setResult(RESULT_OK, dataIntent);
				finish();
			}
		});

		TextView signWithDnieTv = this.findViewById(R.id.dnie_option);
		signWithDnieTv.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent dataIntent = new Intent();
				setResult(REQUEST_NFC_KEYSTORE, dataIntent);
				finish();
			}
		});

		Button closeBtn = this.findViewById(R.id.iconCloseDialogButton);
		closeBtn.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});

	}


}
