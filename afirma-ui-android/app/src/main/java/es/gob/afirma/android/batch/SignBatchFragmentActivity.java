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

import android.app.PendingIntent;
import android.os.Build;
import android.security.KeyChainException;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateEncodingException;

import es.gob.afirma.R;
import es.gob.afirma.android.LoadKeyStoreFragmentActivity;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.crypto.MSCBadPinException;
import es.gob.afirma.android.crypto.MobileKeyStoreManager;
import es.gob.afirma.android.crypto.MobileKeyStoreManager.SelectCertificateEvent;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.http.HttpError;
import es.gob.afirma.core.misc.protocol.UrlParametersForBatch;

/** Esta actividad abstracta integra las funciones necesarias para la ejecuci&oacute;n de
 * operaciones de firma por lotes en una actividad. La actividad integra la l&oacute;gica necesaria para
 * utilizar DNIe 3.0 v&iacute;a NFC, DNIe 2.0/3.0 a trav&eacute;s de lector de tarjetas y el
 * almac&eacute;n de Android. */
public abstract class SignBatchFragmentActivity extends LoadKeyStoreFragmentActivity
											implements  MobileKeyStoreManager.PrivateKeySelectionListener,
														SignBatchTask.SignBatchListener {

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private UrlParametersForBatch batchParams;
	private PrivateKeyEntry pke;

	/**
	 * Inicia el proceso de firma.
	 * @param batchParams Firma de lotes a realizar
     */
	public void sign(final UrlParametersForBatch batchParams) {

		this.batchParams = batchParams;

		// Iniciamos la carga del almacen
		loadKeyStore(this);
	}

	@Override
	public synchronized void keySelected(final SelectCertificateEvent kse) {

		try {
			pke = kse.getPrivateKeyEntry();
		}
		catch (final KeyChainException e) {
			if ("4.1.1".equals(Build.VERSION.RELEASE) || "4.1.0".equals(Build.VERSION.RELEASE) || "4.1".equals(Build.VERSION.RELEASE)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Logger.e(ES_GOB_AFIRMA, "Error al extraer la clave en Android " + Build.VERSION.RELEASE + ": " + e); //$NON-NLS-1$ //$NON-NLS-2$
				onSigningError(KeyStoreOperation.SELECT_CERTIFICATE, getString(R.string.error_android_4_1), new SelectKeyAndroid41BugException(e));
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "No se pudo extraer la clave privada del certificado: " + e); //$NON-NLS-1$
				onSigningError(KeyStoreOperation.SELECT_CERTIFICATE, "No se pudo extraer la clave privada del certificado", e);
			}
			return;
		}
		catch (final AOCancelledOperationException e) {
			Logger.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado: " + e); //$NON-NLS-1$
			onSigningError(KeyStoreOperation.SELECT_CERTIFICATE, "El usuario no selecciono un certificado", new PendingIntent.CanceledException(e));
			return;
		}
		// Cuando se instala el certificado desde el dialogo de seleccion, Android da a elegir certificado
		// en 2 ocasiones y en la segunda se produce un "java.lang.AssertionError". Se ignorara este error.
		catch (final AssertionError e) {
			Logger.e(ES_GOB_AFIRMA, "Posible error al insertar un nuevo certificado en el almacen. No se hara nada", e); //$NON-NLS-1$
			return;
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA, "Error al recuperar la clave del certificado de firma", e); //$NON-NLS-1$
			onSigningError(KeyStoreOperation.SELECT_CERTIFICATE, "Error al recuperar la clave del certificado de firma", e); //$NON-NLS-1$
			return;
		}

		// Ya cargado el certificado, eliminamos el CAN de memoria y el objeto para que se vuelva a pedir
		if (getPasswordCallback() != null) {
            getPasswordCallback().clearPassword();
            setPasswordCallback(null);
		}

		try {
			doSign(pke);
		}
		catch (final Exception e) {
			onSigningError(KeyStoreOperation.SIGN, "Error durante la operacion de firma", e);
		}
	}

	private void doSign(final PrivateKeyEntry keyEntry) {

		if (keyEntry == null) {
			onSigningError(KeyStoreOperation.SIGN, "No se pudo extraer la clave privada del certificado", new Exception());
			return;
		}

		new SignBatchTask(
			keyEntry,
			this.batchParams,
			this
		).execute();
	}

	protected PrivateKeyEntry getPke() {
		return this.pke;
	}

	protected UrlParametersForBatch getBatchParams() {
		return this.batchParams;
	}

	protected void setBatchParams(UrlParametersForBatch batchParams) {
		this.batchParams = batchParams;
	}

	@Override
	public synchronized void onLoadingKeyStoreSuccess(final MobileKeyStoreManager msm) {

		// Si el usuario cancelo la insercion de PIN o cualquier otro dialogo del almacen
		if(msm == null){
			onSigningError(KeyStoreOperation.LOAD_KEYSTORE, "El usuario cancelo la operacion durante la carga del almacen", new PendingIntent.CanceledException("Se cancela la seleccion del almacen"));
			return;
		}
		msm.getPrivateKeyEntryAsynchronously(this);
	}

	@Override
	public void onSignSuccess(final String signatureResult) {
		onSigningSuccess(signatureResult);
	}

	@Override
	public void onSignError(final Throwable t) {
		if (t instanceof AOCancelledOperationException) {
			onSigningError(KeyStoreOperation.SIGN, "Operacion cancelada por el usuario", t);
		}
		else if (t instanceof IllegalArgumentException) {
			onSigningError(KeyStoreOperation.SIGN, "Los datos proporcionados al servicio no son validos", t);
		}
		else if (t instanceof CertificateEncodingException) {
			onSigningError(KeyStoreOperation.SIGN, "Error al codificar el certificado", t);
		}
		else if (t instanceof HttpError) {
			onSigningError(KeyStoreOperation.SIGN, "No se pudo conectar con el servicio de firma de lotes", t);
		}
		else if (t instanceof MSCBadPinException) {
			onSigningError(KeyStoreOperation.SIGN, "No se pudo solicitar el PIN de la tarjeta criptografica", t);
		}
		else if (t instanceof AOException) {
			onSigningError(KeyStoreOperation.SIGN, "El servicio de firma de lotes devolvio un error", t);
		}else {
			onSigningError(KeyStoreOperation.SIGN, "Error en el proceso de firma", t);
		}
	}

	protected abstract void onSigningSuccess(final String signature);

	protected abstract void onSigningError(final KeyStoreOperation op, final String msg, final Throwable t);
}
