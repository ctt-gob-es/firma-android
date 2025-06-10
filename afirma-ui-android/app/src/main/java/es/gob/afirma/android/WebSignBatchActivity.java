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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.security.KeyChainException;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateEncodingException;
import java.util.HashMap;
import java.util.Map;

import es.gob.afirma.R;
import es.gob.afirma.android.batch.SignBatchFragmentActivity;
import es.gob.afirma.android.crypto.AndroidHttpManager;
import es.gob.afirma.android.crypto.CipherDataManager;
import es.gob.afirma.android.crypto.KeyStoreManagerListener;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.android.errors.AppErrorCode;
import es.gob.afirma.android.errors.AppKeyStoreErrorCode;
import es.gob.afirma.android.errors.ErrorManager;
import es.gob.afirma.android.errors.ErrorMapper;
import es.gob.afirma.android.gui.CustomDialog;
import es.gob.afirma.android.gui.DownloadFileTask;
import es.gob.afirma.android.gui.SendDataTask;
import es.gob.afirma.android.gui.SendDataTask.SendDataListener;
import es.gob.afirma.android.util.Utils;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.AOControlledException;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.AOUnsupportedSignFormatException;
import es.gob.afirma.core.ErrorCode;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.misc.http.HttpError;
import es.gob.afirma.core.misc.http.UrlHttpManagerFactory;
import es.gob.afirma.core.misc.protocol.ParameterException;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParser;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParserUtil;
import es.gob.afirma.core.signers.ExtraParamsProcessor;
import es.gob.afirma.keystores.KeyStoreErrorCode;

/** Actividad dedicada a la firma por lotes de los datos recibidos en la entrada mediante un certificado
 * del almac&eacute;n central seleccionado por el usuario. */
