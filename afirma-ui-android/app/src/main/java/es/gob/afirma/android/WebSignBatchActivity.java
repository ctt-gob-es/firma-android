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

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.security.KeyChainException;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.util.HashMap;
import java.util.Map;

import es.gob.afirma.R;
import es.gob.afirma.android.batch.SignBatchFragmentActivity;
import es.gob.afirma.android.batch.TriphaseDataParser;
import es.gob.afirma.android.crypto.AndroidHttpManager;
import es.gob.afirma.android.crypto.CipherDataManager;
import es.gob.afirma.android.crypto.MSCBadPinException;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.android.gui.DownloadFileTask;
import es.gob.afirma.android.gui.MessageDialog;
import es.gob.afirma.android.gui.SendDataTask;
import es.gob.afirma.android.gui.SendDataTask.SendDataListener;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.AOUnsupportedSignFormatException;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.misc.http.UrlHttpManagerFactory;
import es.gob.afirma.core.misc.protocol.ParameterException;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParser;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParserUtil;
import es.gob.afirma.core.signers.ExtraParamsProcessor;

/** Actividad dedicada a la firma por lotes de los datos recibidos en la entrada mediante un certificado
 * del almac&eacute;n central seleccionado por el usuario. */
public final class WebSignBatchActivity extends SignBatchFragmentActivity
		implements SendDataListener, DownloadFileTask.DownloadDataListener {

	private static final char RESULT_SEPARATOR = '|';
	private static final String ES_GOB_AFIRMA = "es.gob.afirma";
	private static final String OK_SERVER_RESULT = "OK";
	private final static String INTENT_ENTRY_ACTION = "es.gob.afirma.android.SIGN_SERVICE";

	/** Juego de carateres UTF-8. */
	private static final String DEFAULT_URL_ENCODING = "UTF-8"; //$NON-NLS-1$

	private MessageDialog messageDialog;
	MessageDialog getMessageDialog() {
		return this.messageDialog;
	}

	private ProgressDialog progressDialog = null;
	void setProgressDialog(final ProgressDialog pd) {
		this.progressDialog = pd;
	}

	/**
	 * Hilo para solicitar activamente a traves del servidor intermedio que se
	 * espere a que termine de ejecutarse la aplicaci&oacute;n.
	 */
	private static Thread activeWaitingThread = null;

	/**
	 * Versi&oacute;n del protocolo de comunicaci&oacute;n solicitada.
	 */
	private static int requestedProtocolVersion = -1;

	static {
		// Instalamos el gestor de descargas que deseamos utilizar en las invocaciones por
		// protocolo a la aplicacion
		UrlHttpManagerFactory.install(new AndroidHttpManager());
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent() == null || getIntent().getData() == null) {
			Logger.w(ES_GOB_AFIRMA, "No se han indicado parametros de entrada para la actividad");  //$NON-NLS-1$
			closeActivity();
			return;
		}

		// No queremos que se pueda acceder a esta actividad desde el historial de aplicaciones. Si
		// se intenta, cargaremos en su lugar la pantalla principal
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
			== Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return;
		}

		// Si no estamos creando ahora la pantalla (por ser una rotacion)
		if (savedInstanceState != null){
			Logger.i(ES_GOB_AFIRMA, "Se esta relanzando la actividad. Se omite volver a iniciar el proceso de firma de lote");
			return;
		}

		Logger.d(ES_GOB_AFIRMA, "URI de invocacion: " + getIntent().getDataString()); //$NON-NLS-1$

		if (getIntent().getDataString() == null) {
			Logger.w(ES_GOB_AFIRMA, "Se ha invocado sin URL a la actividad de firma de lote por protocolo. Se cierra la actividad"); //$NON-NLS-1$
			closeActivity();
			return;
		}

		// Extraemos los parametros de la URL
		final Map<String, String> urlParams = extractParamsForBatch(getIntent().getDataString());
		try {
			setBatchParams(ProtocolInvocationUriParserUtil.getParametersToBatch(urlParams));
		} catch (ParameterException e) {
			Logger.e(ES_GOB_AFIRMA, "Error con el parametro utilizado", e);
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, getString(R.string.error_bad_params), true);
			return;
		}

		if (requestedProtocolVersion == -1) {
			requestedProtocolVersion = parseProtocolVersion(getBatchParams().getMinimumProtocolVersion());
		}

		// Si se indica un identificador de fichero, es que el JSON de definicion de lote
		// se tiene que descargar previamente desde el servidor intermedio
		if (getBatchParams().getFileId() != null) {
			Logger.i(ES_GOB_AFIRMA, "Se van a descargar el lote desde servidor con el identificador: " + getBatchParams().getFileId()); //$NON-NLS-1$
			new DownloadFileTask(getBatchParams().getFileId(), getBatchParams().getRetrieveServletUrl(), this).execute();
			return;
		}

		loadKeyStore(this);
	}

	/**
	 * Recibe el lote que se debe firmar despu&eacute;s de descargarlo del servidor intermedio.
	 * @param batchDefinition Lote para firmar.
	 */
	@Override
	public void onDownloadingDataSuccess(byte[] cipheredBatchDefinition) {

		byte[] batchDefinition;
		try {
			batchDefinition = CipherDataManager.decipherData(cipheredBatchDefinition, getBatchParams().getDesKey());
		}
		catch (final IOException e) {
			Logger.e(ES_GOB_AFIRMA, "Los datos proporcionados no est&aacute;n correctamente codificados en base 64", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, getString(R.string.error_bad_params), true);
			return;
		}
		catch (final GeneralSecurityException e) {
			Logger.e(ES_GOB_AFIRMA, "Error al descifrar los datos recuperados del servidor para la firma", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, getString(R.string.error_bad_params), true);
			return;
		}
		catch (final IllegalArgumentException e) {
			Logger.e(ES_GOB_AFIRMA, "Los datos recuperados no son un base64 valido", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, getString(R.string.error_bad_params), true);
			return;
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA, "Error desconocido durante el descifrado de los datos", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, getString(R.string.error_bad_params), true);
			return;
		}

		try {
			setBatchParams(ProtocolInvocationUriParser.getParametersToBatch(batchDefinition));
		} catch (ParameterException e) {
			Logger.e(ES_GOB_AFIRMA, "Error con el parametro utilizado", e);
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, getString(R.string.error_bad_params), true);
			return;
		}

		if (getBatchParams().isActiveWaiting()) {
			requestWait(getBatchParams().getStorageServletUrl(), getBatchParams().getId());
		}

		loadKeyStore(this);
	}

	/**
	 * Ejecuta el proceso de error debido a un fallo en la descarga del lote del servidor intermedio.
	 * @param msg Mensaje del error
	 * @param t Error producido
	 */
	@Override
	public void onDownloadingDataError(String msg, Throwable t) {
		Logger.e(ES_GOB_AFIRMA, "Error durante la descarga del lote de firmas del servidor intermedio", t);
		showErrorMessage(getString(R.string.error_json));
		launchError(ErrorManager.ERROR_SIGNING, getString(R.string.error_json), true);
		return;
	}

	/** Inicia el proceso de firma con los parametros previamente configurados. */
	private void processSignRequest() {
		Logger.i(ES_GOB_AFIRMA, "Se inicia la firma de los datos obtenidos por parametro");
		showProgressDialog(getString(R.string.dialog_msg_signning));

		sign(getBatchParams());
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	/** Muestra un mensaje de error y lo env&iacute;a al servidor para que la p&aacute;gina Web
	 * tenga constancia de &eacute;l.
	 * @param errorId Identificador del error.
	 * @param errorMsg Mensaje de error.
	 */
	private void launchError(final String errorId, final String errorMsg, final boolean critical) {

		try {
			if (INTENT_ENTRY_ACTION.equals(getIntent().getAction())){
				Logger.i(ES_GOB_AFIRMA, "Devolvemos el error a la app solicitante");
				sendDataIntent(Activity.RESULT_CANCELED, ErrorManager.genError(errorId, null));
			}
			else {
				sendData(URLEncoder.encode(ErrorManager.genError(errorId, errorMsg), DEFAULT_URL_ENCODING), critical);
			}
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA,
					"Error desconocido al enviar el error obtenido al servidor: " + e, e
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

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					WebSignBatchActivity.this.getMessageDialog().show(getSupportFragmentManager(), "ErrorDialog"); //$NON-NLS-1$;
				}
				catch (final Exception e) {
					Toast.makeText(WebSignBatchActivity.this, message, Toast.LENGTH_LONG).show();
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
					Toast.makeText(WebSignBatchActivity.this, message, Toast.LENGTH_LONG).show();
				}
			}
		);
	}

	@Override
	protected void onSigningError(final KeyStoreOperation op, final String msg, final Throwable t) {
		if (op == KeyStoreOperation.LOAD_KEYSTORE) {
			Log.e(ES_GOB_AFIRMA, "Error al cargar el almacen de certificados", t);
			launchError(ErrorManager.ERROR_ESTABLISHING_KEYSTORE, t.getMessage(), true);
			return;
		}
		else if (op == KeyStoreOperation.SELECT_CERTIFICATE) {

			if (t instanceof SelectKeyAndroid41BugException) {
				Log.e(ES_GOB_AFIRMA, "Error al cargar el certificado, posiblemente relacionado por usar un alias de certificado no valido", t);
				launchError(ErrorManager.ERROR_PKE_ANDROID_4_1, t.getMessage(), true);
				return;
			}
			else if (t instanceof KeyChainException) {
				Log.e(ES_GOB_AFIRMA, "Error al cargar la clave del certificado", t);
				launchError(ErrorManager.ERROR_PKE, t.getMessage(), true);
				return;
			}
			else if (t instanceof PendingIntent.CanceledException) {
				Logger.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado", t);
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, t.getMessage(), false);
				return;
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error al recuperar la clave del certificado de firma", t);
				launchError(ErrorManager.ERROR_PKE, t.getMessage(), true);
				return;
			}
		}
		else if (op == KeyStoreOperation.SIGN) {
			if (t instanceof MSCBadPinException) {
				Logger.e(ES_GOB_AFIRMA, "PIN erroneo: " + t);
				showErrorMessage(getString(R.string.error_msc_pin));
				launchError(ErrorManager.ERROR_MSC_PIN, t.getMessage(), false);
				return;
			}
			else if (t instanceof AOUnsupportedSignFormatException) {
				Logger.e(ES_GOB_AFIRMA, "Formato de firma no soportado: " + t);
				showErrorMessage(getString(R.string.error_format_not_supported));
				launchError(ErrorManager.ERROR_NOT_SUPPORTED_FORMAT, t.getMessage(), true);
				return;
			}
			else if (t instanceof ExtraParamsProcessor.IncompatiblePolicyException) {
				Logger.e(ES_GOB_AFIRMA, "Los parametros configurados son incompatibles con la politica de firma: " + t);
				showErrorMessage(getString(R.string.error_signing_config));
				launchError(ErrorManager.ERROR_BAD_PARAMETERS, t.getMessage(), true);
				return;
			}
			else if (t instanceof AOException) {
				Logger.e(ES_GOB_AFIRMA, "Error controlado al firmar", t);
				launchError(ErrorManager.ERROR_SIGNING, t.getMessage(), true);
				return;
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error desconocido durante la firma", t);
				launchError(ErrorManager.ERROR_SIGNING, t.getMessage(), true);
				return;
			}
		}
		Logger.e(ES_GOB_AFIRMA, "Error desconocido", t);
		launchError(ErrorManager.ERROR_SIGNING, t.getMessage(), true);
	}

	private void showProgressDialog(final String message) {
		runOnUiThread(
			new Runnable() {
				@Override
				public void run() {
					try {
						setProgressDialog(ProgressDialog.show(WebSignBatchActivity.this, "", message, true));
					}
					catch (final Throwable e) {
						Logger.e(ES_GOB_AFIRMA, "No se ha podido mostrar el dialogo de progreso", e);
					}
				}
			}
		);
	}

	@Override
	public void onSigningSuccess(final String signature) {
		Logger.i(ES_GOB_AFIRMA, "Firma generada correctamente. Se cifra el resultado.");

		final StringBuilder result = new StringBuilder();

		// Si se nos ha indicado en la llamadada que devolvamos el certificado de firma, lo adjuntamos la resultado con un separador
		byte[] signingCertEncoded = null;
		if (getBatchParams().isCertNeeded()) {
			try {
				signingCertEncoded = getPke().getCertificate().getEncoded();
			} catch (final CertificateEncodingException e) {
				Logger.e(ES_GOB_AFIRMA, "No se ha podido codificar el certificado de firma para su devolucion", e); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_SIGNING, e.getMessage(), true);
				return;
			}
		}

		// Si hay clave de cifrado, ciframos
		if (getBatchParams().getDesKey() != null) {
			try {
				result.append(CipherDataManager.cipherData(signature.getBytes(), getBatchParams().getDesKey()));
				if (signingCertEncoded != null) {
					result.append(RESULT_SEPARATOR)
							.append(CipherDataManager.cipherData(signingCertEncoded, getBatchParams().getDesKey()));
				}
			}
			catch (final Exception e) {
				Logger.e(ES_GOB_AFIRMA, "Error en el cifrado de los datos a enviar", e); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_SIGNING, e.getMessage(), true);
				return;
			}
		}
		else {
			Logger.i(ES_GOB_AFIRMA, "Se omite el cifrado de los datos resultantes por no haberse proporcionado una clave de cifrado");
			result.append(Base64.encode(signature.getBytes()));
			if (signingCertEncoded != null) {
				result.append(RESULT_SEPARATOR).append(Base64.encode(signingCertEncoded));
			}
		}

		// Si la aplicacion se ha llamado desde intent de firma devolvemos datos a la aplicacion llamante
		if (getIntent().getAction() != null && getIntent().getAction().equals(INTENT_ENTRY_ACTION)){
			Logger.i(ES_GOB_AFIRMA, "Devolvemos datos a la app solicitante"); //$NON-NLS-1$
			sendDataIntent(Activity.RESULT_OK, result.toString());
		}
		else {
			Logger.i(ES_GOB_AFIRMA, "Firma cifrada. Se envia al servidor."); //$NON-NLS-1$
			try {
				sendData(result.toString(), true);
				Logger.i(ES_GOB_AFIRMA, "Firma enviada."); //$NON-NLS-1$
			}
			catch (final Throwable e) {
				Logger.e(ES_GOB_AFIRMA,
						"Error desconocido la firma al servidor al servidor: " + e, e //$NON-NLS-1$
				);
				onSendingDataError(e, true);
			}
		}
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

		// Si el usuario cancelo la seleccion del fichero a firmar
		if (resultCode == RESULT_CANCELED) {
			launchError(ErrorManager.ERROR_CANCELLED_OPERATION, "Operacion cancelada", false);
			return;
		}
		else if (resultCode == RESULT_OK) {
			try {
				processSignRequest();
			}
			catch (final Throwable e) {
				Logger.e(ES_GOB_AFIRMA, "Error durante la firma", e); //$NON-NLS-1$
				showErrorMessageOnToast(getString(R.string.error_signing));
				return;
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onKeyStoreError(KeyStoreOperation op, String msg, Throwable t) {
		if (op == KeyStoreOperation.LOAD_KEYSTORE) {
			launchError(ErrorManager.ERROR_ESTABLISHING_KEYSTORE, "Error cargando almacen", true);
		}
		else if (op == KeyStoreOperation.SELECT_CERTIFICATE) {

			if (t instanceof SelectKeyAndroid41BugException) {
				launchError(ErrorManager.ERROR_PKE_ANDROID_4_1, "Error cargando PKE", true);
			}
			else if (t instanceof KeyChainException) {
				launchError(ErrorManager.ERROR_PKE, "Error cargando PKE", true);
			}
			else if (t instanceof PendingIntent.CanceledException) {
				Logger.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, "El usuario no selecciono un certificado", false);
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error al recuperar el certificado", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_PKE, "Error al recuperar el certificado", true);
			}
		}
		Logger.e(ES_GOB_AFIRMA, "Error desconocido", t); //$NON-NLS-1$
		launchError(ErrorManager.ERROR_SELECTING_CERTIFICATE, "Error desconocido", true);
	}

	/**
	 * Parsea la cadena con la versi&oacute;n del protocolo de comunicacion
	 * solicitada.
	 *
	 * @param version Declarada del protocolo.
	 * @return Version de protocolo o {@code 1} si no era una cadena v&aacute;lida.
	 */
	private static int parseProtocolVersion(final String version) {
		int protocolVersion;
		try {
			protocolVersion = Integer.parseInt(version);
		} catch (final Exception e) {
			protocolVersion = 1;
		}
		return protocolVersion;
	}

	/**
	 * Extrae los parametros declarados en una URL con sus valores asignados.
	 * @param url URL de la que extraer los par&aacute;metros.
	 * @return Conjunto de par&aacute;metros con sus valores.
	 */
	private static Map<String, String> extractParamsForBatch(final String url) {

		final Map<String, String> params = new HashMap<>();

		final int initPos = url.indexOf('?') + 1;
		final String[] urlParams = url.substring(initPos).split("&"); //$NON-NLS-1$
		for (final String param : urlParams) {
			final int equalsPos = param.indexOf('=');
			if (equalsPos > 0) {
				try {
					params.put(
							param.substring(0, equalsPos),
							URLDecoder.decode(param.substring(equalsPos + 1), StandardCharsets.UTF_8.toString()));
				} catch (final UnsupportedEncodingException e) {
					Logger.e(ES_GOB_AFIRMA, "No se pudo decodificar el valor del parametro '" + param.substring(0, equalsPos) + "': " + e);
				}
			}
		}
		return params;
	}

	private static void requestWait(final URL storageServletUrl, final String id) {
		try {
			activeWaitingThread = new ActiveWaitingThread(storageServletUrl.toString(), id);
			activeWaitingThread.start();
		} catch (final Exception e) {
			Logger.w(ES_GOB_AFIRMA, "Se ha interrumpido la espera activa para la conexion con servidor intermedio", e); //$NON-NLS-1$
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

	/** Env&iacute;a los datos indicado a un servlet. En caso de error, cierra la aplicaci&oacute;n.
	 * @param data Datos que se desean enviar. */
	private void sendData(final String data, final boolean critical) {
		Logger.i(ES_GOB_AFIRMA, "Se almacena el resultado en el servidor con el Id: " + getBatchParams().getId()); //$NON-NLS-1$

		new SendDataTask(
				getBatchParams().getId(),
				getBatchParams().getStorageServletUrl(),
				data,
				this,
				critical
		).execute();
	}

	@Override
	public void onSendingDataSuccess(byte[] result, boolean critical) {
		Logger.i(ES_GOB_AFIRMA, "Resultado del deposito de la firma: " + (result == null ? null : new String(result))); //$NON-NLS-1$
		if (result == null || !new String(result).trim().equals(OK_SERVER_RESULT)) {
			Logger.e(ES_GOB_AFIRMA, "No se pudo entregar la firma al servlet: " + (result == null ? null : new String(result))); //$NON-NLS-1$
			if (critical) {
				showErrorMessage(getString(R.string.error_sending_data));
				return;
			}
		}
		else {
			Logger.i(ES_GOB_AFIRMA, "Resultado entregado satisfactoriamente."); //$NON-NLS-1$
		}
		closeActivity();
	}

	@Override
	public void onSendingDataError(Throwable error, boolean critical) {

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
		finishAffinity();
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_HOME) {
			launchError(ErrorManager.ERROR_CANCELLED_OPERATION, "Operacion cancelada", false);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		launchError(ErrorManager.ERROR_CANCELLED_OPERATION, "Operacion cancelada", false);
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		dismissProgressDialog();
		dismissMessageDialog();
		super.onStop();
	}
}