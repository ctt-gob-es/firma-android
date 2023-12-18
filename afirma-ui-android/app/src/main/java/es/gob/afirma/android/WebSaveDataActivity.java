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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.AndroidHttpManager;
import es.gob.afirma.android.crypto.CipherDataManager;
import es.gob.afirma.android.gui.DownloadFileTask;
import es.gob.afirma.android.gui.MessageDialog;
import es.gob.afirma.android.gui.SendDataTask;
import es.gob.afirma.core.misc.MimeHelper;
import es.gob.afirma.core.misc.http.UrlHttpManagerFactory;
import es.gob.afirma.core.misc.protocol.ParameterException;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParser;
import es.gob.afirma.core.misc.protocol.UrlParametersToSave;

/** Actividad para el guardado de datos en el almacenamiento del dispositivo. */
public final class WebSaveDataActivity extends FragmentActivity
		implements DownloadFileTask.DownloadDataListener, SendDataTask.SendDataListener {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma";

	/** Juego de carateres UTF-8. */
	private static final String DEFAULT_URL_ENCODING = "UTF-8"; //$NON-NLS-1$

	private static final int REQUEST_WRITE_STORAGE = 2;

	private static final int REQUEST_CODE_SYSTEM_SAVE_FILE_BROWSER = 3;

	private UrlParametersToSave parameters = null;

	private DownloadFileTask downloadFileTask = null;

	static {
		// Instalamos el gestor de descargas que deseamos utilizar en las invocaciones por
		// protocolo a la aplicacion
		UrlHttpManagerFactory.install(new AndroidHttpManager());
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Si cargamos la actividad desde el carrusel de aplicaciones, redirigimos a la
		// pantalla principal
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
				== Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(this, HomeActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return;
		}

		// Si no estamos creando ahora la pantalla (por se una rotacion)
		if (savedInstanceState != null){
			Logger.i(ES_GOB_AFIRMA, "Se esta relanzando la actividad. Se omite volver a iniciar el proceso de firma");
			return;
		}

		boolean writePermissionGranted;
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			writePermissionGranted = true;
		}
		else {
			writePermissionGranted = (
					ContextCompat.checkSelfPermission(
							this,
							Manifest.permission.WRITE_EXTERNAL_STORAGE
					) == PackageManager.PERMISSION_GRANTED
			);
		}

		if (!writePermissionGranted) {
			requestStoragePerm();
			return;
		}

		executeSaveData();
	}

	void executeSaveData() {

		// Comprobamos si se indico un nombre de fichero en la peticion
		try {
			this.parameters = ProtocolInvocationUriParser.getParametersToSave(getIntent().getDataString());
		}
		catch (final ParameterException e) {
			Logger.e(ES_GOB_AFIRMA, "Error en los parametros de entrada: " + e); //$NON-NLS-1$
			finish();
			return;
		}

		// Si no tenemos datos, los podamos descargar y no hemos empezado todavia la descarga, empezamos a descargarlos
		if (this.parameters.getData() == null && this.parameters.getFileId() != null && this.downloadFileTask == null) {
			this.downloadFileTask = new DownloadFileTask(
					this.parameters.getFileId(),
					this.parameters.getRetrieveServletUrl(),
					this);
			this.downloadFileTask.execute();
			return;
		}

		// Abrimos la pantalla de seleccion del fichero de guardado
		openFileChooserActivity();
	}

	void openFileChooserActivity() {

		// El nombre por defecto del fichero sera el de la peticion o, si no se indico, uno
		// preestablecido
		String filename = getFilename();

		// Configuramos la ventana de guardado del sistema
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*"); //$NON-NLS-1$
		// Para esta actividad EXTRA_TITLE es el nombre del fichero por defecto
		intent.putExtra(Intent.EXTRA_TITLE, filename);
		startActivityForResult(intent, REQUEST_CODE_SYSTEM_SAVE_FILE_BROWSER);
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
		if (requestCode == REQUEST_WRITE_STORAGE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Logger.i("es.gob.afirma", "Concedido permiso de escritura en disco");
				executeSaveData();
			}
			else {
				showErrorMessage(getString(R.string.error_no_storage_permissions));
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
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_SYSTEM_SAVE_FILE_BROWSER) {
			if (resultCode == RESULT_OK && data != null) {
				try {
					OutputStream outputStream = getContentResolver().openOutputStream(data.getData());
					if (outputStream == null) {
						throw new IOException("No pudo localizarse el fichero a traves del proveedor");
					}
					outputStream.write(this.parameters.getData());
					outputStream.close();

				} catch (final IOException e) {
					showErrorMessage(getString(R.string.error_saving_data));
					sendError(ErrorManager.ERROR_SAVING_DATA);
					Logger.e(ES_GOB_AFIRMA, "Error al guardar los datos", e); //$NON-NLS-1$
					return;
				}
				sendData("OK", true);
				return;
			}
		}

		// Si no se mando a guardar, se aborta la operacion
		sendError(ErrorManager.ERROR_CANCELLED_OPERATION);
	}

	private String getFilename() {
		String filename = this.parameters.getFileName();
		if (filename == null) {
			filename = getString(R.string.default_saved_filename);
			final String ext = getExtension(this.parameters.getData());
			if (ext != null) {
				filename += filename + "." + ext; //$NON-NLS-1$
			}
		}
		return filename;
	}

	/**
	 * Recupera la extensi&oacute;n preferente para un fichero con los datos indicados.
	 * @param data Datos para los que se busca una extensi&oacute;n de fichero.
	 * @return Extensi&oacute;n (por ejemplo, "jpg") o {@code null} si no se identific&oacute; una.
	 */
	private static String getExtension(final byte[] data) {
		String ext;
		try {
			ext = new MimeHelper(data).getExtension();
		}
		catch( Exception e) {
			Logger.w(ES_GOB_AFIRMA, "No se pudo identificar la extension del fichero", e);
			ext = null;
		}
		return ext;
	}

	private void sendError(String errorId) {

		String errorData = ErrorManager.genError(errorId);
		String msgEncoded;
		try {
			msgEncoded = URLEncoder.encode(errorData, DEFAULT_URL_ENCODING);
		}
		catch (UnsupportedEncodingException e) {
			msgEncoded = URLEncoder.encode(errorData);
		}
		sendData(msgEncoded, false);
	}

	@Override
	public void onDownloadingDataSuccess(byte[] data) {

		Logger.i(ES_GOB_AFIRMA, "Se ha descargado correctamente la configuracion de guardado de datos almacenada en servidor"); //$NON-NLS-1$

		// Si hemos tenido que descargar los datos desde el servidor, los desciframos,
		// actualizamos los datos que teniamos y  continuamos con la operacion
		final byte[] decipheredData;
		try {
			decipheredData = CipherDataManager.decipherData(data, this.parameters.getDesKey());
		}
		catch (final IOException e) {
			Logger.e(ES_GOB_AFIRMA, "Los datos proporcionados no est&aacute;n correctamente codificados en base 64", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			sendError(ErrorManager.ERROR_BAD_PARAMETERS);
			return;
		}
		catch (final GeneralSecurityException e) {
			Logger.e(ES_GOB_AFIRMA, "Error al descifrar los datos recuperados del servidor para la firma", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			sendError(ErrorManager.ERROR_BAD_PARAMETERS);
			return;
		}
		catch (final IllegalArgumentException e) {
			Logger.e(ES_GOB_AFIRMA, "Los datos recuperados no son un base64 valido", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			sendError(ErrorManager.ERROR_BAD_PARAMETERS);
			return;
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA, "Error desconocido durante el descifrado de los datos", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			sendError(ErrorManager.ERROR_BAD_PARAMETERS);
			return;
		}

		Logger.i(ES_GOB_AFIRMA, "Se han descifrado los datos y se inicia su analisis:\n" + new String(decipheredData)); //$NON-NLS-1$

		try {
			this.parameters = ProtocolInvocationUriParser.getParametersToSave(decipheredData);
		}
		catch (final ParameterException e) {
			Logger.e(ES_GOB_AFIRMA, "Error en los parametros XML de configuracion de guardado de datos: " + e, e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			sendError(ErrorManager.ERROR_BAD_PARAMETERS);
			return;
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA, "Error desconocido al analizar los datos descargados desde el servidor", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			sendError(ErrorManager.ERROR_BAD_PARAMETERS);
			return;
		}

		// Iniciamos el guardado
		openFileChooserActivity();
	}

	@Override
	public void onDownloadingDataError(String msg, Throwable t) {
		Logger.e(ES_GOB_AFIRMA, "Ocurrio un error descargando los datos del servidor intermedio: " + msg, t); //$NON-NLS-1$
		showErrorMessage(getString(R.string.error_server_connect));
		sendError(ErrorManager.ERROR_COMMUNICATING_WITH_WEB);
	}

	/** Env&iacute;a los datos indicado a un servlet. En caso de error, cierra la aplicaci&oacute;n.
	 * @param data Datos que se desean enviar.
	 * @param critical Indica si despu&eacute;s del intento de env&iacute;o del mensaje (da igual si
	 *                 llega o no) se debe cerrar la aplicaci&oacute;n. */
	private void sendData(final String data, final boolean critical) {
		Logger.i(ES_GOB_AFIRMA, "Se almacena el resultado en el servidor con el Id: " + this.parameters.getId()); //$NON-NLS-1$

		try {
			new SendDataTask(
					this.parameters.getId(),
					this.parameters.getStorageServletUrl(),
					data,
					this,
					critical
			).execute();
		}
		catch (Throwable e) {
			onSendingDataError(e, true);
		}
	}

	@Override
	public void onSendingDataSuccess(byte[] result, boolean critical) {
		Logger.i(ES_GOB_AFIRMA, "Se ejecuta la funcion de exito en el guardado de datos"); //$NON-NLS-1$
		finishAffinity();
	}

	@Override
	public void onSendingDataError(Throwable error, boolean critical) {
		Logger.e(ES_GOB_AFIRMA, "Se ejecuta la funcion de error en el guardado de datos", error); //$NON-NLS-1$

		if (critical) {
			showErrorMessage(getString(R.string.error_sending_data));
			return;
		}

		finishAffinity();
	}
}