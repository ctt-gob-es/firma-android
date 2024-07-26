/* Copyright (C) 2024 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
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

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import es.gob.afirma.R;

/** Di&aacute;logo modal con informaci&oacute;n sobre la app */
public class HelpDialog extends BottomSheetDialog {

	public HelpDialog(final Context context) {
		super(context, R.style.BottomSheetDialogTheme);
		View layout = LayoutInflater.from(context).inflate(R.layout.help_dialog, this.findViewById(R.id.helpDialog));
		this.setContentView(layout);
		this.setCancelable(false);
		this.setCanceledOnTouchOutside(true);

		Button moreInfoButton = this.findViewById(R.id.moreInfoButton);
		moreInfoButton.setPaintFlags(moreInfoButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		moreInfoButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
					{
						hide();
					}
		});

		Button acceptButton = this.findViewById(R.id.agreeButton);

		acceptButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v) { hide(); }
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

		this.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				BottomSheetDialog d = (BottomSheetDialog) dialog;
				FrameLayout bottomSheet = d.findViewById(R.id.design_bottom_sheet);
				CoordinatorLayout coordinatorLayout = (CoordinatorLayout) bottomSheet.getParent();
				BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
				bottomSheetBehavior.setPeekHeight(bottomSheet.getHeight());
				coordinatorLayout.getParent().requestLayout();
			}
		});
	}

}
