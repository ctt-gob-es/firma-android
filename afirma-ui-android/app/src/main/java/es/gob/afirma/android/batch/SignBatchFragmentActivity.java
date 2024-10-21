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
import android.content.Context;
import android.os.Build;
import android.security.KeyChainException;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import es.gob.afirma.R;
import es.gob.afirma.android.LoadKeyStoreFragmentActivity;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.crypto.KeyStoreManagerListener;
import es.gob.afirma.android.crypto.MSCBadPinException;
import es.gob.afirma.android.crypto.MobileKeyStoreManager;
import es.gob.afirma.android.crypto.MobileKeyStoreManager.SelectCertificateEvent;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.android.gui.CustomDialog;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.http.HttpError;
import es.gob.afirma.core.misc.protocol.UrlParametersForBatch;
import es.gob.afirma.signers.cades.CAdESExtraParams;

/** Esta actividad abstracta integra las funciones necesarias para la ejecuci&oacute;n de
 * operaciones de firma por lotes en una actividad. La actividad integra la l&oacute;gica necesaria para
 * utilizar DNIe 3.0 v&iacute;a NFC, DNIe 2.0/3.0 a trav&eacute;s de lector de tarjetas y el
 * almac&eacute;n de Android. */
public abstract class SignBatchFragmentActivity extends LoadKeyStoreFragmentActivity
											implements KeyStoreManagerListener, MobileKeyStoreManager.PrivateKeySelectionListener,
														SignBatchTask.SignBatchListener {

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	public static final String SIGN_TYPE_BATCH = "BATCH";

	private UrlParametersForBatch batchParams;
	private PrivateKeyEntry pke;

	/**
	 * Inicia el proceso de firma.
	 * @param batchParams Firma de lotes a realizar
     */
	public void sign(final UrlParametersForBatch batchParams) {



		if (batchParams == null) {
			throw new IllegalArgumentException("No se han indicado el lote de firmas que procesar");
		}

		this.batchParams = batchParams;
		this.ksmListener = this;

		// Indicamos que las claves que se carguen no se usaran para autenticacion
		setOnlyAuthenticationOperation(false);

		// Iniciamos la carga del almacen
		loadKeyStore(this);
	}

	@Override
	public synchronized void keySelected(final SelectCertificateEvent kse) {

		X509Certificate cert;

		try {
			pke = kse.getPrivateKeyEntry();
			cert = (X509Certificate) pke.getCertificate();
			cert.checkValidity();
		}
		catch (final CertificateExpiredException e) {
			Logger.e(ES_GOB_AFIRMA, "El certificado seleccionado esta caducado: " + e); //$NON-NLS-1$
			PrivateKeyEntry finalPke = pke;
			SignBatchFragmentActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					CustomDialog cd = new CustomDialog(SignBatchFragmentActivity.this, R.drawable.baseline_info_24, getString(R.string.expired_cert),
							getString(R.string.not_valid_cert), getString(R.string.drag_on), true, getString(R.string.cancel_underline));
					CustomDialog finalCd = cd;
					cd.setAcceptButtonClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							finalCd.cancel();
							String providerName = null;
							if (kse.getKeyStore() != null) {
								providerName = kse.getKeyStore().getProvider().getName();
							}
							startDoSign(kse, finalPke, providerName, false);
						}
					});
					cd.setCancelButtonClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Logger.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado: " + e); //$NON-NLS-1$
							onSigningError(KeyStoreOperation.SELECT_CERTIFICATE, "El usuario no selecciono un certificado", new PendingIntent.CanceledException(new AOCancelledOperationException("Operacion cancelada")));
						}
					});
					cd.show();
				}
			});
			return;
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

		startDoSign(kse, pke, providerName, false);

	}

	private void startDoSign(final SelectCertificateEvent kse, final PrivateKeyEntry keyEntry, final String providerName, final boolean pseudonymChecked) {

		X509Certificate cert = (X509Certificate) pke.getCertificate();

		Context ctx = this;

		// Comprobamos si es un certificado de seudonimo
		if (cert != null && !pseudonymChecked && AOUtil.isPseudonymCert(cert)) {
			PrivateKeyEntry finalPke = pke;

			CustomDialog signFragmentCustomDialog = new CustomDialog(ctx, R.drawable.baseline_info_24, getString(R.string.pseudonym_cert),
					getString(R.string.pseudonym_cert_desc), getString(R.string.ok), true, getString(R.string.change_cert));
			signFragmentCustomDialog.setAcceptButtonClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					signFragmentCustomDialog.cancel();
					startDoSign(kse, finalPke, providerName, true);
				}
			});
			signFragmentCustomDialog.setCancelButtonClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					signFragmentCustomDialog.cancel();
					Properties extraParams = new Properties();
					extraParams.setProperty(CAdESExtraParams.MODE, "implicit");
					sign(batchParams);
				}
			});
			signFragmentCustomDialog.show();
			return;
		}

		try {
			doSign(keyEntry, providerName);
		}
		catch (final Exception e) {
			onSigningError(KeyStoreOperation.SIGN, "Error durante la operacion de firma de lote", e);
		}
	}

	private void doSign(final PrivateKeyEntry keyEntry, String providerName) {

		if (keyEntry == null) {
			onSigningError(KeyStoreOperation.SIGN, "No se pudo extraer la clave privada del certificado", new Exception());
			return;
		}

		Properties pkcs1ExtraParams = null;
		if (providerName != null) {
			pkcs1ExtraParams = new Properties();
			pkcs1ExtraParams.setProperty("Provider." + keyEntry.getPrivateKey().getClass().getName(), providerName);
		}

		new SignBatchTask(
			keyEntry,
			this.batchParams,
			pkcs1ExtraParams,
			this
		).execute();
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
	public void onSignSuccess(final byte[] batchResult) {
		onSigningSuccess(batchResult);
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

	protected abstract void onSigningSuccess(final byte[] batchResult);

	protected abstract void onSigningError(final KeyStoreOperation op, final String msg, final Throwable t);

	/**
	 * Registra en un archivo datos sobre una firma que se haya realizado.
	 * @param signType Tipo de firma: local, web o de lotes.
	 * @param fileInfo Nombre de archivo, dominio o aplicaci;oacute;n desde la que se realiza la firma.
	 */
	protected void saveSignRecord(String signType, String fileInfo) {
		File directory = getFilesDir();
		String signsRecordFileName = "signsRecord.txt";
		File signRecordFile = new File(directory, signsRecordFileName);
		if (!signRecordFile.exists()) {
			try {
				signRecordFile.createNewFile();
			} catch (IOException e) {
				Logger.e(ES_GOB_AFIRMA, "Error al crear archivo para registrar firmas.", e); //$NON-NLS-1$
				return;
			}
		}
		try (FileOutputStream fileos = new FileOutputStream(signRecordFile, true)) {
			PrintWriter pw = new PrintWriter(fileos, true);
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			StringBuilder sb = new StringBuilder(sdf.format(new Date()));
			sb.append(";");
			sb.append(signType);
			sb.append(";");
			sb.append(fileInfo);
			sb.append(";");
			sb.append("sign");
			sb.append("\n");
			pw.write(sb.toString());
			pw.close();
		} catch (IOException e) {
			Logger.e(ES_GOB_AFIRMA, "Error al registrar firma.", e); //$NON-NLS-1$
		}
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
}
