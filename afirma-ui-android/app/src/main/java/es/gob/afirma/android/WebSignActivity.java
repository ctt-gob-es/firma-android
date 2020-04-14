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
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.security.KeyChainException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.CipherDataManager;
import es.gob.afirma.android.crypto.MSCBadPinException;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.android.crypto.SignResult;
import es.gob.afirma.android.gui.DownloadFileTask;
import es.gob.afirma.android.gui.DownloadFileTask.DownloadDataListener;
import es.gob.afirma.android.gui.MessageDialog;
import es.gob.afirma.android.gui.SendDataTask;
import es.gob.afirma.android.gui.SendDataTask.SendDataListener;
import es.gob.afirma.core.AOUnsupportedSignFormatException;
import es.gob.afirma.core.misc.protocol.ParameterException;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParser;
import es.gob.afirma.core.misc.protocol.UrlParametersToSign;
import es.gob.afirma.core.signers.AOSignConstants;

/** Actividad dedicada a la firma de los datos recibidos en la entrada mediante un certificado
 * del almac&eacute;n central seleccionado por el usuario. */
public final class WebSignActivity extends SignFragmentActivity implements DownloadDataListener,
                                                                           SendDataListener {
	private static final char CERT_SIGNATURE_SEPARATOR = '|';

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private static final String OK_SERVER_RESULT = "OK"; //$NON-NLS-1$

	/** Juego de carateres UTF-8. */
	private static final String DEFAULT_URL_ENCODING = "UTF-8"; //$NON-NLS-1$

	private final static String EXTRA_RESOURCE_TITLE = "es.gob.afirma.android.title"; //$NON-NLS-1$
	private final static String EXTRA_RESOURCE_EXT = "es.gob.afirma.android.exts"; //$NON-NLS-1$
	private final static String EXTRA_RESOURCE_EXCLUDE_DIRS = "es.gob.afirma.android.excludedDirs"; //$NON-NLS-1$

	private final static String INTENT_ENTRY_ACTION = "es.gob.afirma.android.SIGN_SERVICE"; //$NON-NLS-1$
	
	/** C&oacute;digo de petici\u00F3n usado para invocar a la actividad que selecciona el fichero para firmar. */
	private static final int SELECT_FILE_REQUEST_CODE = 102;

	private boolean fileChooserOpenned;

	private UrlParametersToSign parameters;

	private DownloadFileTask downloadFileTask = null;

	private MessageDialog messageDialog;
	MessageDialog getMessageDialog() {
		return this.messageDialog;
	}

	private ProgressDialog progressDialog = null;
	void setProgressDialog(final ProgressDialog pd) {
		this.progressDialog = pd;
	}

    private static final int REQUEST_WRITE_STORAGE = 112;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.i(ES_GOB_AFIRMA, " -- WebSignActivity onCreate");

		if (getIntent() == null || getIntent().getData() == null) {
			Log.w(ES_GOB_AFIRMA, "No se han indicado parametros de entrada para la actividad");  //$NON-NLS-1$
			closeActivity();
			return;
		}

		Log.d(ES_GOB_AFIRMA, "URI de invocacion: " + getIntent().getDataString()); //$NON-NLS-1$

		if (getIntent().getDataString() == null) {
			Log.w(ES_GOB_AFIRMA, "Se ha invocado sin URL a la actividad de firma por protocolo. Se cierra la actividad"); //$NON-NLS-1$
			closeActivity();
			return;
		}

		try {
			this.parameters = ProtocolInvocationUriParser.getParametersToSign(getIntent().getDataString());
		}
		catch (final ParameterException e) {
			Log.e(ES_GOB_AFIRMA, "Error en los parametros de firma: " + e.toString(), e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true);
			return;
		}
		catch (final Throwable e) {
			Log.e(ES_GOB_AFIRMA, "Error grave en el onCreate de WebSignActivity: " + e.toString(), e); //$NON-NLS-1$
			e.printStackTrace();
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true);
			return;
		}

        // Si no hay permisos de acceso al almacenamiento, se piden
        if (!(
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )) {
            Log.i("es.gob.afirma", "No se tiene permiso de escritura en memoria");
            ActivityCompat.requestPermissions(
                this,
                new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                REQUEST_WRITE_STORAGE
            );
        }
        // Si ya se tienen permisos, se procede
        else {
            processSignRequest();
        }
	}

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Log.i("es.gob.afirma", "Concedido permiso de escritura en memoria");
				processSignRequest();
			}
			else {
				// Si no nos dan los permisos, directamente cerramos la aplicacion
				android.os.Process.killProcess(android.os.Process.myPid());
				finishAffinity();
			}
        }
    }

	/** Inicia el proceso de firma con los parametros previamente configurados. */
	private void processSignRequest() {

		// Si no tenemos datos ni un fichero de descargar, cargaremos un fichero del dispositivo
		if (this.parameters.getData() == null && this.parameters.getFileId() == null) {
			Log.i(ES_GOB_AFIRMA, "Se va a cargar un fichero local para la firma"); //$NON-NLS-1$
			// Comprobamos que no este ya abierta la pantalla de seleccion, ya que puede ser un caso
			// de cancelacion de la seleccion de fichero, en cuyo caso no deseamos que se vuelva a abrir
			if (!this.fileChooserOpenned) {
				openSelectFileActivity();
			}
			else {
				this.fileChooserOpenned = false;
			}
		}

		// Si no se han indicado datos y si el identificador de un fichero remoto, lo recuperamos para firmarlos
		else if (this.parameters.getData() == null && this.parameters.getFileId() != null) {
			Log.i(ES_GOB_AFIRMA, "Se van a descargar los datos desde servidor con el identificador: " + this.parameters.getFileId()); //$NON-NLS-1$
			this.downloadFileTask = new DownloadFileTask(
					this.parameters.getFileId(),
					this.parameters.getRetrieveServletUrl(),
					this
			);
			this.downloadFileTask.execute();
		}

		// Si tenemos los datos, cargamos un certificado para firmarlos
		else {
			Log.i(ES_GOB_AFIRMA, "Se inicia la firma de los datos obtenidos por parametro"); //$NON-NLS-1$
			showProgressDialog(getString(R.string.dialog_msg_signning));
			sign(
					this.parameters.getOperation().name(),
					this.parameters.getData(),
					this.parameters.getSignatureFormat(),
					this.parameters.getSignatureAlgorithm(),
					this.parameters.getExtraParams());
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.i(ES_GOB_AFIRMA, " -- WebSignActivity onStart");
	}

	/** Identifica las extensiones de los ficheros que se pueden firmar con un formato de firma.
	 * @param signatureFormat Formato de firma.
	 * @return Extensiones. */
	private static String identifyExts(final String signatureFormat) {

		if (AOSignConstants.SIGN_FORMAT_PADES.equals(signatureFormat) ||
			AOSignConstants.SIGN_FORMAT_PADES_TRI.equals(signatureFormat)) {
				return ".pdf"; //$NON-NLS-1$
		}
		return null;
	}

	/** Env&iacute;a los datos indicado a un servlet. En caso de error, cierra la aplicaci&oacute;n.
	 * @param data Datos que se desean enviar. */
	private void sendData(final String data, final boolean critical) {

		Log.i(ES_GOB_AFIRMA, " -- WebSignActivity sendData");

		Log.i(ES_GOB_AFIRMA, "Se almacena el resultado en el servidor con el Id: " + this.parameters.getId()); //$NON-NLS-1$

		new SendDataTask(
			this.parameters.getId(),
			this.parameters.getStorageServletUrl().toExternalForm(),
			data,
			this,
			critical
		).execute();
	}

	/** Muestra un mensaje de error y lo env&iacute;a al servidor para que la p&aacute;gina Web
	 * tenga constancia de &eacute;l.
	 * @param errorId Identificador del error.
	 * @param critical <code>true</code> si debe mostrarse el error al usuario, <code>false</code>
	 *                    en caso contrario.
	 */
	private void launchError(final String errorId, final boolean critical) {

		Log.i(ES_GOB_AFIRMA, " -- WebSignActivity launchError");

		try {
			if (INTENT_ENTRY_ACTION.equals(getIntent().getAction())){
				Log.i(ES_GOB_AFIRMA, "Devolvemos el error a la app solicitante"); //$NON-NLS-1$
				sendDataIntent(Activity.RESULT_CANCELED, ErrorManager.genError(errorId, null));
			}
			else {
				sendData(URLEncoder.encode(ErrorManager.genError(errorId, null), DEFAULT_URL_ENCODING), critical);
			}
		}
		catch (final UnsupportedEncodingException e) {
			// No puede darse, el soporte de UTF-8 es obligatorio
			Log.e(ES_GOB_AFIRMA,
				"No se ha podido enviar la respuesta al servidor por error en la codificacion " + DEFAULT_URL_ENCODING, e //$NON-NLS-1$
			);
		}
		catch (final Throwable e) {
			Log.e(ES_GOB_AFIRMA,
				"Error desconocido al enviar el error obtenido al servidor: " + e, e //$NON-NLS-1$
			);
		}
	}

	/** Muestra un mensaje de advertencia al usuario.
	 * @param message Mensaje que se desea mostrar. */
	private void showErrorMessage(final String message) {

		dismissProgressDialog();

		if (this.messageDialog == null) {
			this.messageDialog = MessageDialog.newInstance(message);
			this.messageDialog.setListener(new CloseActivityDialogAction());
			this.messageDialog.setDialogBuilder(this);
		}
		//this.messageDialog.setMessage(message);

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					WebSignActivity.this.getMessageDialog().show(getSupportFragmentManager(), "ErrorDialog"); //$NON-NLS-1$;
				}
				catch (final Exception e) {
					// Si falla el mostrar el error (posiblemente por no disponer de un contexto grafico para mostrarlo)
					// se mostrara en un
					Toast.makeText(WebSignActivity.this, message, Toast.LENGTH_LONG).show();
				}

			}
		});
	}

	/** Muestra un mensaje de advertencia al usuario.
	 * @param message Mensaje que se desea mostrar. */
	private void showErrorMessageOnToast(final String message) {

		dismissProgressDialog();
		dismissMessageDialog();

		runOnUiThread(
			new Runnable() {
				@Override
				public void run() {
					Toast.makeText(WebSignActivity.this, message, Toast.LENGTH_LONG).show();
				}
			}
		);
	}

	@Override
	protected void onSigningError(final KeyStoreOperation op, final String msg, final Throwable t) {
		if (op == KeyStoreOperation.LOAD_KEYSTORE) {
			launchError(ErrorManager.ERROR_ESTABLISHING_KEYSTORE, true);
		}
		else if (op == KeyStoreOperation.SELECT_CERTIFICATE) {

			if (t instanceof SelectKeyAndroid41BugException) {
				launchError(ErrorManager.ERROR_PKE_ANDROID_4_1, true);
			}
			else if (t instanceof KeyChainException) {
				launchError(ErrorManager.ERROR_PKE, true);
			}
			else if (t instanceof PendingIntent.CanceledException) {
				Log.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false);

			}
			else {
				Log.e(ES_GOB_AFIRMA, "Error al recuperar la clave del certificado de firma", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_PKE, true);
			}
		}
		else if (op == KeyStoreOperation.SIGN) {
			if (t instanceof MSCBadPinException) {
				Log.e(ES_GOB_AFIRMA, "PIN erroneo: " + t); //$NON-NLS-1$
				showErrorMessage(getString(R.string.error_msc_pin));
				launchError(ErrorManager.ERROR_MSC_PIN, false);
			}
			else if (t instanceof AOUnsupportedSignFormatException) {
				Log.e(ES_GOB_AFIRMA, "Formato de firma no soportado: " + t); //$NON-NLS-1$
				showErrorMessage(getString(R.string.error_format_not_supported));
				launchError(ErrorManager.ERROR_NOT_SUPPORTED_FORMAT, true);
			}
			else {
				Log.e(ES_GOB_AFIRMA, "Error al firmar", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_SIGNING, true);
			}
		}
		Log.e(ES_GOB_AFIRMA, "Error desconocido", t); //$NON-NLS-1$
		launchError(ErrorManager.ERROR_SIGNING, true);
	}

	private void showProgressDialog(final String message) {
		runOnUiThread(
			new Runnable() {
				@Override
				public void run() {
					try {
						setProgressDialog(ProgressDialog.show(WebSignActivity.this, "", message, true)); //$NON-NLS-1$
					}
					catch (final Throwable e) {
						Log.e(ES_GOB_AFIRMA, "No se ha podido mostrar el dialogo de progreso", e); //$NON-NLS-1$
					}
				}
			}
		);
	}

	@Override
	public synchronized void onDownloadingDataSuccess(final byte[] data) {

        Log.i(ES_GOB_AFIRMA, " -- WebSignActivity onDownloadingDataSuccess");

        Log.i(ES_GOB_AFIRMA, "Se ha descargado correctamente la configuracion de firma almacenada en servidor"); //$NON-NLS-1$
        Log.i(ES_GOB_AFIRMA, "Cantidad de datos descargada: " + (data == null ? -1 : data.length)); //$NON-NLS-1$

        // Si hemos tenido que descargar los datos desde el servidor, los desciframos y llamamos
        // al dialogo de seleccion de certificados para la firma
        final byte[] decipheredData;
        try {
            decipheredData = CipherDataManager.decipherData(data, this.parameters.getDesKey());
        }
        catch (final IOException e) {
            Log.e(ES_GOB_AFIRMA, "Los datos proporcionados no est&aacute;n correctamente codificados en base 64", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        }
        catch (final GeneralSecurityException e) {
            Log.e(ES_GOB_AFIRMA, "Error al descifrar los datos recuperados del servidor para la firma", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        }
        catch (final IllegalArgumentException e) {
            Log.e(ES_GOB_AFIRMA, "Los datos recuperados no son un base64 valido", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        }
        catch (final Throwable e) {
            Log.e(ES_GOB_AFIRMA, "Error desconocido durante el descifrado de los datos", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        }

        Log.i(ES_GOB_AFIRMA, "Se han descifrado los datos y se inicia su analisis:\n" + new String(decipheredData)); //$NON-NLS-1$

        try {
            this.parameters = ProtocolInvocationUriParser.getParametersToSign(decipheredData);
        }
        catch (final ParameterException e) {
            Log.e(ES_GOB_AFIRMA, "Error en los parametros XML de configuracion de firma: " + e.toString(), e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        }
        catch (final Throwable e) {
            Log.e(ES_GOB_AFIRMA, "Error desconocido al analizar los datos descargados desde el servidor", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        }

        // Comprobamos que en los datos descargados de servidor esten los datos a firmar y,
        // en caso contrario, se permite al usuario cargarlos de fichero. Si ya tenemos los
        // datos, los firmamos directamente
        if (this.parameters.getData() == null) {
            Log.i(ES_GOB_AFIRMA, "Se va a cargar un fichero local para la firma"); //$NON-NLS-1$
            // Comprobamos que no este ya abierta la pantalla de seleccion, ya que puede ser un caso
            // de cancelacion de la seleccion de fichero, en cuyo caso no deseamos que se vuelva a abrir
            if (!this.fileChooserOpenned) {
                openSelectFileActivity();
            }
            else {
                this.fileChooserOpenned = false;
            }
        }
        else {
            Log.i(ES_GOB_AFIRMA, "Se inicia la firma de los datos descargados desde el servidor"); //$NON-NLS-1$
            showProgressDialog(getString(R.string.dialog_msg_signning));
            try {
                sign(
					this.parameters.getOperation().name(),
					this.parameters.getData(),
					this.parameters.getSignatureFormat(),
					this.parameters.getSignatureAlgorithm(),
					this.parameters.getExtraParams()
				);
            }
            catch (final Exception e) {
                Log.e(ES_GOB_AFIRMA, "Error durante la firma", e); //$NON-NLS-1$
                showErrorMessage(getString(R.string.error_signing_config));
            }
        }
	}

	@Override
	public synchronized void onDownloadingDataError(final String msg, final Throwable t) {
		Log.e(ES_GOB_AFIRMA, "Error durante la descarga de la configuracion de firma guardada en servidor:" + msg + (t != null ? ": " + t.toString() : ""), t); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		showErrorMessage(getString(R.string.error_server_connect));
	}

	@Override
	public void onSigningSuccess(final SignResult signature) {

		Log.i(ES_GOB_AFIRMA, " -- WebSignActivity onSigningSuccess");

		Log.i(ES_GOB_AFIRMA, "Firma generada correctamente. Se cifra el resultado.");

		// Ciframos si nos dieron clave privada, si no subimos los datos sin cifrar
		final String data;
		try {
			data = CipherDataManager.cipherData(
				signature.getSignature(),
				this.parameters.getDesKey()
			);
		}
		catch (final GeneralSecurityException e) {
			Log.e(ES_GOB_AFIRMA, "Error en el cifrado de la firma", e); //$NON-NLS-1$
			launchError(ErrorManager.ERROR_CIPHERING, true);
			return;
		}
		catch (final Throwable e) {
			Log.e(ES_GOB_AFIRMA, "Error desconocido al cifrar el resultado de la firma", e); //$NON-NLS-1$
			launchError(ErrorManager.ERROR_CIPHERING, true);
			return;
		}

		String signingCert;
		try {
			signingCert = CipherDataManager.cipherData(
				signature.getSigningCertificate().getEncoded(),
				this.parameters.getDesKey()
			);
		}
		catch (final GeneralSecurityException e) {
			Log.e(ES_GOB_AFIRMA, "Error en el cifrado del certificado de firma: " + e, e); //$NON-NLS-1$
			signingCert = null;
		}

		//Si la aplicacion se ha llamado desde intent de firma devolvemos datos a la aplicacion llamante
		if (getIntent().getAction() != null && getIntent().getAction().equals(INTENT_ENTRY_ACTION)){
			Log.i(ES_GOB_AFIRMA, "Devolvemos datos a la app solicitante"); //$NON-NLS-1$
			sendDataIntent(
				Activity.RESULT_OK,
				signingCert != null ? signingCert + CERT_SIGNATURE_SEPARATOR + data : data
			);
		}
		else {
			Log.i(ES_GOB_AFIRMA, "Firma cifrada. Se envia al servidor."); //$NON-NLS-1$
			sendData(
				signingCert != null ? signingCert + CERT_SIGNATURE_SEPARATOR + data : data,
				true
			);
			Log.i(ES_GOB_AFIRMA, "Firma enviada."); //$NON-NLS-1$
		}
	}

	private void sendDataIntent (final int isOk, String data) {
		Intent result = new Intent();
		result.setData(Uri.parse(data));
		if (getParent() == null) {
			setResult(isOk, result);
		}
		else {
			getParent().setResult(isOk, result);
		}
		finish();
		closeActivity();
	}

	@Override
	public void onSendingDataSuccess(final byte[] result, final boolean critical) {
		Log.i(ES_GOB_AFIRMA, "Resultado del deposito de la firma: " + (result == null ? null : new String(result))); //$NON-NLS-1$

		if (result == null || !new String(result).trim().equals(OK_SERVER_RESULT)) {
			Log.e(ES_GOB_AFIRMA, "No se pudo entregar la firma al servlet: " + (result == null ? null : new String(result))); //$NON-NLS-1$
			if (critical) {
				showErrorMessage(getString(R.string.error_sending_data));
				return;
			}
		}
		else {
			Log.i(ES_GOB_AFIRMA, "Resultado entregado satisfactoriamente."); //$NON-NLS-1$
		}
		closeActivity();
	}

	@Override
	public void onSendingDataError(final Throwable error, final boolean critical) {

		Log.e(ES_GOB_AFIRMA, "Se ejecuta la funcion de error en el envio de datos", error); //$NON-NLS-1$
		error.printStackTrace();

		if (critical) {
			dismissProgressDialog();
			showErrorMessage(getString(R.string.error_sending_data));
			return;
		}
		closeActivity();
	}

	/** Comprueba si esta abierto el di&aacute;logo de espera y lo cierra en dicho caso. */
	private void dismissProgressDialog() {
		if (this.progressDialog != null) {
			this.progressDialog.dismiss();
		}
	}

	/** Comprueba si esta abierto el di&aacute;logo de mensajes y lo cierra en dicho caso. */
	private void dismissMessageDialog() {
		if (this.messageDialog != null && this.messageDialog.isVisible()) {
					this.messageDialog.dismiss();
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

		Log.i(ES_GOB_AFIRMA, " -- WebSignActivity onActivityResult");

		if (requestCode == SELECT_FILE_REQUEST_CODE) {
			// Si el usuario cancelo la seleccion del fichero a firmar
			if (resultCode == RESULT_CANCELED) {
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false);
				return;
			}
			else if (resultCode == RESULT_OK) {

				final String filename = data.getStringExtra(FileChooserActivity.RESULT_DATA_STRING_FILENAME);

				int n;
				final byte[] buffer = new byte[1024];
				final ByteArrayOutputStream baos;
				try {
					baos = new ByteArrayOutputStream();
					final InputStream is = new FileInputStream(filename);
					while ((n = is.read(buffer)) > 0) {
						baos.write(buffer, 0, n);
					}
					is.close();
				}
				catch (final IOException e) {
					Log.e(ES_GOB_AFIRMA, "Error al cargar el fichero, se dara al usuario la posibilidad de reintentar", e); //$NON-NLS-1$
					showErrorMessageOnToast(getString(R.string.error_loading_selected_file, filename));
					openSelectFileActivity();
					return;
				}
				catch (final Throwable e) {
					Log.e(ES_GOB_AFIRMA, "Error desconocido al cargar el fichero", e); //$NON-NLS-1$
					showErrorMessageOnToast(getString(R.string.error_loading_selected_file, filename));
					e.printStackTrace();
					return;
				}

				this.parameters.setData(baos.toByteArray());

				processSignRequest();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void openSelectFileActivity() {

		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setClass(this, FileChooserActivity.class);
		intent.putExtra(EXTRA_RESOURCE_TITLE, getString(R.string.title_activity_choose_sign_file));
		intent.putExtra(EXTRA_RESOURCE_EXCLUDE_DIRS, FileSystemConstants.COMMON_EXCLUDED_DIRS);
		final String exts = identifyExts(this.parameters.getSignatureFormat());
		if (exts != null) {
			intent.putExtra(EXTRA_RESOURCE_EXT, exts);
		}

		this.fileChooserOpenned = true;
		startActivityForResult(intent, SELECT_FILE_REQUEST_CODE);
	}

	/** Accion para el cierre de la actividad. */
	private final class CloseActivityDialogAction implements DialogInterface.OnClickListener {

		CloseActivityDialogAction() {
			// Constructor vacio para evitar el sintetico
		}

		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			closeActivity();
		}
	}

	void closeActivity() {
		// Cerramos a la fuerza para, en siguientes ejecuciones, no se vuelvan a cargar los mismos datos
		finishAffinity();
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_HOME) {
			launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false);
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		dismissProgressDialog();
		dismissMessageDialog();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (this.downloadFileTask != null) {
			Log.d(ES_GOB_AFIRMA, "WebSignActivity onDestroy: Cancelamos la descarga"); //$NON-NLS-1$
			try {
				this.downloadFileTask.cancel(true);
			}
			catch(final Exception e) {
				Log.e(ES_GOB_AFIRMA, "No se ha podido cancelar el procedimiento de descarga de los datos", e); //$NON-NLS-1$
			}
		}
		super.onDestroy();
	}
}