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


import android.app.ListActivity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.AndroidHttpManager;
import es.gob.afirma.android.crypto.CipherDataManager;
import es.gob.afirma.android.gui.DownloadFileTask;
import es.gob.afirma.android.gui.DownloadFileTask.DownloadDataListener;
import es.gob.afirma.android.gui.FileArrayAdapter;
import es.gob.afirma.android.gui.FileOption;
import es.gob.afirma.android.gui.SendDataTask;
import es.gob.afirma.core.misc.MimeHelper;
import es.gob.afirma.core.misc.http.UrlHttpManagerFactory;
import es.gob.afirma.core.misc.protocol.ParameterException;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParser;
import es.gob.afirma.core.misc.protocol.UrlParametersToSave;

/** Actividad  para el guardado de un fichero en el almacenamiento del dispositivo en Android 10 o inferior. */
public final class SaveDataActivity extends ListActivity implements DownloadDataListener, SendDataTask.SendDataListener {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private static final String SAVE_INSTANCE_KEY_CURRENT_DIR = "currentDir"; //$NON-NLS-1$
	private static final String SAVE_INSTANCE_KEY_INITIAL_DIR = "initialDir"; //$NON-NLS-1$
	private static final String SAVE_INSTANCE_KEY_SELECTED_DIR = "selectedDir"; //$NON-NLS-1$

	private static final int REQUEST_CODE_SAVE_FILE = 1; //$NON-NLS-1$

	private static final String RESULT_OK = "OK"; //$NON-NLS-1$

	private static final String DEFAULT_FILENAME = "firma"; //$NON-NLS-1$

	private File currentDir;
	File getCurrentDir() {
		return this.currentDir;
	}
	private File selectedDir;
	void setSelectedDir(final File sd) {
		this.selectedDir = sd;
	}
	File getSelectedDir() {
		return this.selectedDir;
	}

	private String initialDirectoryName = null;

	private UrlParametersToSave parameters = null;
	UrlParametersToSave getParameters() {
		return this.parameters;
	}

	private DownloadFileTask downloadFileTask = null;
	DownloadFileTask getDownloadFileTask() {
		return this.downloadFileTask;
	}

	/** Di&aacute;logo de espera durante la carga de los datos. */
	ProgressDialog progressDialog = null;

	static {
		// Instalamos el gestor de descargas que deseamos utilizar en las invocaciones por
		// protocolo a la aplicacion
		UrlHttpManagerFactory.install(new AndroidHttpManager());
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent() == null || getIntent().getData() == null) {
			Logger.w(ES_GOB_AFIRMA, "No se han indicado parametros de entrada para la actividad"); //$NON-NLS-1$
			closeActivity();
			return;
		}

		// Recogemos los datos de la URI
		if (this.parameters == null) {
			try {
				this.parameters = ProtocolInvocationUriParser.getParametersToSave(getIntent().getDataString());
			}
			catch (final ParameterException e) {
				showMessage(getString(R.string.error_bad_params));
				Logger.e(ES_GOB_AFIRMA, "Error en los parametros de entrada: " + e); //$NON-NLS-1$
				closeActivity();
				return;
			}
		}

