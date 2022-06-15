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
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
public final class WebSignBatchActivity extends SignBatchFragmentActivity implements SendDataListener  {

	private static final char RESULT_SEPARATOR = '|';
	private static final String ES_GOB_AFIRMA = "es.gob.afirma";
	private static final String OK_SERVER_RESULT = "OK";
	private final static String INTENT_ENTRY_ACTION = "es.gob.afirma.android.SIGN_SERVICE";

	private MessageDialog messageDialog;
	MessageDialog getMessageDialog() {
		return this.messageDialog;
	}

	private ProgressDialog progressDialog = null;
	void setProgressDialog(final ProgressDialog pd) {
		this.progressDialog = pd;
	}

    private static final int REQUEST_WRITE_STORAGE = 112;

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

		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
			== Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return;
		}

		// Si no estamos creando ahora la pantalla (por se una rotacion)
		if (savedInstanceState != null){
			Logger.i(ES_GOB_AFIRMA, "Se esta relanzando la actividad. Se omite volver a iniciar el proceso de firma");
			return;
		}

		Logger.d(ES_GOB_AFIRMA, "URI de invocacion: " + getIntent().getDataString()); //$NON-NLS-1$

		if (getIntent().getDataString() == null) {
			Logger.w(ES_GOB_AFIRMA, "Se ha invocado sin URL a la actividad de firma por protocolo. Se cierra la actividad"); //$NON-NLS-1$
			closeActivity();
			return;
		}

        // Si no hay permisos de acceso al almacenamiento, se piden
        if (!(
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )) {
            Logger.i("es.gob.afirma", "No se tiene permiso de escritura en memoria");
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
			// Extraemos los parametros de la URL
			final Map<String, String> urlParams = extractParamsForBatch(getIntent().getDataString());
			try {
				this.setBatchParams(ProtocolInvocationUriParserUtil.getParametersForBatch(urlParams));
			} catch (ParameterException e) {
				Logger.e("Error con el parametro utilizado", e.toString());
				showErrorMessage(getString(R.string.error_bad_params));
				launchError(ErrorManager.ERROR_BAD_PARAMETERS, getString(R.string.error_bad_params));
				return;
			}

			if (requestedProtocolVersion == -1) {
				requestedProtocolVersion = parseProtocolVersion(this.getBatchParams().getMinimumProtocolVersion());
			}

			// Si se indica un identificador de fichero, es que el JSON o XML de definicion de lote
			// se tiene que descargar desde el servidor intermedio
			if (this.getBatchParams().getFileId() != null) {
				byte[] batchDefinition;
				try {
					batchDefinition = WebSignUtil.getDataFromRetrieveServlet(this.getBatchParams());
				} catch (final Exception e) {
					Logger.e("No se pueden recuperar los datos del servidor", e.toString());
					showErrorMessage(getString(R.string.error_server_connect));
					launchError(ErrorManager.ERROR_CIPHERING, getString(R.string.error_server_connect));
					return;
				}

				try {
					Map<String, String> paramsMap;
					paramsMap = TriphaseDataParser.parseParamsListJson(batchDefinition);
					this.setBatchParams(ProtocolInvocationUriParser.getParametersForBatch(paramsMap));
				} catch (ParameterException e) {
					Logger.e("Error con el parametro utilizado", e.toString());
					showErrorMessage(getString(R.string.error_bad_params));
					launchError(ErrorManager.ERROR_BAD_PARAMETERS, getString(R.string.error_bad_params));
					return;
				} catch (JSONException e) {
					Logger.e("Error en el JSON con el que se esta trabajando", e.toString());
					showErrorMessage(getString(R.string.error_json));
					launchError(ErrorManager.ERROR_NOT_SUPPORTED_FORMAT, getString(R.string.error_json));
					return;
				}

				if (this.getBatchParams().isActiveWaiting()) {
					requestWait(this.getBatchParams().getStorageServletUrl(), this.getBatchParams().getId());
				}
			}

			loadKeyStore(this);
		}
	}

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Logger.i("es.gob.afirma", "Concedido permiso de escritura en memoria");
				try {
					processSignRequest();
				}
				catch (final Throwable e) {
					Logger.e(ES_GOB_AFIRMA, "Error al generar la firma: " + e, e);
					onSigningError(KeyStoreOperation.SIGN, "Error al generar la firma", e);
				}
			}
			else {
				showErrorMessage(getString(R.string.error_no_read_permissions));
			}
        }
    }

	/** Inicia el proceso de firma con los parametros previamente configurados. */
	private void processSignRequest() {
		Logger.i(ES_GOB_AFIRMA, "Se inicia la firma de los datos obtenidos por parametro");
		showProgressDialog(getString(R.string.dialog_msg_signning));

		sign(this.getBatchParams());
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
	private void launchError(final String errorId, final String errorMsg) {

		try {
			if (INTENT_ENTRY_ACTION.equals(getIntent().getAction())){
				Logger.i(ES_GOB_AFIRMA, "Devolvemos el error a la app solicitante");
				sendDataIntent(Activity.RESULT_CANCELED, ErrorManager.genError(errorId, null));
			}
			else {
				showErrorMessage(errorMsg);
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
			launchError(ErrorManager.ERROR_ESTABLISHING_KEYSTORE, t.getMessage());
			return;
		}
		else if (op == KeyStoreOperation.SELECT_CERTIFICATE) {

			if (t instanceof SelectKeyAndroid41BugException) {
				Log.e(ES_GOB_AFIRMA, "Error al cargar el certificado, posiblemente relacionado por usar un alias de certificado no valido", t);
				launchError(ErrorManager.ERROR_PKE_ANDROID_4_1, t.getMessage());
				return;
			}
			else if (t instanceof KeyChainException) {
				Log.e(ES_GOB_AFIRMA, "Error al cargar la clave del certificado", t);
				launchError(ErrorManager.ERROR_PKE, t.getMessage());
				return;
			}
			else if (t instanceof PendingIntent.CanceledException) {
				Logger.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado", t);
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, t.getMessage());
				return;
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error al recuperar la clave del certificado de firma", t);
				launchError(ErrorManager.ERROR_PKE, t.getMessage());
				return;
			}
		}
		else if (op == KeyStoreOperation.SIGN) {
			if (t instanceof MSCBadPinException) {
				Logger.e(ES_GOB_AFIRMA, "PIN erroneo: " + t);
				showErrorMessage(getString(R.string.error_msc_pin));
				launchError(ErrorManager.ERROR_MSC_PIN, t.getMessage());
				return;
			}
			else if (t instanceof AOUnsupportedSignFormatException) {
				Logger.e(ES_GOB_AFIRMA, "Formato de firma no soportado: " + t);
				showErrorMessage(getString(R.string.error_format_not_supported));
				launchError(ErrorManager.ERROR_NOT_SUPPORTED_FORMAT, t.getMessage());
				return;
			}
			else if (t instanceof ExtraParamsProcessor.IncompatiblePolicyException) {
				Logger.e(ES_GOB_AFIRMA, "Los parametros configurados son incompatibles con la politica de firma: " + t);
				showErrorMessage(getString(R.string.error_signing_config));
				launchError(ErrorManager.ERROR_BAD_PARAMETERS, t.getMessage());
				return;
			}
			else if (t instanceof AOException) {
				Logger.e(ES_GOB_AFIRMA, "Error controlado al firmar", t);
				launchError(ErrorManager.ERROR_SIGNING, t.getMessage());
				return;
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error desconocido durante la firma", t);
				launchError(ErrorManager.ERROR_SIGNING, t.getMessage());
				return;
			}
		}
		Logger.e(ES_GOB_AFIRMA, "Error desconocido", t);
		launchError(ErrorManager.ERROR_SIGNING, t.getMessage());
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
		if (this.getBatchParams().isCertNeeded()) {
			try {
				signingCertEncoded = getPke().getCertificate().getEncoded();
			} catch (final CertificateEncodingException e) {
				Logger.e(ES_GOB_AFIRMA, "No se ha podido codificar el certificado de firma para su devolucion", e); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_SIGNING, e.getMessage());
				return;
			}
		}

		// Si hay clave de cifrado, ciframos
		if (this.getBatchParams().getDesKey() != null) {
			try {
				result.append(CipherDataManager.cipherData(signature.getBytes(), this.getBatchParams().getDesKey()));
				if (signingCertEncoded != null) {
					result.append(RESULT_SEPARATOR)
							.append(CipherDataManager.cipherData(signingCertEncoded, this.getBatchParams().getDesKey()));
				}
			}
			catch (final Exception e) {
				Logger.e(ES_GOB_AFIRMA, "Error en el cifrado de los datos a enviar", e); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_SIGNING, e.getMessage());
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
			launchError(ErrorManager.ERROR_CANCELLED_OPERATION, "Operacion cancelada");
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
			launchError(ErrorManager.ERROR_ESTABLISHING_KEYSTORE, "Error cargando almacen");
		}
		else if (op == KeyStoreOperation.SELECT_CERTIFICATE) {

			if (t instanceof SelectKeyAndroid41BugException) {
				launchError(ErrorManager.ERROR_PKE_ANDROID_4_1, "Error cargando PKE");
			}
			else if (t instanceof KeyChainException) {
				launchError(ErrorManager.ERROR_PKE, "Error cargando PKE");
			}
			else if (t instanceof PendingIntent.CanceledException) {
				Logger.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, "El usuario no selecciono un certificado");
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error al recuperar el certificado", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_PKE, "Error al recuperar el certificado");
			}
		}
		Logger.e(ES_GOB_AFIRMA, "Error desconocido", t); //$NON-NLS-1$
		launchError(ErrorManager.ERROR_SELECTING_CERTIFICATE, "Error desconocido");
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
			Logger.w("Se ha interrumpido la espera activa para la conexion con servidor intermedio", e.toString()); //$NON-NLS-1$
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
		Logger.i(ES_GOB_AFIRMA, "Se almacena el resultado en el servidor con el Id: " + this.getBatchParams().getId()); //$NON-NLS-1$

		new SendDataTask(
				this.getBatchParams().getId(),
				this.getBatchParams().getStorageServletUrl(),
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
			launchError(ErrorManager.ERROR_CANCELLED_OPERATION, "Operacion cancelada");
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		launchError(ErrorManager.ERROR_CANCELLED_OPERATION, "Operacion cancelada");
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		dismissProgressDialog();
		dismissMessageDialog();
		super.onStop();
	}
}