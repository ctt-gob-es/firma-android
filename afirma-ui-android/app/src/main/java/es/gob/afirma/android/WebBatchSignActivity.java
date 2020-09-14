/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 30 ago 2020
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
import android.util.Base64;
import android.view.KeyEvent;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.CipherDataManager;
import es.gob.afirma.android.crypto.MSCBadPinException;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.android.crypto.SignResult;
import es.gob.afirma.android.signers.batch.BatchReader;
import es.gob.afirma.android.signers.batch.signer.SingleSign;
import es.gob.afirma.android.gui.DownloadFileTask;
import es.gob.afirma.android.gui.DownloadFileTask.DownloadDataListener;
import es.gob.afirma.android.gui.MessageDialog;
import es.gob.afirma.android.gui.SendDataTask;
import es.gob.afirma.android.gui.SendDataTask.SendDataListener;
import es.gob.afirma.core.AOUnsupportedSignFormatException;
import es.gob.afirma.core.misc.protocol.ParameterException;
import es.gob.afirma.core.misc.protocol.ProtocolConstants;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParser;
import es.gob.afirma.core.misc.protocol.UrlParametersForBatch;
import es.gob.afirma.core.misc.protocol.UrlParametersToSign;
import es.gob.afirma.signers.batch.BatchException;

/** Actividad dedicada a la firma de los datos recibidos en la entrada mediante un certificado
 * del almac&eacute;n central seleccionado por el usuario. */