public final class WebSignBatchActivity extends SignBatchFragmentActivity
		implements KeyStoreManagerListener, SendDataListener, DownloadFileTask.DownloadDataListener {

	private static final char RESULT_SEPARATOR = '|';
	private static final String ES_GOB_AFIRMA = "es.gob.afirma";
	private final static String INTENT_ENTRY_ACTION = "es.gob.afirma.android.SIGN_BATCH_SERVICE";

	/** Juego de carateres UTF-8. */
	private static final String DEFAULT_URL_ENCODING = "UTF-8"; //$NON-NLS-1$

	private CustomDialog messageDialog;
	CustomDialog getMessageDialog() {
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
		Utils.setPortraitSmartphone(this);

		if (getIntent() == null || getIntent().getData() == null) {
			Logger.w(ES_GOB_AFIRMA, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID.toString());  //$NON-NLS-1$
			closeActivity();
			return;
		}

		// No queremos que se pueda acceder a esta actividad desde el historial de aplicaciones. Si
		// se intenta, cargaremos en su lugar la pantalla principal
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
			== Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(this, HomeActivity.class);
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
			Logger.w(ES_GOB_AFIRMA, AppErrorCode.Request.INVOCATION_WITHOUT_URL.toString());
			closeActivity();
			return;
		}

		// Extraemos los parametros de la URL
		final Map<String, String> urlParams = extractParamsForBatch(getIntent().getDataString());
		try {
			setBatchParams(ProtocolInvocationUriParserUtil.getParametersToBatch(urlParams, true));
		} catch (ParameterException e) {
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID.toString() + e, e); //$NON-NLS-1$
			showErrorMessage(AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
			return;
		}

		if (requestedProtocolVersion == -1) {
			requestedProtocolVersion = parseProtocolVersion(getBatchParams().getMinimumProtocolVersion());
		}

		// Si se indica un identificador de fichero, es que el JSON de definicion de lote
		// se tiene que descargar previamente desde el servidor intermedio
		if (getBatchParams().getFileId() != null) {
			Logger.i(ES_GOB_AFIRMA, "Se van a descargar el lote desde servidor con el identificador: " + getBatchParams().getFileId()); //$NON-NLS-1$
			if (getBatchParams().getRetrieveServletUrl() != null) {
				new DownloadFileTask(getBatchParams().getFileId(), getBatchParams().getRetrieveServletUrl(), this).execute();
			} else {
				Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.URL_DOWNLOAD_NOT_FOUND_BATCH.toString());
				launchError(ErrorManager.ERROR_BAD_PARAMETERS, true, AppErrorCode.Request.URL_DOWNLOAD_NOT_FOUND_BATCH);
			}
			return;
		}

		processSignRequest();
	}

	/**
	 * Recibe el lote que se debe firmar despu&eacute;s de descargarlo del servidor intermedio.
	 * @param cipheredBatchDefinition Lote para firmar.
	 */
	@Override
	public void onDownloadingDataSuccess(byte[] cipheredBatchDefinition) {

		byte[] batchDefinition;
		try {
			batchDefinition = CipherDataManager.decipherData(cipheredBatchDefinition, getBatchParams().getDesKey());
		}
		catch (final IOException e) {
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID + " - Los datos proporcionados no est&aacute;n correctamente codificados en base 64", e); //$NON-NLS-1$
			showErrorMessage(AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
			return;
		}
		catch (final GeneralSecurityException e) {
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID + " - Error al descifrar los datos recuperados del servidor para la firma", e); //$NON-NLS-1$
			showErrorMessage(AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
			return;
		}
		catch (final IllegalArgumentException e) {
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID + " - Los datos recuperados no son un base64 valido", e); //$NON-NLS-1$
			showErrorMessage(AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
			return;
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID + " - Error desconocido durante el descifrado de los datos", e); //$NON-NLS-1$
			showErrorMessage(AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
			return;
		}

		try {
			setBatchParams(ProtocolInvocationUriParser.getParametersToBatch(batchDefinition, true));
		} catch (AOException e) {
			Logger.e(ES_GOB_AFIRMA, e.getErrorCode() + " - Error con el parametro utilizado", e);
			showErrorMessage(e.getErrorCode());
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true, e.getErrorCode());
			return;
		}

		if (getBatchParams().isActiveWaiting()) {
			requestWait(getBatchParams().getStorageServletUrl(), getBatchParams().getId());
		}

		processSignRequest();
	}

	/**
	 * Ejecuta el proceso de error debido a un fallo en la descarga del lote del servidor intermedio.
	 * @param msg Mensaje del error
	 * @param t Error producido
	 */
	@Override
	public void onDownloadingDataError(String msg, Throwable t) {
		Logger.e(ES_GOB_AFIRMA,AppErrorCode.Communication.DOWNLOAD_SIGN + " - Error durante la descarga del lote de firmas del servidor intermedio", t);
		showErrorMessage(AppErrorCode.Communication.DOWNLOAD_SIGN);
		launchError(ErrorManager.ERROR_SIGNING, true, AppErrorCode.Communication.DOWNLOAD_SIGN);
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
	 * @param critical Error critico que obliga a cerrar la aplicaci&oacute;n.
	 * @param errorCode Categor&iacute;a del error
	 */
	private void launchError(final String errorId, final boolean critical, final ErrorCode errorCode) {

		try {
			if (INTENT_ENTRY_ACTION.equals(getIntent().getAction())){
				Logger.i(ES_GOB_AFIRMA, "Devolvemos el error a la app solicitante");
				sendErrorByIntent(errorId, ErrorManager.genError(errorId, errorCode, this));
			}
			else {
				sendData(URLEncoder.encode(ErrorManager.genError(errorId, errorCode, this), DEFAULT_URL_ENCODING), critical);
			}
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA,
					"Error desconocido al enviar el error obtenido al servidor: " + e, e
			);
		}
	}

	/** Muestra un mensaje de advertencia al usuario.
	 * @param errorCode Error que se desea mostrar. */
	private void showErrorMessage(final ErrorCode errorCode) {

		showErrorMessage(null, errorCode);
	}

	/** Muestra un mensaje de advertencia al usuario.
	 * @param title T&iacute;tulo para el di&aacute;logo de error.
	 * @param errorCode Error que se desea mostrar. */
	private void showErrorMessage(final String title, final ErrorCode errorCode) {
		dismissProgressDialog();

		String dlgTitle;
		if (title != null) {
			dlgTitle = title;
		} else {
			dlgTitle = getString(R.string.error_ocurred);
		}

		String message = ErrorMapper.getErrorMsgFormatted(this, errorCode);

		if (this.messageDialog == null) {
			this.messageDialog = new CustomDialog(this, R.drawable.warn_icon, dlgTitle, message,
					getString(R.string.ok));
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					WebSignBatchActivity.this.getMessageDialog().show(); //$NON-NLS-1$;
				}
				catch (final Exception e) {
					Toast.makeText(WebSignBatchActivity.this, message, Toast.LENGTH_LONG).show();
				}

			}
		});
	}

	@Override
	protected void onSigningError(final KeyStoreOperation op,  final Throwable t) {
		if (op == KeyStoreOperation.LOAD_KEYSTORE) {
			ErrorCode errorCode = KeyStoreErrorCode.Internal.LOADING_KEYSTORE_INTERNAL_ERROR;
			Log.e(ES_GOB_AFIRMA, errorCode.toString(), t);
			launchError(ErrorManager.ERROR_ESTABLISHING_KEYSTORE, true, errorCode);
			return;
		}
		else if (op == KeyStoreOperation.SELECT_CERTIFICATE) {
			if (t instanceof SelectKeyAndroid41BugException) {
				Log.e(ES_GOB_AFIRMA, AppKeyStoreErrorCode.Internal.ALIAS_NOT_VALID.toString(), t);
				launchError(ErrorManager.ERROR_PKE_ANDROID_4_1, true, AppKeyStoreErrorCode.Internal.ALIAS_NOT_VALID);
			}
			else if (t instanceof KeyChainException) {
				Log.e(ES_GOB_AFIRMA, KeyStoreErrorCode.Internal.LOADING_PRIVATE_KEY_ERROR.toString(), t);
				launchError(ErrorManager.ERROR_PKE, true, KeyStoreErrorCode.Internal.LOADING_PRIVATE_KEY_ERROR);
			}
			else if (t instanceof PendingIntent.CanceledException) {
				Logger.e(ES_GOB_AFIRMA, ErrorCode.Functional.CANCELLED_OPERATION.toString(), t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false, ErrorCode.Functional.CANCELLED_OPERATION);
			}
			else {
				Logger.e(ES_GOB_AFIRMA, AppKeyStoreErrorCode.Internal.UNEXPECTED_RECOVERING_KEY.toString(), t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_PKE, true, AppKeyStoreErrorCode.Internal.UNEXPECTED_RECOVERING_KEY);
			}
			return;
		}
		else if (op == KeyStoreOperation.SIGN) {
			if (t instanceof AOCancelledOperationException) {
				Logger.i(ES_GOB_AFIRMA, ErrorCode.Functional.CANCELLED_OPERATION.toString() + t);
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false, ErrorCode.Functional.CANCELLED_OPERATION);
			}
			else if (t instanceof AOUnsupportedSignFormatException) {
				Logger.e(ES_GOB_AFIRMA, ErrorCode.Request.UNSUPPORTED_SIGNATURE_FORMAT.toString() + t); //$NON-NLS-1$
				showErrorMessage(ErrorCode.Request.UNSUPPORTED_SIGNATURE_FORMAT);
				launchError(ErrorManager.ERROR_NOT_SUPPORTED_FORMAT, true, ErrorCode.Request.UNSUPPORTED_SIGNATURE_FORMAT);
			}
			else if (t instanceof IllegalArgumentException) {
				Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID.toString(), t); //$NON-NLS-1$
				showErrorMessage(AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
				launchError(ErrorManager.ERROR_BAD_PARAMETERS, true, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID);
				return;
			}
			else if (t instanceof CertificateEncodingException) {
				Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.ENCODING_CERT.toString()); //$NON-NLS-1$
				showErrorMessage(AppErrorCode.Request.ENCODING_CERT);
				launchError(ErrorManager.ERROR_BAD_PARAMETERS, true, AppErrorCode.Request.ENCODING_CERT);
				return;
			}
			else if (t instanceof ExtraParamsProcessor.IncompatiblePolicyException) {
				Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.PARAM_NOT_COMPATIBLE_POLICY.toString() + t); //$NON-NLS-1$
				showErrorMessage(AppErrorCode.Request.PARAM_NOT_COMPATIBLE_POLICY);
				launchError(ErrorManager.ERROR_BAD_PARAMETERS, true, AppErrorCode.Request.PARAM_NOT_COMPATIBLE_POLICY);
			}
			else if (t instanceof HttpError) {
				Logger.e(ES_GOB_AFIRMA, AppErrorCode.Communication.COMMUNICATION_WITH_SERVICE.toString(), t);
				launchError(ErrorManager.ERROR_COMMUNICATING_WITH_WEB, true, AppErrorCode.Communication.COMMUNICATION_WITH_SERVICE);
			}
			else if (t instanceof AOControlledException) {
				ErrorCode errorSigning = ((AOControlledException) t).getErrorCode();
				Logger.e(ES_GOB_AFIRMA, errorSigning.toString(), t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_SIGNING, true, errorSigning);
			}
		}
		// Se introduce este error por si llegara alguno no controlado
		Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.GENERAL_ERROR.toString(), t); //$NON-NLS-1$
		launchError(ErrorManager.ERROR_SIGNING, true, AppErrorCode.Internal.GENERAL_ERROR);
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
	public void onSigningSuccess(final byte[] batchResult) {
		Logger.i(ES_GOB_AFIRMA, "Firma de lote generada correctamente. Se cifra el resultado.");

		final StringBuilder result = new StringBuilder();

		// Si se nos ha indicado en la llamadada que devolvamos el certificado de firma, lo adjuntamos al resultado con un separador
		byte[] signingCertEncoded = null;
		if (getBatchParams().isCertNeeded()) {
			try {
				signingCertEncoded = getPke().getCertificate().getEncoded();
			} catch (final CertificateEncodingException e) {
				Logger.e(ES_GOB_AFIRMA, AppKeyStoreErrorCode.Internal.CYPHERING_CERT_TO_SEND.toString(), e); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_SIGNING, true, AppKeyStoreErrorCode.Internal.CYPHERING_CERT_TO_SEND);
				return;
			}
		}

		// Si hay clave de cifrado, ciframos
		if (getBatchParams().getDesKey() != null) {
			try {
				result.append(CipherDataManager.cipherData(batchResult, getBatchParams().getDesKey()));
				if (signingCertEncoded != null) {
					result.append(RESULT_SEPARATOR)
							.append(CipherDataManager.cipherData(signingCertEncoded, getBatchParams().getDesKey()));
				}
			}
			catch (final Exception e) {
				Logger.e(ES_GOB_AFIRMA, AppKeyStoreErrorCode.Internal.CYPHERING_CERT_TO_SEND.toString(), e); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_SIGNING, true, AppKeyStoreErrorCode.Internal.CYPHERING_CERT_TO_SEND);
				return;
			}
		}
		else {
			Logger.i(ES_GOB_AFIRMA, "Se omite el cifrado de los datos resultantes por no haberse proporcionado una clave de cifrado");
			result.append(Base64.encode(batchResult));
			if (signingCertEncoded != null) {
				result.append(RESULT_SEPARATOR).append(Base64.encode(signingCertEncoded));
			}
		}
		String appName = getBatchParams().getAppName();
		String signType = SIGN_TYPE_BATCH;
		// Si la aplicacion se ha llamado desde intent de firma devolvemos datos a la aplicacion llamante
		if (getIntent().getAction() != null && getIntent().getAction().equals(INTENT_ENTRY_ACTION)){
			Logger.i(ES_GOB_AFIRMA, "Devolvemos datos a la app solicitante"); //$NON-NLS-1$
			// Registramos los datos sobre la firma realizada
			if (appName == null) {
				appName = getCallingPackage();
			}
			signType = SIGN_TYPE_BATCH_APP;
			sendDataByIntent(signingCertEncoded, batchResult);
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

		saveSignRecord(signType, appName);
	}

	/** Comprueba si esta abierto el di&aacute;logo de espera y lo cierra en dicho caso. */
	private void dismissProgressDialog() {
		if (this.progressDialog != null) {
			this.progressDialog.dismiss();
		}
	}

	/** Comprueba si esta abierto el di&aacute;logo de mensajes y lo cierra en dicho caso. */
	private void dismissMessageDialog() {
		if (this.messageDialog != null && this.messageDialog.isShowing()) {
					this.messageDialog.hide();
					this.messageDialog = null;
		}
	}

	@Override
	public void onKeyStoreError(KeyStoreOperation op, Throwable t) {
		if (op == KeyStoreOperation.LOAD_KEYSTORE) {
			launchError(ErrorManager.ERROR_ESTABLISHING_KEYSTORE, true, KeyStoreErrorCode.Internal.LOADING_KEYSTORE_INTERNAL_ERROR);
			return;
		}
		if (op == KeyStoreOperation.SELECT_CERTIFICATE) {
			if (t instanceof SelectKeyAndroid41BugException) {
				launchError(ErrorManager.ERROR_PKE_ANDROID_4_1, true, KeyStoreErrorCode.Internal.LOADING_PRIVATE_KEY_ERROR);
			}
			else if (t instanceof KeyChainException) {
				launchError(ErrorManager.ERROR_PKE, true, KeyStoreErrorCode.Internal.LOADING_PRIVATE_KEY_ERROR);
			}
			else if (t instanceof PendingIntent.CanceledException) {
				Logger.e(ES_GOB_AFIRMA, ErrorCode.Functional.CANCELLED_OPERATION.toString(), t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false, ErrorCode.Functional.CANCELLED_OPERATION);
			}
			else {
				Logger.e(ES_GOB_AFIRMA, KeyStoreErrorCode.Internal.LOADING_CERTIFICATE_ERROR.toString(), t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_PKE, true, KeyStoreErrorCode.Internal.LOADING_CERTIFICATE_ERROR);
			}
			return;
		}
		Logger.e(ES_GOB_AFIRMA, KeyStoreErrorCode.Internal.LOADING_CERTIFICATE_ERROR.toString(), t); //$NON-NLS-1$
		launchError(ErrorManager.ERROR_SELECTING_CERTIFICATE, true, KeyStoreErrorCode.Internal.LOADING_CERTIFICATE_ERROR);
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
							URLDecoder.decode(param.substring(equalsPos + 1), DEFAULT_URL_ENCODING));
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

	private void sendDataByIntent (byte[] cert, byte[] jsonResult) {
		Intent result = new Intent();
		result.putExtra("cert", cert);
		result.putExtra("batchresult", jsonResult);
		Activity activity = getParent() != null ? getParent() : this;
		activity.setResult(RESULT_OK, result);
		finish();
	}

	private void sendErrorByIntent (String errorId, String errorMsg) {
		Intent result = new Intent();
		result.putExtra("errorId", errorId);
		if (errorMsg != null){
			result.putExtra("errorMsg", errorMsg);
		}
		Activity activity = getParent() != null ? getParent() : this;
		activity.setResult(RESULT_CANCELED, result);
		finish();
	}

	/** Env&iacute;a los datos indicado a un servlet. En caso de error, cierra la aplicaci&oacute;n.
	 * @param data Datos que se desean enviar. */
	private void sendData(final String data, final boolean critical) {
		Logger.i(ES_GOB_AFIRMA, "Se almacena el resultado en el servidor con el Id: " + getBatchParams().getId()); //$NON-NLS-1$

		// Interrumpimos la espera activa si se daba
		if (activeWaitingThread != null) {
			activeWaitingThread.interrupt();
		}

		// Enviamos los datos al servidor
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
		closeActivity();
	}

	@Override
	public void onSendingDataError(Throwable e, boolean critical) {
		Logger.e(ES_GOB_AFIRMA, "No se pudo enviar el resultado al navegador: " + e, e); //$NON-NLS-1$
		if (critical) {
			showErrorMessage(AppErrorCode.Communication.UPLOAD_DATA);
		}
	}

	void closeActivity() {
		Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("CLOSE_ACTIVITY", true);
		startActivity(intent);
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_HOME) {
			launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false, ErrorCode.Functional.CANCELLED_OPERATION);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false, ErrorCode.Functional.CANCELLED_OPERATION);
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		dismissProgressDialog();
		dismissMessageDialog();
		super.onStop();
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(LocaleHelper.onAttach(base));
	}
}