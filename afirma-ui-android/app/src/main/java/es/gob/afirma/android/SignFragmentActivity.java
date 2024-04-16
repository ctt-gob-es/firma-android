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

import android.app.PendingIntent;
import android.os.Build;
import android.security.KeyChainException;
import android.widget.Toast;

import java.security.KeyStore.PrivateKeyEntry;
import java.util.Locale;
import java.util.Properties;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.MobileKeyStoreManager;
import es.gob.afirma.android.crypto.MobileKeyStoreManager.SelectCertificateEvent;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.android.crypto.SignResult;
import es.gob.afirma.android.crypto.SignTask;
import es.gob.afirma.android.crypto.SignTask.SignListener;
import es.gob.afirma.android.gui.PDFPasswordDialog;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.RuntimeConfigNeededException;
import es.gob.afirma.signers.pades.common.BadPdfPasswordException;
import es.gob.afirma.signers.pades.common.PdfIsPasswordProtectedException;

/** Esta actividad abstracta integra las funciones necesarias para la ejecuci&oacute;n de
 * operaciones de firma en una actividad. La actividad integra la l&oacute;gica necesaria para
 * utilizar DNIe 3.0 v&iacute;a NFC, DNIe 2.0/3.0 a trav&eacute;s de lector de tarjetas y el
 * almac&eacute;n de Android. */
public abstract class SignFragmentActivity	extends LoadKeyStoreFragmentActivity
											implements  MobileKeyStoreManager.PrivateKeySelectionListener,
                                                        SignListener {

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private String signOperation;
	private byte[] dataToSign;
	private String format = null;
	private String algorithm = null;
	private Properties extraParams = null;

	private boolean signing = false;

	private PrivateKeyEntry keyEntry = null;

	/**
	 * Inicia el proceso de firma.
	 * @param signOperation Operacion de firma (firma, cofirma o multifirma)
	 * @param data Datos a firmar.
	 * @param format Formato de firma.
	 * @param algorithm Algoritmo de firma.
     * @param extraParams Par&aacute;metros
     */
	public void sign(String signOperation, final byte[] data, final String format,
						final String algorithm, final Properties extraParams) {

		if (signOperation == null) {
			throw new IllegalArgumentException("No se han indicado la operacion de firma");
		}
		if (SignTask.OP_SIGN.equalsIgnoreCase(signOperation) ||
				SignTask.OP_COSIGN.equalsIgnoreCase(signOperation) ||
				SignTask.OP_COUNTERSIGN.equalsIgnoreCase(signOperation)) {
			this.signOperation = signOperation.toLowerCase(Locale.ENGLISH);
		}
		else {
			throw new IllegalArgumentException(String.format(
							"Operacion de firma no valida. Debe ser: %1s, %2s o %3s.",
							SignTask.OP_SIGN, SignTask.OP_COSIGN, SignTask.OP_COUNTERSIGN
					));
		}
		if (data == null) {
			throw new IllegalArgumentException("No se han indicado los datos a firmar");
		}
		if (format == null) {
			throw new IllegalArgumentException("No se ha indicado el formato de firma");
		}
		if (algorithm == null) {
			throw new IllegalArgumentException("No se han indicado el algoritmo de firma");
		}

		this.dataToSign = data;
		this.format = format;
		this.algorithm = algorithm;
		this.extraParams = extraParams;

		this.signing = true;

		// Iniciamos la carga del almacen
		loadKeyStore(this);
	}

	@Override
	public synchronized void keySelected(final SelectCertificateEvent kse) {

		PrivateKeyEntry pke;
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

		String providerName = null;
		if (kse.getKeyStore() != null) {
			providerName = kse.getKeyStore().getProvider().getName();
		}

		try {
			doSign(pke, providerName);
		}
		catch (final Exception e) {
			onSigningError(KeyStoreOperation.SIGN, "Error durante la operacion de firma", e);
		}
	}

	private void doSign(final PrivateKeyEntry keyEntry, String providerName) {

		if (keyEntry == null) {
			onSigningError(KeyStoreOperation.SIGN, "No se pudo extraer la clave privada del certificado", new Exception());
			return;
		}
		if (providerName != null) {
			if (this.extraParams == null) {
				this.extraParams = new Properties();
			}
			this.extraParams.setProperty("Provider." + keyEntry.getPrivateKey().getClass().getName(), providerName);
		}

		this.keyEntry = keyEntry;

		new SignTask(
			this.signOperation,
			this.dataToSign,
			this.format,
			this.algorithm,
			this.keyEntry,
			this.extraParams,
			this
		).execute();
	}

	protected boolean isSigning() {
		return this.signing;
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
	public void onKeyStoreError(KeyStoreOperation op, String msg, Throwable t) {
		this.signing = false;
		onSigningError(op, msg, t);
	}

	@Override
	public void onSignSuccess(final SignResult signature) {
		this.signing = false;
		onSigningSuccess(signature);
	}

	@Override
	public void onSignError(final Throwable t) {
		if (t instanceof PdfIsPasswordProtectedException && ((PdfIsPasswordProtectedException) t).getRequestType() == RuntimeConfigNeededException.RequestType.PASSWORD
			|| t instanceof BadPdfPasswordException && ((BadPdfPasswordException) t).getRequestType() == RuntimeConfigNeededException.RequestType.PASSWORD) {
			// Este error se da cuando el PDF esta protegido o se ha introducido de manera erronea, por lo que se pedira la contrasena al usuario
			try {
				final PDFPasswordDialog pdfPasswordDialog = new PDFPasswordDialog(new SignTask(
						this.signOperation,
						this.dataToSign,
						this.format,
						this.algorithm,
						this.keyEntry,
						this.extraParams,
						this
				),
				this,
				t);

				pdfPasswordDialog.show(this.getSupportFragmentManager(),
						"PasswordDialog");
			}
			catch (final Exception e1) {
				// Si falla el mostrar el error (posiblemente por no disponer de un contexto grafico para mostrarlo)
				// se mostrara en un toast
				Toast.makeText(getApplicationContext(), "El PDF esta protegido con contrasena", Toast.LENGTH_LONG).show();
			}
		}
		else {
			this.signing = false;
			onSigningError(KeyStoreOperation.SIGN, "Error en el proceso de firma", t);
		}
	}

	protected abstract void onSigningSuccess(final SignResult signature);

	protected abstract void onSigningError(final KeyStoreOperation op, final String msg, final Throwable t);
}