		// Si ya estaban configurado el estado de la activity, lo recargamos
		if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_INSTANCE_KEY_INITIAL_DIR)) {
			this.initialDirectoryName = savedInstanceState.getString(SAVE_INSTANCE_KEY_INITIAL_DIR);
			if (savedInstanceState.containsKey(SAVE_INSTANCE_KEY_CURRENT_DIR)) {
				this.currentDir = new File(savedInstanceState.getString(SAVE_INSTANCE_KEY_CURRENT_DIR));
			}
			this.selectedDir = new File(savedInstanceState.getString(SAVE_INSTANCE_KEY_SELECTED_DIR));
		}

		if (this.currentDir == null) {
			if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				this.currentDir = Environment.getExternalStorageDirectory();
			} else {
				this.currentDir = Environment.getDownloadCacheDirectory();
			}
			this.initialDirectoryName = this.currentDir.getName();
			this.selectedDir = null;
		}

		Logger.d(ES_GOB_AFIRMA, "Se abre el directorio: " + this.currentDir.getAbsolutePath());  //$NON-NLS-1$

		// Establecemos el layout con la interfaz
		setContentView(R.layout.activity_save_data);

		fill(this.currentDir);
	}

	private void fill(final File f) {

		TextView currentDirectory = findViewById(R.id.current_directory);
		currentDirectory.setText(getString(R.string.file_chooser_directorio_actual, f.getName()));  //$NON-NLS-1$
		ViewCompat.setAccessibilityHeading(currentDirectory, true);


		final List<FileOption> dir = new ArrayList<FileOption>();
		for (final File ff : f.listFiles()) {
			// No mostramos ficheros ni directorios ocultos
			if (ff.getName().startsWith(".")) { //$NON-NLS-1$
				continue;
			}
			if (ff.isDirectory()) {
				dir.add(new FileOption(ff));
			}
		}

		Collections.sort(dir);
		if (!f.getName().equalsIgnoreCase(this.initialDirectoryName)) {
			dir.add(0, new FileOption(f, true));
		}

		final FileArrayAdapter adapter = new FileArrayAdapter(
				SaveDataActivity.this,
				R.layout.array_adapter_file_chooser,
				dir);
		setListAdapter(adapter);
	}

	/**
	 * Muestra un mensaje al usuario.
	 * @param message Mensaje que se desea mostrar.
	 */
	private void showMessage(final String message) {

		//TODO: Comprobar que se muestran los mensajes de error antes de cerrar la aplicacion

		runOnUiThread(
			new Runnable() {
				@Override
				public void run() {
					Toast.makeText(SaveDataActivity.this, message, Toast.LENGTH_LONG).show();
				}
			}
		);
	}

	@Override
	protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
		super.onListItemClick(l, v, position, id);

		final FileOption item = (FileOption) l.getItemAtPosition(position);
		if (item.isDirectory()) {
			this.currentDir = new File(item.getPath());
			fill(this.currentDir);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		Button saveButton = findViewById(R.id.saveButton);
		saveButton.setOnClickListener(new SaveDataAction(this));
		saveButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					saveButton.setBackgroundColor(Color.parseColor("#000000"));
				} else {
					saveButton.setBackgroundColor(Color.parseColor("#981c1c"));
				}
			}
		});

		Button cancelButton = findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				// Devolvemos a la aplicacion cliente una respuesta de cancelacion
				Logger.i(ES_GOB_AFIRMA, "Se cancela el guardado de los datos");  //$NON-NLS-1$
				sendData(ErrorManager.genError(ErrorManager.ERROR_CANCELLED_OPERATION), true);
			}
		});
		cancelButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					cancelButton.setBackgroundColor(Color.parseColor("#000000"));
				} else {
					cancelButton.setBackgroundColor(Color.parseColor("#981c1c"));
				}
			}
		});

		if (this.parameters != null) {
			// Si no tenemos datos, los podamos descargar y no hemos empezado todavia la descarga, empezamos a descargarlos
			if (this.parameters.getData() == null && this.parameters.getFileId() != null && this.downloadFileTask == null) {
				this.downloadFileTask = new DownloadFileTask(
						this.parameters.getFileId(),
						this.parameters.getRetrieveServletUrl(),
						this);
				this.downloadFileTask.execute();
			}

			// Establecemos el titulo de la ventana si se definio
			if (this.parameters.getTitle() != null) {
				setTitle(this.parameters.getTitle());
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (this.downloadFileTask != null) {
			this.downloadFileTask.cancel(true);
		}
	}

	/**
	 * Construye un nombre apropiado para un fichero de firma en base a un nombre base
	 * y un &iacute;ndice.
	 * @param originalName Nombre base del fichero.
	 * @param index &Iacute;ndice.
	 * @return Nombre apropiado para el fichero de firma.
	 */
	private static String buildName(final String originalName, final int index) {

		String indexSuffix = ""; //$NON-NLS-1$
		if (index > 0) {
			indexSuffix = "(" + index + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		final int dotPos = originalName.lastIndexOf('.');
		if (dotPos == -1) {
			return originalName + indexSuffix;
		}
		return originalName.substring(0, dotPos) + indexSuffix + originalName.substring(dotPos);
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

	void saveData(final byte[] data, final File dir, final String configuredFilename) {
		String filename = configuredFilename;
		if (filename == null) {
			final String ext = getExtension(data);
			if (ext != null) {
				filename = DEFAULT_FILENAME + "." + ext; //$NON-NLS-1$
			}
			else {
				filename = DEFAULT_FILENAME;
			}
		}
		int i = 0;
		File outFile = new File(dir, filename);
		while (outFile.exists()) {
			outFile = new File(dir, buildName(filename, ++i));
		}

		try {
			final FileOutputStream fos = new FileOutputStream(outFile);
			fos.write(data);
			fos.close();
		}
		catch (final IOException e) {
			Logger.e(ES_GOB_AFIRMA, "No se han podido guardar los datos: " + e); //$NON-NLS-1$
			showMessage(getString(R.string.error_saving_data));
			sendData(ErrorManager.genError(ErrorManager.ERROR_SAVING_DATA), false);
			return;
		}
		// Mostramos el mensaje de confirmacion del guardado
		showMessage(getString(R.string.data_saved, outFile.getName()));

		Logger.d(ES_GOB_AFIRMA, "Los datos se han guardado correctamente"); //$NON-NLS-1$

		// Refrescamos el directorio para permitir acceder al fichero
		try {
			MediaScannerConnection.scanFile(
				this,
				new String[] { outFile.toString(),
					outFile.getParentFile().toString()
				},
				null,
				null
			);
		}
		catch(final Exception e) {
			Logger.w(ES_GOB_AFIRMA, "Error refrescando el MediaScanner: " + e); //$NON-NLS-1$
		}

		// Notificamos el resultado a la aplicacion que llamo a la actividad
		sendData(RESULT_OK, true);
	}

	@Override
	public void onDownloadingDataSuccess(final byte[] data) {

		Logger.i(ES_GOB_AFIRMA, "Datos descargados correctamente"); //$NON-NLS-1$

		// Si hemos tenido que descargar los datos desde el servidor, los desciframos y llamamos
		// al dialogo de seleccion de certificados para la firma
		byte[] decipheredData;
		try {
			decipheredData = CipherDataManager.decipherData(data, this.parameters.getDesKey());
		}
		catch (final IOException e) {
			Logger.e(ES_GOB_AFIRMA, "Los datos proporcionados no estan correctamente codificados en Base64: " + e); //$NON-NLS-1$
			showMessage(getString(R.string.error_bad_params));
			closeActivity();
			return;
		}
		catch (final GeneralSecurityException e) {
			Logger.e(ES_GOB_AFIRMA, "Error al descifrar los datos recuperados del servidor para la firma: " + e); //$NON-NLS-1$
			showMessage(getString(R.string.error_bad_params));
			closeActivity();
			return;
		}
		catch (final IllegalArgumentException e) {
			Logger.e(ES_GOB_AFIRMA, "Los datos recuperados no son un base64 valido: " + e); //$NON-NLS-1$
			showMessage(getString(R.string.error_bad_params));
			closeActivity();
			return;
		}

		try {
			this.parameters = ProtocolInvocationUriParser.getParametersToSave(decipheredData);
		}
		catch (final ParameterException e) {
			showMessage(getString(R.string.error_bad_params));
			Logger.e(ES_GOB_AFIRMA, "Error en los parametros XML de configuracion de guardado: " + e); //$NON-NLS-1$
			closeActivity();
			return;
		}

		// Establecemos el titulo de la ventana si se definio
		if (this.parameters.getTitle() != null) {
			setTitle(this.parameters.getTitle());
		}

		// Si ya se selecciono el directorio en el que guardar los datos, lo hacemos directamente
		if (this.selectedDir != null) {
			saveData(this.parameters.getData(), this.selectedDir, this.parameters.getFileName());
		}
	}

	@Override
	public void onDownloadingDataError(final String msg, final Throwable t) {
		Logger.e(ES_GOB_AFIRMA, "Ocurrio un error descargando los datos del servidor intermedio: " + msg); //$NON-NLS-1$
		if (t != null) {
			t.printStackTrace();
		}
		showMessage(getString(R.string.error_saving_data));
		closeActivity();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(SAVE_INSTANCE_KEY_CURRENT_DIR, this.currentDir.getAbsolutePath());
		outState.putString(SAVE_INSTANCE_KEY_INITIAL_DIR, this.initialDirectoryName);
		if (this.selectedDir != null) {
			outState.putString(SAVE_INSTANCE_KEY_SELECTED_DIR, this.selectedDir.getAbsolutePath());
		}
	}

	private class SaveDataAction implements OnClickListener {

		final SaveDataActivity saveDataActivity;

		SaveDataAction(final SaveDataActivity saveDataActivity) {
			this.saveDataActivity = saveDataActivity;
		}

		@Override
		public void onClick(final View v) {

			this.saveDataActivity.setSelectedDir(this.saveDataActivity.getCurrentDir());

			// Si todavia estamos descargando los datos, mostramos un dialogo de espera.
			// Sera el propio proceso de descarga el que guarde los datos cuando detecte
			// un directorio seleccionado.
			if (SaveDataActivity.this.getDownloadFileTask() != null && SaveDataActivity.this.getDownloadFileTask().getStatus() == Status.RUNNING) {
				SaveDataActivity.this.progressDialog = ProgressDialog.show(SaveDataActivity.this, "", //$NON-NLS-1$
						getString(R.string.dialog_msg_loading_data), true);
				return;
			}


			final UrlParametersToSave params = this.saveDataActivity.getParameters();
			saveData(params.getData(), this.saveDataActivity.getSelectedDir(), params.getFileName());
		}

	}

	void closeActivity() {

		if (this.progressDialog != null && this.progressDialog.isShowing()) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					SaveDataActivity.this.progressDialog.dismiss();
				}
			});
		}

		finish();
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
		if (critical) {
			closeActivity();
		}
	}

	@Override
	public void onSendingDataError(Throwable error, boolean critical) {
		Logger.w(
				"es.gob.afirma",
				"No se pudo enviar al servidor el resultado de la operacion de guardado");
		if (critical) {
			closeActivity();
		}
	}

	//	@Override
//	protected void onStop() {
//		super.onStop();
//		EasyTracker.getInstance().activityStop(this);
//	}


	@Override
	public void onBackPressed() {
		sendData(ErrorManager.genError(ErrorManager.ERROR_CANCELLED_OPERATION), true);
	}
}