/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.android;


import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.AndroidHttpManager;
import es.gob.afirma.android.gui.MessageDialog;
import es.gob.afirma.core.misc.http.UrlHttpManagerFactory;
import es.gob.afirma.core.misc.protocol.ParameterException;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParser;
import es.gob.afirma.core.misc.protocol.UrlParametersToSave;

/** Actividad para el guardado de datos en el almacenamiento del dispositivo. */
public final class WebSaveDataActivity extends FragmentActivity {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma";

	private static final int REQUEST_CODE_SAVE_FILE = 1;

	private static final int REQUEST_WRITE_STORAGE = 2;

	static {
		// Instalamos el gestor de descargas que deseamos utilizar en las invocaciones por
		// protocolo a la aplicacion
		UrlHttpManagerFactory.install(new AndroidHttpManager());
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
				== Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(this, HomeActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return;
		}
		else {
			boolean writePermissionGranted = (
					ContextCompat.checkSelfPermission(
							this,
							Manifest.permission.WRITE_EXTERNAL_STORAGE
					) == PackageManager.PERMISSION_GRANTED
			);

			if (writePermissionGranted) {
				openSaveDataActivity();
			} else {
				requestStoragePerm();
			}
		}
	}

	void openSaveDataActivity() {

		Intent intent;

		// Para Androd 11 y superiores usaremos el diálogo de guardado del sistema
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

			// Comprobamos si se indico un nombre de fichero en la peticion
			UrlParametersToSave parameters;
			try {
				parameters = ProtocolInvocationUriParser.getParametersToSave(getIntent().getDataString());
			}
			catch (final ParameterException e) {
				Logger.e(ES_GOB_AFIRMA, "Error en los parametros de entrada: " + e); //$NON-NLS-1$
				finish();
				return;
			}

			// El nombre por defecto del fichero sera el de la peticion o, si no se indico, uno
			// preestablecido
			String filename;
			if (parameters.getFileName() != null && !parameters.getFileName().trim().isEmpty()) {
				filename = parameters.getFileName();
			} else {
				filename = getString(R.string.default_saved_filename);
			}

			// Configuramos la ventana de guardado del sistema
			intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("*/*"); //$NON-NLS-1$
			intent.putExtra(Intent.EXTRA_TITLE, filename);
		}
		// Para Android 10 y anteriores mostraremos un selector de fichero
		else {
			intent = getIntent();
			if (intent == null) {
				finish();
				return;
			}
			intent.setClass(this, SaveDataActivity.class);
		}

		startActivityForResult(intent, REQUEST_CODE_SAVE_FILE);
	}

	private void requestStoragePerm() {
		ActivityCompat.requestPermissions(
				this,
				new String[]{
						Manifest.permission.WRITE_EXTERNAL_STORAGE
				},
				REQUEST_WRITE_STORAGE
		);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case REQUEST_WRITE_STORAGE: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Logger.i("es.gob.afirma", "Concedido permiso de escritura en disco");
					openSaveDataActivity();
				}
				else {
					showErrorMessage(getString(R.string.error_no_storage_permissions));
				}
			}
		}
	}

	/**
	 * Muestra un mensaje de advertencia al usuario.
	 * @param message Mensaje que se desea mostrar.
	 */
	private void showErrorMessage(final String message) {
		final MessageDialog md = MessageDialog.newInstance(message);
		md.setListener(new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finishAffinity();
			}
		});
		md.setDialogBuilder(this);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					md.show(getSupportFragmentManager(), "ErrorDialog"); //$NON-NLS-1$;
				}
				catch (final Exception e) {
					// Si falla el mostrar el error (posiblemente por no disponer de un contexto grafico para mostrarlo)
					// se mostrara en un
					Toast.makeText(WebSaveDataActivity.this, message, Toast.LENGTH_LONG).show();
				}
			}
		});
		//md.show(getSupportFragmentManager(), "ErrorDialog"); //$NON-NLS-1$;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		finish();
	}
}