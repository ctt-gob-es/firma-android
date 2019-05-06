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
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import es.gob.afirma.R;

/** Asocia una lista de objetos de la clase <code>Option</code> a un layout donde mostrarlos de forma iterativa
 * @author Alberto Martinez */
public final class FileArrayAdapter extends ArrayAdapter<FileOption> {

	private static final String PARENT_DIRECTORY_NAME = ".."; //$NON-NLS-1$

	private final Context c;
	private final int id;
	private final List<FileOption> items;

	/** Construye un objeto <code>FileArrayAdapter</code> con una lista de objetos de tipo <code>Option</code> y lo asocia a un contexto definido por
	 * el usuario y a un recurso donde se mostrar&aacute;n
	 * @param context Contexto de la aplicaci&oacute;n donde se mostrar&aacute; la lista
	 * @param textViewResourceId Recurso en el que se pintara la lista
	 * @param objects Lista de objetos a pintar */
	public FileArrayAdapter(final Context context, final int textViewResourceId, final List<FileOption> objects) {
		super(context, textViewResourceId, objects);
		this.c = context;
		this.id = textViewResourceId;
		this.items = objects;
	}

	@Override
	public FileOption getItem(final int i) {
		return this.items.get(i);
	}

	@NonNull
	@Override
	public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			final LayoutInflater vi = (LayoutInflater) this.c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(this.id, null);
		}
		final FileOption o = this.items.get(position);
		if (o != null) {
			final ImageView icon = v.findViewById(R.id.fileIcon);
			final TextView t1 = (TextView) v.findViewById(R.id.TextView01);
			final TextView t2 = (TextView) v.findViewById(R.id.TextView02);


			if (t1 != null) {
				t1.setText(o.getName());
			}
			if (t2 != null) {
				if (o.isDirectory()) {
					if (PARENT_DIRECTORY_NAME.equals(o.getName())) {
						t2.setText(R.string.file_chooser_directorio_padre);
						icon.setImageDrawable(this.c.getResources().getDrawable(R.drawable.icon_folder_back));
					}
					else {
						t2.setText(R.string.file_chooser_directorio);
						icon.setImageDrawable(this.c.getResources().getDrawable(R.drawable.icon_folder));
					}
				} else {
					t2.setText(this.c.getString(R.string.file_chooser_tamano_del_fichero) + " " + formatFileSize(o.getSize())); //$NON-NLS-1$
					icon.setImageDrawable(this.c.getResources().getDrawable(R.drawable.icon_file));
				}
			}
		}

		return v;
	}

	private String formatFileSize(final long size) {

		if (size < 1024) {
			return addDotMiles(size) + " " + this.c.getString(R.string.bytes);  //$NON-NLS-1$
		}
		else if (size/1024 < 1024) {
			return addDotMiles(size/1024) + " " + this.c.getString(R.string.kilobytes);  //$NON-NLS-1$
		}
		else {
			final long kbs = size/1024;
			String fraction = Long.toString(kbs % 1024);
			if (fraction.length() > 2) {
				fraction = fraction.substring(0, 2);
			}
			return addDotMiles(kbs/1024) + "," + fraction + " " + this.c.getString(R.string.megabytes);  //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static String addDotMiles(final long number) {

		final String nString = Long.toString(number);
		final StringBuilder buffer = new StringBuilder();
		if (nString.length() > 3) {
			int dotPos = nString.length() % 3;
			if (dotPos > 0) {
				buffer.append(nString.substring(0, dotPos));
			}
			while (dotPos < nString.length()) {
				if (dotPos > 0) {
					buffer.append('.');
				}
				buffer.append(nString.substring(dotPos, dotPos + 3));
				dotPos += 3;
			}

		} else {
			buffer.append(nString);
		}
		return buffer.toString();
	}
}