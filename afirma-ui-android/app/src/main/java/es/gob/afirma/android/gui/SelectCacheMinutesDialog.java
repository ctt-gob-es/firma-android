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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;

import es.gob.afirma.R;
import es.gob.afirma.android.MinutesAdapter;
import es.gob.afirma.android.SignConfigurationActivity;

/** Di&aacute;logo modal con informaci&oacute;n sobre la app */
public class SelectCacheMinutesDialog extends BottomSheetDialog {

	private ListView minutesLV;
	private ArrayList<Integer> minutesList;
	private Context context;
	private MinutesAdapter adapter;

	public SelectCacheMinutesDialog(final Button timeoutButton, final Context context) {
		super(context, R.style.BottomSheetDialogTheme);
		View layout = LayoutInflater.from(context).inflate(R.layout.select_cache_minutes_dialog, this.findViewById(R.id.selectCacheMinutesDialog));
		this.context = context;
		this.setContentView(layout);
		this.setCancelable(false);
		this.setCanceledOnTouchOutside(true);

		Button selectButton = this.findViewById(R.id.selectButton);
		selectButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
					{
						SignConfigurationActivity.timeoutMinutes =  minutesList.get(adapter.getSelectedPosition());
						timeoutButton.setText(minutesList.get(adapter.getSelectedPosition()) + " Min");
						hide();
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


		minutesLV = this.findViewById(R.id.minutesLV);
		minutesList = new ArrayList<>();

		loadData();

	}

	private void loadData() {

		minutesList.add(5);
		minutesList.add(15);
		minutesList.add(30);
		minutesList.add(60);
		minutesList.add(120);

		adapter = new MinutesAdapter(minutesList, this.context);
		minutesLV.setAdapter(adapter);
		minutesLV.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		minutesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				adapter.setSelectedPosition(position);
			}
		});

		int minutesConfig = SignConfigurationActivity.timeoutMinutes;
		for (int i = 0 ; i < minutesList.size() ; i++) {
			if (minutesConfig == minutesList.get(i)) {
				adapter.setSelectedPosition(i);
			}
		}

	}

}
