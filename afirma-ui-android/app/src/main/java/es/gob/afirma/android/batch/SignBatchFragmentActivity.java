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
import es.gob.afirma.android.KeyEntryCache;
import es.gob.afirma.android.LoadKeyStoreFragmentActivity;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.NfcHelper;
import es.gob.afirma.android.crypto.KeyStoreManagerListener;
import es.gob.afirma.android.crypto.MSCBadPinException;
import es.gob.afirma.android.crypto.MobileKeyStoreManager;
import es.gob.afirma.android.crypto.MobileKeyStoreManager.SelectCertificateEvent;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.android.errors.AppErrorCode;
import es.gob.afirma.android.gui.CustomDialog;
import es.gob.afirma.android.util.CertificateUtil;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.ErrorCode;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.http.HttpError;
import es.gob.afirma.core.misc.protocol.UrlParametersForBatch;

/** Esta actividad abstracta integra las funciones necesarias para la ejecuci&oacute;n de
 * operaciones de firma por lotes en una actividad. La actividad integra la l&oacute;gica necesaria para
 * utilizar DNIe 3.0 v&iacute;a NFC, DNIe 2.0/3.0 a trav&eacute;s de lector de tarjetas y el
 * almac&eacute;n de Android. */
public abstract class SignBatchFragmentActivity extends LoadKeyStoreFragmentActivity
											implements KeyStoreManagerListener, MobileKeyStoreManager.PrivateKeySelectionListener,
														SignBatchTask.SignBatchListener {

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	public static final String SIGN_TYPE_BATCH = "BATCH";
	public static final String SIGN_TYPE_BATCH_APP = "BATCH_APP";

	private UrlParametersForBatch batchParams;
	private PrivateKeyEntry keyEntry = null;

	/**
	 * Inicia el proceso de firma.
	 * @param batchParams Firma de lotes a realizar
     */
	public void sign(final UrlParametersForBatch batchParams) {

		if (batchParams == null) {
			throw new IllegalArgumentException(ErrorCode.Request.DATA_NOT_FOUND.toString());
		}

		this.batchParams = batchParams;
		this.ksmListener = this;

		// Indicamos que las claves que se carguen no se usaran para autenticacion
		setOnlyAuthenticationOperation(false);

		if (this.batchParams.getSticky() && !this.batchParams.getResetSticky() && KeyEntryCache.getStickyKeyEntry() != null) {
			keySelected(new SelectCertificateEvent(KeyEntryCache.getStickyKeyEntry(), false, false));
		} else {
			// Iniciamos la carga del almacen
			loadKeyStore(this, null);
		}
	}

	@Override
	public synchronized void keySelected(final SelectCertificateEvent kse) {

        PrivateKeyEntry pke = null;
		try {
            pke = kse.getPrivateKeyEntry();
            X509Certificate cert = (X509Certificate) pke.getCertificate();
			if (kse.isCertExpirationWarningNeed()) {
				cert.checkValidity();
				boolean expiredSoon = CertificateUtil.checkExpiredSoon(cert);
				if (expiredSoon) {
					Logger.e(ES_GOB_AFIRMA, "El certificado seleccionado esta a punto de caducar"); //$NON-NLS-1$
                    final PrivateKeyEntry finalPke = pke;
					SignBatchFragmentActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							showCertExpiringSoonDialog(kse, finalPke);
						}
					});
					return;
				}
			}
		}
		catch (final CertificateExpiredException e) {
			Logger.w(ES_GOB_AFIRMA, "El certificado seleccionado esta caducado: " + e); //$NON-NLS-1$
            final PrivateKeyEntry finalPke = pke;
			SignBatchFragmentActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					showExpiredCertDialog(kse, finalPke);
				}
			});
			return;
		}
		catch (final KeyChainException e) {
			if ("4.1.1".equals(Build.VERSION.RELEASE) || "4.1.0".equals(Build.VERSION.RELEASE) || "4.1".equals(Build.VERSION.RELEASE)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Logger.e(ES_GOB_AFIRMA, "Error al extraer la clave en Android " + Build.VERSION.RELEASE + ": " + e); //$NON-NLS-1$ //$NON-NLS-2$
				onSigningError(KeyStoreOperation.SELECT_CERTIFICATE, new SelectKeyAndroid41BugException(e));
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "No se pudo extraer la clave privada del certificado: " + e); //$NON-NLS-1$
				onSigningError(KeyStoreOperation.SELECT_CERTIFICATE, e);
			}
			return;
		}
		catch (final AOCancelledOperationException e) {
			Logger.w(ES_GOB_AFIRMA, "El usuario no selecciono un certificado: " + e); //$NON-NLS-1$
			// Si se ha cancelado la operacion y esta disponible el uso de mas de un almacen, permitimos
			// seleccionar almacen. Si no, damos por hecho que el usuario quiere cancelar.
			if (NfcHelper.isNfcPreferredConnection(this)) {
				loadKeyStore(this, null);
			} else {
				onSigningError(KeyStoreOperation.SELECT_CERTIFICATE, new PendingIntent.CanceledException(e));
			}
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
			onSigningError(KeyStoreOperation.SELECT_CERTIFICATE, e); //$NON-NLS-1$
			return;
		}

		startDoSign(kse, pke);

	}

	private void startDoSign(final SelectCertificateEvent kse, final PrivateKeyEntry pke) {

        this.keyEntry = pke;

		X509Certificate cert = (X509Certificate) this.keyEntry.getCertificate();

		// Comprobamos si es un certificado de seudonimo si se ha solicitado
		if (kse.isPseudonymWarningNeed()) {
			if (AOUtil.isPseudonymCert(cert)) {
				SignBatchFragmentActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						showPseudonymCertDialog(kse, SignBatchFragmentActivity.this.keyEntry);
					}
				});
				return;
			}
		}

		String providerName = null;
		if (kse.getKeyStore() != null) {
			providerName = kse.getKeyStore().getProvider().getName();
		}

		try {
			doSign(this.keyEntry, providerName);
		}
		catch (final Exception e) {
			onSigningError(KeyStoreOperation.SIGN, e);
		}
	}

	private void doSign(final PrivateKeyEntry pke, String providerName) {

		if (this.batchParams.getSticky() && !isDNIeCert) {
			KeyEntryCache.setStickyKeyEntry(pke, this);
		} else {
			KeyEntryCache.setStickyKeyEntry(null, this);
		}

		Properties pkcs1ExtraParams = null;
		if (providerName != null) {
			pkcs1ExtraParams = new Properties();
			pkcs1ExtraParams.setProperty("Provider." + pke.getPrivateKey().getClass().getName(), providerName);
		}

        new SignBatchTask(
                pke,
                this.batchParams,
                pkcs1ExtraParams,
                this,
                this
        ).execute();
	}

	@Override
	public synchronized void onLoadingKeyStoreSuccess(final MobileKeyStoreManager msm) {

		// Si el usuario cancelo la insercion de PIN o cualquier otro dialogo del almacen
		if(msm == null){
			onSigningError(KeyStoreOperation.LOAD_KEYSTORE, new PendingIntent.CanceledException("Se cancela la seleccion del almacen"));
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
			onSigningError(KeyStoreOperation.SIGN, t);
		}
		else if (t instanceof IllegalArgumentException) {
			onSigningError(KeyStoreOperation.SIGN, t);
		}
		else if (t instanceof CertificateEncodingException) {
			onSigningError(KeyStoreOperation.SIGN, t);
		}
		else if (t instanceof HttpError) {
			onSigningError(KeyStoreOperation.SIGN, t);
		}
		else if (t instanceof MSCBadPinException) {
			// Se reintenta la operacion de lectura de DNI indicando que el PIN es incorrecto
			loadKeyStore(this, t);
		}
		else if (t instanceof AOException) {
			onSigningError(KeyStoreOperation.SIGN, t);
		} else {
			// Se introduce este error por si llegara alguno no controlado
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.GENERAL_ERROR.toString(), t); //$NON-NLS-1$
			onSigningError(KeyStoreOperation.SIGN, t);
		}
	}

	protected abstract void onSigningSuccess(final byte[] batchResult);

	protected abstract void onSigningError(final KeyStoreOperation op, final Throwable t);

	/**
	 * Registra en un archivo datos sobre una firma que se haya realizado.
	 * @param signType Tipo de firma: local, web o de lotes.
	 * @param appName Nombre de archivo, dominio o aplicaci;oacute;n desde la que se realiza la firma.
	 */
	protected void saveSignRecord(String signType, String appName) {
		File directory = getFilesDir();
		String signsRecordFileName = "signsRecord.txt";
		File signRecordFile = new File(directory, signsRecordFileName);
		if (!signRecordFile.exists()) {
			try {
				signRecordFile.createNewFile();
			} catch (IOException e) {
				Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.CANT_SAVE_SIGN_RECORD.toString(), e);
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
			sb.append("sign");
			sb.append(";");
			// El nombre de archivo es null, ya que es una firma por lotes
			sb.append("null");
			sb.append(";");
			sb.append(appName);
			sb.append("\n");
			pw.write(sb.toString());
			pw.close();
		} catch (IOException e) {
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.CANT_SAVE_SIGN_RECORD.toString(), e); //$NON-NLS-1$
		}
	}

	private void showCertExpiringSoonDialog(SelectCertificateEvent kse, PrivateKeyEntry pke) {
		showCertWarningDialog(R.string.expired_cert_soon, R.string.expired_cert_soon_desc,
				new SelectCertificateEvent(kse, kse.isPseudonymWarningNeed(), false), pke);
	}

	private void showExpiredCertDialog(SelectCertificateEvent kse, PrivateKeyEntry pke) {
		showCertWarningDialog(R.string.expired_cert, R.string.not_valid_cert,
				new SelectCertificateEvent(kse, kse.isPseudonymWarningNeed(), false), pke);
	}

	private void showPseudonymCertDialog(SelectCertificateEvent kse, PrivateKeyEntry pke) {
		showCertWarningDialog(R.string.pseudonym_cert, R.string.pseudonym_cert_desc,
				new SelectCertificateEvent(kse, false, kse.isCertExpirationWarningNeed()), pke);
	}

	private void showCertWarningDialog(int title, int text, SelectCertificateEvent kse, PrivateKeyEntry pke) {
		CustomDialog cd = new CustomDialog(SignBatchFragmentActivity.this, R.drawable.baseline_info_24, getString(title),
				getString(text), getString(R.string.drag_on), true, getString(R.string.cancel_underline));
		cd.setAcceptButtonClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cd.cancel();
				startDoSign(kse, pke);
			}
		});
		cd.setCancelButtonClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				cd.cancel();
				Logger.i(ES_GOB_AFIRMA, "El usuario cancela el uso del certificado debido a una advertencia y se le ofrecera usar otro"); //$NON-NLS-1$
				sign(batchParams);
			}
		});
		cd.show();
	}

	protected PrivateKeyEntry getKeyEntry() {
		return this.keyEntry;
	}

	protected UrlParametersForBatch getBatchParams() {
		return this.batchParams;
	}

	protected void setBatchParams(UrlParametersForBatch batchParams) {
		this.batchParams = batchParams;
	}
}
