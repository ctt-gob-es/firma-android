/* Copyright (C) 2024 [Gobierno de Espana]
 * This file is part of "AutoFirma App".
 * "AutoFirma App" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 15/07/24
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.android.gui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import es.gob.afirma.R;

public class CompatibleDniDialog extends BottomSheetDialog {

	public CompatibleDniDialog(final Context context) {
		super(context, R.style.BottomSheetDialogTheme);
		View layout = LayoutInflater.from(context).inflate(R.layout.compatible_dni_dialog, this.findViewById(R.id.compatibleDniDialog));

		this.setContentView(layout);
		this.setCancelable(false);

		TextView messageTv = this.findViewById(R.id.message);
		messageTv.setText(Html.fromHtml(context.getString(R.string.is_my_dnie_suitable_desc)));

		Button portalButton = this.findViewById(R.id.portalButton);
		portalButton.setText(Html.fromHtml(context.getString(R.string.dni_portal_text)));
		portalButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(context.getString(R.string.dni_portal_url)));
				context.startActivity(i);
			}
		});

		Button acceptButton = this.findViewById(R.id.acceptButton);
		acceptButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
				{
					hide();
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
