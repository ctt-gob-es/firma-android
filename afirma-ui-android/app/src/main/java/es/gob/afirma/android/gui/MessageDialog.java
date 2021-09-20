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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/** Di&aacute;logo modal con el que mostrar al usuario un mensaje y un bot&oacute;n para ocultar el
 * di&aacute;logo y, opcionalmente, realizar una acci&oacute;n. */
public class MessageDialog extends DialogFragment {

	private String message = null;

	public void setListener(final DialogInterface.OnClickListener listener) {
		this.listener = listener;
	}

	public void setDialogBuilder(final Activity activity) {
		if (activity == null) {
			this.dialogBuilder = new AlertDialog.Builder(getActivity());
		}
		else {
			this.dialogBuilder = new AlertDialog.Builder(activity);
		}
	}

	private DialogInterface.OnClickListener listener = null;

	private AlertDialog.Builder dialogBuilder = null;

	public MessageDialog() {
		//Vacio
	}

	public static MessageDialog newInstance(final String message) {
		MessageDialog md = new MessageDialog();
		Bundle args = new Bundle();
		args.putString("message", message);
		md.setArguments(args);
		return md;
	}
	/*/** Construye el di&aacute;logo.
	 * @param message Mensaje que mostrar al usuario.
	 * @param listener Manejador con la acci&oacute;n a realizar al cerrar el di&aacute;logo.
	 * @param activity Actividad de la que depende el di&aacute;logo. */
	/*MessageDialog(final String message, final DialogInterface.OnClickListener listener, final Activity activity) {
		this.message = message;
		this.listener = listener;

		if (activity == null) {
			this.dialogBuilder = new AlertDialog.Builder(getActivity());
		}
		else {
			this.dialogBuilder = new AlertDialog.Builder(activity);
		}
	}

	void setMessage(final String message) {
		this.message = message;
	}*/

	@NonNull
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.message = getArguments().getString("message");
		this.dialogBuilder.setMessage(this.message);
		if (this.dialogBuilder == null) {
			this.dialogBuilder = new AlertDialog.Builder(getActivity());
		}
		if (this.listener != null) {
			this.dialogBuilder.setPositiveButton(android.R.string.ok, this.listener);
		}
		return this.dialogBuilder.create();
	}
}
