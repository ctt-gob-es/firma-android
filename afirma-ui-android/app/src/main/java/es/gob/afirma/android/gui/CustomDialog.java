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
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import es.gob.afirma.R;

/** Di&aacute;logo modal con el que mostrar un mensaje personalizado y dar a sus botones
 * funciones personalizadas. */
public class CustomDialog extends BottomSheetDialog {

	public CustomDialog(final Context context, final Integer iconID, String title, String message, final String acceptButtonText) {
		this(context, iconID, title, message, acceptButtonText,false, null);
	}

	public CustomDialog(final Context context, final Integer iconID, String title, String message, final String acceptButtonText,
						final boolean includeCancelButton, final String cancelButtonText) {
		super(context, R.style.BottomSheetDialogTheme);
		View layout = LayoutInflater.from(context).inflate(R.layout.custom_dialog, this.findViewById(R.id.customDialog));
		this.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				BottomSheetDialog d = (BottomSheetDialog) dialog;

				FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
				BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
			}
		});
		this.setContentView(layout);
		this.setCancelable(false);
		if (iconID != null) {
			ImageView iconImage = this.findViewById(R.id.iconImg);
			iconImage.setBackgroundResource(iconID);
		}
		TextView titleTv = this.findViewById(R.id.title);
		titleTv.setText(title);
		TextView messageTv = this.findViewById(R.id.message);
		messageTv.setText(message);
		if (includeCancelButton) {
			Button cancelButton = this.findViewById(R.id.cancelButton);
			if (cancelButtonText != null) {
				cancelButton.setPaintFlags(cancelButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
				cancelButton.setText(cancelButtonText);
			}
			cancelButton.setVisibility(View.VISIBLE);

			cancelButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
					{
						hide();
					}
			});
		}
		Button acceptButton = this.findViewById(R.id.buttonSign);
		if (acceptButtonText != null) {
			acceptButton.setText(acceptButtonText);
		}

		acceptButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
				{
					hide();
				}
		});
	}

	public void setAcceptButtonClickListener(final View.OnClickListener onClickAcceptButton) {
		Button acceptButton = this.findViewById(R.id.buttonSign);
		acceptButton.setOnClickListener(onClickAcceptButton);
	}

	public void setCancelButtonClickListener(final View.OnClickListener onClickCancelButton) {
		Button cancelButton = this.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(onClickCancelButton);
	}

}
