/* Copyright (C) 2022 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.android.batch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.os.AsyncTask;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateEncodingException;
import java.util.Properties;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.batch.client.BatchSigner;
import es.gob.afirma.android.crypto.MSCBadPinException;
import es.gob.afirma.android.errors.AppErrorCode;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.misc.http.HttpError;
import es.gob.afirma.core.misc.protocol.UrlParametersForBatch;
import es.gob.jmulticard.card.BadPinException;
import es.gob.jmulticard.jse.provider.SignatureAuthException;

/**
 * Tarea que ejecuta una firma electr&oacute;nica por lotes.
 * @author Jose Montero
 */
public class SignBatchTask extends AsyncTask<Void, Void, byte[]>{

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private final PrivateKeyEntry pke;
	private final UrlParametersForBatch batchParameters;
	private final Properties pkcs1ExtraParams;
	private final SignBatchListener signBatchListener;

	private Throwable t;

    final Activity activity;
    private ProgressDialog progressDialog = null;

	/** Construye la tarea encargada de realizar la operaci&oacute;n.
	 * @param pke Clave privada para la firma.
	 * @param batchParameters Par&aacute;metros para la configuraci&oacute;n de la firma.
	 * @param pkcs1ExtraParams Par&aacute;metros adicionales para la firma PKCS#1.
	 * @param signBatchListener Manejador para el tratamiento del resultado de la firma. */
	public SignBatchTask(final PrivateKeyEntry pke,
						 final UrlParametersForBatch batchParameters,
						 final Properties pkcs1ExtraParams,
                         final SignBatchListener signBatchListener,
                         final Activity activity) {

		this.pke = pke;
		this.batchParameters = batchParameters;
		this.pkcs1ExtraParams = pkcs1ExtraParams;
		this.signBatchListener = signBatchListener;
		this.t = null;
        this.activity = activity;
	}

	@Override
	protected byte[] doInBackground(final Void... params) {

        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    setProgressDialog(
                            ProgressDialog.show(
                                    activity,
                                    "",
                                    activity.getString(R.string.batch_signing),
                                    true)); //$NON-NLS-1$
                }
                catch (Throwable e) {
                    Logger.w(ES_GOB_AFIRMA, "No se pudo mostrar el dialogo de progreso de firma", e);
                }
            }
        });

		// Generacion de la firma
		byte[] batchResult = null;
		try {
			// Ejecutamos la operacion pertinente. Si no se indico nada, por defecto, el metodo
			// que devuelve la operacion indica que es firma
			if (batchParameters.isJsonBatch()) {
				batchResult = BatchSigner.signJSON(
						Base64.encode(batchParameters.getData(), true),
						batchParameters.getBatchPresignerUrl(),
						batchParameters.getBatchPostSignerUrl(),
						pke.getCertificateChain(),
						pke.getPrivateKey(),
						this.pkcs1ExtraParams
				);
			} else {
				throw new IllegalStateException(AppErrorCode.Request.JSON_NOT_FORMED_CORRECTLY.toString()); //$NON-NLS-1$
			}
		}
		catch (final AOCancelledOperationException e) {
			this.t = e;
		}
		catch (final IllegalArgumentException e) {
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.REQUEST_PARAM_NOT_VALID.toString() + e); //$NON-NLS-1$
			this.t = e;
		}
		catch (final CertificateEncodingException e) {
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Request.ENCODING_CERT.toString() + e); //$NON-NLS-1$
			this.t = e;
		}
		catch (final HttpError e) {
			Logger.e(ES_GOB_AFIRMA, "El servicio devolvio un error: " + e); //$NON-NLS-1$
			if (e.getResponseCode() == 400) {
				this.t = new IllegalArgumentException(AppErrorCode.Request.REQUEST_PARAM_NOT_VALID.toString(), e);
			}
			else if (e.getResponseCode() / 100 == 4) {
				this.t = e;
			}
			else {
				this.t = new AOException(e, AppErrorCode.Internal.GENERAL_ERROR);
			}
		}
		catch (final AOException e) {
			if (e.getCause() instanceof AOException && e.getCause().getCause() instanceof ActivityNotFoundException) {
				// Solo se dara este error (hasta la fecha) cuando se intente cargar el dialogo de PIN de
				// una tarjeta criptografica
				Logger.e(ES_GOB_AFIRMA, "Se ha intentado cargar el dialogo de PIN de una tarjeta criptografica: " + e); //$NON-NLS-1$
				this.t = new MSCBadPinException("Se ha intentado cargar el dialogo de PIN de una tarjeta criptografica", e); //$NON-NLS-1$
			} else if (e instanceof AOException
					&& e.getCause() != null && e.getCause() instanceof SignatureAuthException
					&& e.getCause().getCause()!= null && e.getCause().getCause() instanceof BadPinException){
				this.t = new MSCBadPinException(e.getCause().getCause().getMessage(), e); //$NON-NLS-1$
			}
			else {
				Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.GENERAL_ERROR.toString(), e); //$NON-NLS-1$
				this.t = e;
			}
		}
		catch (final Exception e) {
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.GENERAL_ERROR.toString(), e); //$NON-NLS-1$
			this.t = e;
		}
		return batchResult;
	}

	@Override
	protected void onPostExecute(final byte[] result) {
        super.onPostExecute(result);
        if (getProgressDialog().isShowing()) {
            getProgressDialog().dismiss();
        }
		if (result == null) {
			this.signBatchListener.onSignError(this.t);
		} else {
			this.signBatchListener.onSignSuccess(result);
		}
	}


    ProgressDialog getProgressDialog() {
        return this.progressDialog;
    }

    void setProgressDialog(final ProgressDialog pd) {
        this.progressDialog = pd;
    }

	/** Interfaz que debe implementar el manejador del resultado de la operaci&oacute;n de firma por lotes.
	 * @author Jose Montero. */
	public interface SignBatchListener {

		/** Gestiona el resultado de la operaci&oacute;n de firma por lotes cuando termina correctamente.
		 * @param batchResult Resultado de la firma de lote. */
		void onSignSuccess(byte[] batchResult);

		/**
		 * Gestiona un error en la operaci&oacute;n de firma por lotes.
		 * @param t Excepcion o error lanzada en la operaci&oacute;n de firma por lotes. */
		void onSignError(Throwable t);
	}
}
