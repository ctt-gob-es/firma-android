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

import android.content.ActivityNotFoundException;
import android.os.AsyncTask;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateEncodingException;
import java.util.Properties;

import es.gob.afirma.android.Logger;
import es.gob.afirma.android.batch.client.BatchSigner;
import es.gob.afirma.android.crypto.MSCBadPinException;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.misc.http.HttpError;
import es.gob.afirma.core.misc.protocol.UrlParametersForBatch;

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

	/** Construye la tarea encargada de realizar la operaci&oacute;n.
	 * @param pke Clave privada para la firma.
	 * @param batchParameters Par&aacute;metros para la configuraci&oacute;n de la firma.
	 * @param pkcs1ExtraParams Par&aacute;metros adicionales para la firma PKCS#1.
	 * @param signBatchListener Manejador para el tratamiento del resultado de la firma. */
	public SignBatchTask(final PrivateKeyEntry pke,
						 final UrlParametersForBatch batchParameters,
						 final Properties pkcs1ExtraParams,
                         final SignBatchListener signBatchListener) {

		this.pke = pke;
		this.batchParameters = batchParameters;
		this.pkcs1ExtraParams = pkcs1ExtraParams;
		this.signBatchListener = signBatchListener;
		this.t = null;
	}

	@Override
	protected byte[] doInBackground(final Void... params) {

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
				throw new IllegalStateException("Tipo de operacion de firma no soportado"); //$NON-NLS-1$
			}
		}
		catch (final AOCancelledOperationException e) {
			this.t = e;
		}
		catch (final IllegalArgumentException e) {
			Logger.e(ES_GOB_AFIRMA, "Los parametros de la peticion no eran validos: " + e); //$NON-NLS-1$
			this.t = e;
		}
		catch (final CertificateEncodingException e) {
			Logger.e(ES_GOB_AFIRMA, "Error en la codificacion del certificado: " + e); //$NON-NLS-1$
			this.t = e;
		}
		catch (final HttpError e) {
			Logger.e(ES_GOB_AFIRMA, "El servicio devolvio un error: " + e); //$NON-NLS-1$
			if (e.getResponseCode() == 400) {
				this.t = new IllegalArgumentException("Los parametros enviados al servicio no eran validos", e);
			}
			else if (e.getResponseCode() / 100 == 4) {
				this.t = e;
			}
			else {
				this.t = new AOException("Error durante la operacion de firma en servidor", e);
			}
		}
		catch (final AOException e) {
			if (e.getCause() instanceof AOException && e.getCause().getCause() instanceof ActivityNotFoundException) {
				// Solo se dara este error (hasta la fecha) cuando se intente cargar el dialogo de PIN de
				// una tarjeta criptografica
				Logger.e(ES_GOB_AFIRMA, "Se ha intentado cargar el dialogo de PIN de una tarjeta criptografica: " + e); //$NON-NLS-1$
				this.t = new MSCBadPinException("Se ha intentado cargar el dialogo de PIN de una tarjeta criptografica", e); //$NON-NLS-1$
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error durante la operacion de firma: " + e, e); //$NON-NLS-1$
				this.t = e;
			}
		}
		catch (final Exception e) {
			Logger.e(ES_GOB_AFIRMA, "Error en la firma: " + e, e); //$NON-NLS-1$
			this.t = e;
		}
		return batchResult;
	}

	@Override
	protected void onPostExecute(final byte[] result) {
		if (result == null) {
			this.signBatchListener.onSignError(this.t);
		} else {
			this.signBatchListener.onSignSuccess(result);
		}
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