public class WebBatchSignActivity extends SignFragmentActivity implements DownloadDataListener,
                                                                           SendDataListener {
	private enum Modes { INIT, SIGN };
	private Modes mode;

	private static final char CERT_SIGNATURE_SEPARATOR = '|';

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private static final String OK_SERVER_RESULT = "OK"; //$NON-NLS-1$

	/** Juego de carateres UTF-8. */
	private static final String DEFAULT_URL_ENCODING = "UTF-8"; //$NON-NLS-1$

	private final static String INTENT_SIGN_SERVICE = "es.gob.afirma.android.SIGN_SERVICE"; //$NON-NLS-1$

	protected UrlParametersForBatch parametersBatch;
	private String intentSuffix;
	private BatchReader batchReader;
	private SingleSign actualSign;
	private Properties authMethod;
	private int totalDocumentosAFirmar;
	private int totalErrors;

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

        Logger.i(ES_GOB_AFIRMA, " -- WebMultiSignActivity onCreate");

        if (getIntent() == null || getIntent().getData() == null) {
            Logger.w(ES_GOB_AFIRMA, "No se han indicado parametros de entrada para la actividad");  //$NON-NLS-1$
            closeActivity();
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
            processSignRequest();
        }
	}

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Logger.i("es.gob.afirma", "Concedido permiso de escritura en memoria");
				processSignRequest();
			}
			else {
				// Si no nos dan los permisos, directamente cerramos la aplicacion
				android.os.Process.killProcess(android.os.Process.myPid());
				System.exit(1);
			}
        }
    }

    private void setIntentSuffix(Map<String, String> params) {
        StringBuilder intentParameters = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!"files".equals(entry.getKey()) && !"op".equals(entry.getKey()))
                intentParameters.append("&")
                        .append(entry.getKey())
                        .append("=")
                        .append(Uri.encode(entry.getValue()));
        }
        intentSuffix = intentParameters.toString();
    }

	/** Inicia el proceso de firma con los parametros previamente configurados. */
	private void processSignRequest() {
		try {
			String intentUri = getIntent().getDataString();
			this.parametersBatch = ProtocolInvocationUriParser.getParametersForBatch(intentUri);
			Map<String, String> params  = parserUri(intentUri);
			setIntentSuffix(params);
			setAuthMethod(params);
		}
		catch (final ParameterException e) {
			Logger.e(ES_GOB_AFIRMA, "Error en los parametros de firma: " + e.toString(), e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true);
			return;
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA, "Error grave en el onCreate de WebMultiSignActivity: " + e.toString(), e); //$NON-NLS-1$
			e.printStackTrace();
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true);
			return;
		}

		getBatchFile();
    }

    private void getBatchFile() {
		Logger.i(ES_GOB_AFIRMA, "Se va a descargar la definición del conjunto de ficheros a firmar");
		this.mode = Modes.INIT;
		this.downloadFileTask = new DownloadFileTask(
				parametersBatch.getFileId(),
				parametersBatch.getRetrieveServletUrl(),
				this.authMethod,
				this
		);
		this.downloadFileTask.execute();
	}

    /** Inicia el proceso de firma del siguiente fichero de la lista de operaciones */
    private void signNext() {
        actualSign = batchReader.getSigns().remove(0);
		try {
			Logger.i(ES_GOB_AFIRMA, "Se van a descargar los datos desde servidor con el identificador: " + this.actualSign.getId()); //$NON-NLS-1$

			this.downloadFileTask = new DownloadFileTask(
					this.actualSign.getId(),
					new URL(this.actualSign.getDataSource()),
					this.authMethod,
					this
			);
			this.downloadFileTask.execute();
		} catch (MalformedURLException e) {
			Logger.e(ES_GOB_AFIRMA, "Error en la url de descarga: " + this.actualSign.getDataSource() + " id fichero: " + this.actualSign.getId());
		}
	}

	private void setAuthMethod(Map<String, String> params) {
    	this.authMethod = new Properties();
    	try {
    		String auth = params.get("auth");
    		if (auth != null && !auth.isEmpty()) {
				JSONObject authJson = new JSONObject();
				this.authMethod.put(authJson.get("k"), authJson.get("v"));
			}
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA, "Error grave en el onCreate de WebMultiSignActivity: " + e.toString(), e); //$NON-NLS-1$
			e.printStackTrace();
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true);
			return;
		}
	}

    private boolean isValidOperation(String operation) {
		return UrlParametersToSign.Operation.getOperation(operation.toUpperCase()) != null;
	}

	@Override
	public void onStart() {
		super.onStart();
		Logger.i(ES_GOB_AFIRMA, " -- WebMultiSignActivity onStart");
	}

	/** Env&iacute;a los datos indicado a un servlet. En caso de error, cierra la aplicaci&oacute;n.
	 * @param data Datos que se desean enviar. */
	private void sendData(final String data, final boolean critical) {

		Logger.i(ES_GOB_AFIRMA, " -- WebMultiSignActivity sendData");

		Logger.i(ES_GOB_AFIRMA, "Se almacena el resultado en el servidor con el Id: " + this.actualSign.getSignId()); //$NON-NLS-1$

		new SendDataTask(
			this.actualSign.getSignId(),
			this.actualSign.getRetrieveServerUrl(),
			data,
			authMethod,
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

		Logger.i(ES_GOB_AFIRMA, " -- WebBatchSignActivity launchError");

		try {
			if (INTENT_SIGN_SERVICE.equals(getIntent().getAction())){
				Logger.i(ES_GOB_AFIRMA, "Devolvemos el error a la app solicitante"); //$NON-NLS-1$
				sendDataIntent(Activity.RESULT_CANCELED, ErrorManager.genError(errorId, null));
			}
			else {
				sendData(URLEncoder.encode(ErrorManager.genError(errorId, null), DEFAULT_URL_ENCODING), critical);
			}
		}
		catch (final UnsupportedEncodingException e) {
			// No puede darse, el soporte de UTF-8 es obligatorio
			Logger.e(ES_GOB_AFIRMA,
				"No se ha podido enviar la respuesta al servidor por error en la codificacion " + DEFAULT_URL_ENCODING, e //$NON-NLS-1$
			);
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA,
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

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					WebBatchSignActivity.this.getMessageDialog().show(getSupportFragmentManager(), "ErrorDialog"); //$NON-NLS-1$;
				}
				catch (final Exception e) {
					// Si falla el mostrar el error (posiblemente por no disponer de un contexto grafico para mostrarlo)
					// se mostrara en un
					Toast.makeText(WebBatchSignActivity.this, message, Toast.LENGTH_LONG).show();
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
					Toast.makeText(WebBatchSignActivity.this, message, Toast.LENGTH_LONG).show();
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
				Logger.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false);

			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error al recuperar la clave del certificado de firma", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_PKE, true);
			}
		}
		else if (op == KeyStoreOperation.SIGN) {
			if (t instanceof MSCBadPinException) {
				Logger.e(ES_GOB_AFIRMA, "PIN erroneo: " + t); //$NON-NLS-1$
				showErrorMessage(getString(R.string.error_msc_pin));
				launchError(ErrorManager.ERROR_MSC_PIN, false);
			}
			else if (t instanceof AOUnsupportedSignFormatException) {
				Logger.e(ES_GOB_AFIRMA, "Formato de firma no soportado: " + t); //$NON-NLS-1$
				showErrorMessage(getString(R.string.error_format_not_supported));
				launchError(ErrorManager.ERROR_NOT_SUPPORTED_FORMAT, true);
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error al firmar", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_SIGNING, true);
			}
		}
		Logger.e(ES_GOB_AFIRMA, "Error desconocido", t); //$NON-NLS-1$
		if (batchReader.isStopOnError())
			launchError(ErrorManager.ERROR_SIGNING, true);
		else {
			showErrorMessageOnToast(getString(R.string.error_signing));
			signNext();
		}
	}

	private void showProgressDialog(final String message) {
		dismissProgressDialog();
		runOnUiThread(
			new Runnable() {
				@Override
				public void run() {
					try {
						setProgressDialog(ProgressDialog.show(WebBatchSignActivity.this, "", message, true)); //$NON-NLS-1$
					}
					catch (final Throwable e) {
						Logger.e(ES_GOB_AFIRMA, "No se ha podido mostrar el dialogo de progreso", e); //$NON-NLS-1$
					}
				}
			}
		);
	}

	@Override
	public synchronized void onDownloadingDataSuccess(final byte[] data) {

		Logger.i(ES_GOB_AFIRMA, " -- WebBatchSignActivity onDownloadingDataSuccess");

		Logger.i(ES_GOB_AFIRMA, "Se ha descargado correctamente la configuracion de firma almacenada en servidor"); //$NON-NLS-1$
		Logger.i(ES_GOB_AFIRMA, "Cantidad de datos descargada: " + (data == null ? -1 : data.length)); //$NON-NLS-1$

		// Si hemos tenido que descargar los datos desde el servidor, los desciframos y llamamos
		// al dialogo de seleccion de certificados para la firma
		final byte[] decipheredData;
		try {
			byte[] desKey = this.parametersBatch.getDesKey();
			decipheredData = CipherDataManager.decipherData(data, desKey);

			if (this.mode.equals(Modes.INIT)) {
				initBatchSignData(decipheredData);
				this.totalDocumentosAFirmar = this.batchReader.getSigns().size();
				this.totalErrors = 0;
				this.mode = Modes.SIGN;
				signNext();
			} else
				initSign(decipheredData);
		} catch (final IOException e) {
			Logger.e(ES_GOB_AFIRMA, "Los datos proporcionados no est&aacute;n correctamente codificados en base 64", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			return;
		} catch (final GeneralSecurityException e) {
			Logger.e(ES_GOB_AFIRMA, "Error al descifrar los datos recuperados del servidor para la firma", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			return;
		} catch (final IllegalArgumentException e) {
			Logger.e(ES_GOB_AFIRMA, "Los datos recuperados no son un base64 valido", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			return;
		} catch (final BatchException e) {
			Logger.e(ES_GOB_AFIRMA, "Error en el Batch", e);
			showErrorMessage(getString(R.string.error_batch_process_error));
			return;
		} catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA, "Error desconocido durante el descifrado de los datos", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			return;
		}
	}

	public void initBatchSignData(final byte[] xml) throws BatchException {
		boolean result = false;
		Logger.i(ES_GOB_AFIRMA, "Se han descifrado los datos y se inicia su analisis:\n" + new String(xml)); //$NON-NLS-1$
		try {
			batchReader = new BatchReader();
			batchReader.parse(xml);
			Logger.i(ES_GOB_AFIRMA, "Se ha procesado el fichero bath, se encontraron " + this.batchReader.getSigns().size() + " documentos");
			result = true;
		} catch (IOException e) {
			Logger.e(ES_GOB_AFIRMA, "Error procesando el fichero batch " + e.toString(), e);
			throw new BatchException("Error procesando el fichero batch", e);
		}
	}

	public void initSign(final byte[] decipheredData) {
		Logger.i(ES_GOB_AFIRMA, "Se han descifrado los datos y se inicia su analisis:\n" + new String(decipheredData)); //$NON-NLS-1$
		Logger.i(ES_GOB_AFIRMA, "Se inicia la firma de los datos descargados desde el servidor"); //$NON-NLS-1$
		showProgressDialog(getString(R.string.dialog_msg_signning) + " " +
				(totalDocumentosAFirmar - (batchReader.getSigns().size())) + "/" + totalDocumentosAFirmar);
		try {
			sign(
					actualSign.getSubOperation().toString(),
					decipheredData,
					actualSign.getSignFormat().toString(),
					batchReader.getSignAlgorithm().toString(),
					actualSign.getExtraParams()
			);
		}
		catch (final Exception e) {
			Logger.e(ES_GOB_AFIRMA, "Error durante la firma", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_signing_config));
		}
	}

	@Override
	public synchronized void onDownloadingDataError(final String msg, final Throwable t) {
		Logger.e(ES_GOB_AFIRMA, "Error durante la descarga de la configuracion de firma guardada en servidor:" + msg + (t != null ? ": " + t.toString() : ""), t); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (mode.equals(Modes.INIT)) {
			showErrorMessage(getString(R.string.error_server_connect));
			closeActivity();
		} else
			showErrorMessageOnToast(getString(R.string.error_server_connect));
		totalErrors++;
		if (!batchReader.isStopOnError())
			signNext();
	}

	@Override
	public void onSigningSuccess(final SignResult signature) {

		Logger.i(ES_GOB_AFIRMA, " -- WebBatchSignActivity onSigningSuccess");

		Logger.i(ES_GOB_AFIRMA, "Firma generada correctamente. Se cifra el resultado.");

		// Ciframos si nos dieron clave privada, si no subimos los datos sin cifrar
		final String data;
		final byte[] cipherKey = this.parametersBatch.getDesKey();
		try {
			data = cipherKey != null ? 
					CipherDataManager.cipherData(signature.getSignature(),
					cipherKey) : Base64.encodeToString(signature.getSignature(), Base64.DEFAULT);

		}
		catch (final GeneralSecurityException e) {
			Logger.e(ES_GOB_AFIRMA, "Error en el cifrado de la firma", e); //$NON-NLS-1$
			launchError(ErrorManager.ERROR_CIPHERING, true);
			return;
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA, "Error desconocido al cifrar el resultado de la firma", e); //$NON-NLS-1$
			launchError(ErrorManager.ERROR_CIPHERING, true);
			return;
		}

		String signingCert;
		try {
			signingCert = cipherKey != null ? CipherDataManager.cipherData(
				signature.getSigningCertificate().getEncoded(),
				this.parametersBatch.getDesKey()) : Base64.encodeToString(signature.getSigningCertificate().getEncoded(), Base64.DEFAULT);
		}
		catch (final GeneralSecurityException e) {
			Logger.e(ES_GOB_AFIRMA, "Error en el cifrado del certificado de firma: " + e, e); //$NON-NLS-1$
			signingCert = null;
		}

		Logger.i(ES_GOB_AFIRMA, "Firma cifrada. Se envia al servidor."); //$NON-NLS-1$
		sendData(
			signingCert != null ? signingCert + CERT_SIGNATURE_SEPARATOR + data : data,
			true
		);
		Logger.i(ES_GOB_AFIRMA, "Firma enviada."); //$NON-NLS-1$
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
		Logger.i(ES_GOB_AFIRMA, "Resultado del deposito de la firma: " + (result == null ? null : new String(result))); //$NON-NLS-1$

		if (result == null || !new String(result).trim().equals(OK_SERVER_RESULT)) {
			Logger.e(ES_GOB_AFIRMA, "No se pudo entregar la firma al servlet: " + (result == null ? null : new String(result))); //$NON-NLS-1$
			if (critical) {
				totalErrors++;
				showErrorMessageOnToast(getString(R.string.error_sending_data));
			}
		}
		else {
			Logger.i(ES_GOB_AFIRMA, "Resultado entregado satisfactoriamente."); //$NON-NLS-1$
		}
		if (!isSignEnd())
            signNext();
		else {
			dismissProgressDialog();
			closeActivity();
		}
	}

	private boolean isSignEnd() {
		return batchReader.getSigns().size() == 0;
	}

	private boolean isLastDocument() {
		return isSignEnd();
	}

	@Override
	public void onSendingDataError(final Throwable error, final boolean critical) {

		Logger.e(ES_GOB_AFIRMA, "Se ejecuta la funcion de error en el envio de datos", error); //$NON-NLS-1$
		error.printStackTrace();

		if (critical) {
			showErrorMessageOnToast(getString(R.string.error_sending_data));
			if (batchReader.isStopOnError() || isLastDocument())
				closeActivity();
			else
				signNext();
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
		if (this.messageDialog != null) {
			this.messageDialog.dismiss();
			this.messageDialog = null;
		}
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

	void showProcessErrors() {
		dismissProgressDialog();
		dismissMessageDialog();
		String msg = getString(R.string.error_batch_process_error);
		msg += batchReader.isStopOnError() ? "" : "\n" + String.format(getString(R.string.error_batch_process_continue_on_error), totalErrors);
		showErrorMessage(msg);
		totalErrors = 0;
	}

	void closeActivity() {
		if (totalErrors > 0) {
			showProcessErrors();
		} else
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
			Logger.d(ES_GOB_AFIRMA, "WebBatchSignActivity onDestroy: Cancelamos la descarga"); //$NON-NLS-1$
			try {
				this.downloadFileTask.cancel(true);
			}
			catch(final Exception e) {
				Logger.e(ES_GOB_AFIRMA, "No se ha podido cancelar el procedimiento de descarga de los datos", e); //$NON-NLS-1$
			}
		}
		super.onDestroy();
	}

    //TODO: Proviene de afirma-core, añadir en ProtocolInvocationUriParser un método
    // para obtener el listado de fircheros y operacionesde la uri.
    /** Analiza la URL de entrada para obtener la lista de par&aacute;metros asociados.
     * @param uri URL de llamada.
     * @return Devuelve una tabla <i>hash</i> con cada par&aacute;metro asociado a un valor. */
    private static Map<String, String> parserUri(final String uri) {
        final Map<String, String> params = new HashMap<>();
        final String[] parameters = uri.substring(uri.indexOf('?') + 1).split("&"); //$NON-NLS-1$
        for (final String param : parameters) {
            if (param.indexOf('=') > 0) {
                try {
                    params.put(
                            param.substring(0, param.indexOf('=')),
                            param.indexOf('=') == param.length() - 1 ?
                                    "" : //$NON-NLS-1$
                                    URLDecoder.decode(param.substring(param.indexOf('=') + 1), StandardCharsets.UTF_8.name())
                    );
                }
                catch (final UnsupportedEncodingException e) {
                    params.put(
                            param.substring(0, param.indexOf('=')),
                            param.indexOf('=') == param.length() - 1 ? "" : param.substring(param.indexOf('=') + 1) //$NON-NLS-1$
                    );
                }
            }
        }

        // Agregamos como codigo de operacion el nombre de host de la URL
        Logger.i(ES_GOB_AFIRMA,"URI recibida: " + uri); //$NON-NLS-1$ //$NON-NLS-2$

        String path = uri.substring(uri.indexOf("://") + "://".length(), uri.indexOf('?') != -1 ? uri.indexOf('?') : uri.length()); //$NON-NLS-1$ //$NON-NLS-2$
        if (path.endsWith("/")) { //$NON-NLS-1$
            path = path.substring(0, path.length() - 1);
        }
        params.put(ProtocolConstants.OPERATION_PARAM, path.substring(path.lastIndexOf('/') + 1));

        return params;
    }
}