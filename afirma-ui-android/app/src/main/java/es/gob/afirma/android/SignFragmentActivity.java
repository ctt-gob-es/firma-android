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
import android.content.Context;
import android.os.Build;
import android.security.KeyChainException;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.KeyStoreManagerListener;
import es.gob.afirma.android.crypto.MSCBadPinException;
import es.gob.afirma.android.crypto.MobileKeyStoreManager;
import es.gob.afirma.android.crypto.MobileKeyStoreManager.SelectCertificateEvent;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.android.crypto.SignResult;
import es.gob.afirma.android.crypto.SignTask;
import es.gob.afirma.android.crypto.SignTask.SignListener;
import es.gob.afirma.android.errors.AppErrorCode;
import es.gob.afirma.android.gui.CustomDialog;
import es.gob.afirma.android.gui.PDFPasswordDialog;
import es.gob.afirma.android.util.CertificateUtil;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.ErrorCode;
import es.gob.afirma.core.RuntimeConfigNeededException;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.signers.pades.common.BadPdfPasswordException;
import es.gob.afirma.signers.pades.common.PdfExtraParams;
import es.gob.afirma.signers.pades.common.PdfIsPasswordProtectedException;

/** Esta actividad abstracta integra las funciones necesarias para la ejecuci&oacute;n de
 * operaciones de firma en una actividad. La actividad integra la l&oacute;gica necesaria para
 * utilizar DNIe 3.0 v&iacute;a NFC, DNIe 2.0/3.0 a trav&eacute;s de lector de tarjetas y el
 * almac&eacute;n de Android. */
public abstract class SignFragmentActivity	extends LoadKeyStoreFragmentActivity
											implements KeyStoreManagerListener, MobileKeyStoreManager.PrivateKeySelectionListener,
                                                        SignListener {

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$
	public static final String SIGN_TYPE_LOCAL = "LOCAL";
	public static final String SIGN_TYPE_WEB = "WEB";
	public static final String SIGN_TYPE_APP = "APP";

	private String signOperation;
	protected byte[] dataToSign;
	private String format = null;
	private String algorithm = null;
	private Properties extraParams = null;
	boolean signing = false;
	private PrivateKeyEntry keyEntry = null;
	private boolean isPseudonymCert = false;
	private boolean isLocalSign = false;
	private boolean isSticky = false;
	private boolean isResetSticky = false;

	/**
	 * Inicia el proceso de firma.
	 * @param signOperation Operacion de firma (firma, cofirma o multifirma)
	 * @param data Datos a firmar.
	 * @param format Formato de firma.
	 * @param algorithm Algoritmo de firma.
	 * @param isLocalSign Indica si es una firma local o no.
	 * @param isSticky Indica si se debe cachear el certificado seleccionado.
	 * @param isResetSticky Indica si se debe reiniciar la cache del certificado.
	 * @param extraParams Par&aacute;metros
	 */
	public void sign(String signOperation, final byte[] data, final String format,
					 final String algorithm, final boolean isLocalSign, final boolean isSticky,
					 final boolean isResetSticky, final Properties extraParams) {
		this.isSticky = isSticky;
		this.isResetSticky = isResetSticky;
		sign(signOperation, data, format, algorithm, isLocalSign, extraParams);
	}

	/**
	 * Inicia el proceso de firma.
	 * @param signOperation Operacion de firma (firma, cofirma o multifirma)
	 * @param data Datos a firmar.
	 * @param format Formato de firma.
	 * @param algorithm Algoritmo de firma.
	 * @param isLocalSign Indica si es una firma local o no.
     * @param extraParams Par&aacute;metros
     */
	public void sign(String signOperation, final byte[] data, final String format,
						final String algorithm, final boolean isLocalSign, final Properties extraParams) {

		// Indicamos que las claves que se carguen no se usaran para autenticacion
		setOnlyAuthenticationOperation(false);

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
		this.ksmListener = this;

		this.signing = true;
		this.isLocalSign = isLocalSign;

		if (this.isSticky && !this.isResetSticky && KeyEntryCache.getStickyKeyEntry() != null) {
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
					// Usamos una variable final para su uso desde el listener
					final PrivateKeyEntry finalPke = pke;
					SignFragmentActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							showCertExpiringSoonDialog(kse, finalPke);
						}
					});
					return;
				}
			}
		} catch (final CertificateExpiredException e) {
			Logger.e(ES_GOB_AFIRMA, "El certificado seleccionado esta caducado: " + e); //$NON-NLS-1$
			// Usamos una variable final para su uso desde el listener
			final PrivateKeyEntry finalPke = pke;
			SignFragmentActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					showExpiredCertDialog(kse, finalPke);
				}
			});
			return;
		} catch (final KeyChainException e) {
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
			Logger.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado: " + e); //$NON-NLS-1$

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
			onSigningError(KeyStoreOperation.SELECT_CERTIFICATE, new AOException(ErrorCode.Internal.SIGNING_KEY_AUTHENTICATION_ERROR)); //$NON-NLS-1$
			return;
		}

		startDoSign(kse, pke);
	}

	private synchronized void startDoSign(final SelectCertificateEvent kse, final PrivateKeyEntry pke) {

		X509Certificate cert = (X509Certificate) pke.getCertificate();

		this.isPseudonymCert = AOUtil.isPseudonymCert(cert);

		// Comprobamos si es un certificado de seudonimo si asi se indica
		if (this.isPseudonymCert && kse.isPseudonymWarningNeed()) {
			SignFragmentActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showPseudonymCertDialog(kse, pke);
				}
			});

			return;
		}

		if (this.extraParams == null) {
			this.extraParams = new Properties();
		}

		if (kse.getKeyStore() != null) {
			String providerName = kse.getKeyStore().getProvider().getName();
			this.extraParams.setProperty("Provider." + pke.getPrivateKey().getClass().getName(), providerName);
		}

		try {
			doSign(pke);
		}
		catch (final Exception e) {
			onSigningError(KeyStoreOperation.SIGN, e);
		}
	}

	private void doSign(final PrivateKeyEntry pke) {

		if (this.isLocalSign && this.isPseudonymCert && this.extraParams.containsKey(PdfExtraParams.LAYER2_TEXT)) {
			this.extraParams.setProperty(PdfExtraParams.LAYER2_TEXT , getString(R.string.pdf_visible_sign_pseudonym_template));
		}

		this.keyEntry = pke;

		if (this.isSticky && !isDNIeCert) {
			KeyEntryCache.setStickyKeyEntry(this.keyEntry, this);
		} else {
			KeyEntryCache.setStickyKeyEntry(null, this);
		}

		// Seleccionamos el algoritmo de firma
		final String keyType = this.keyEntry.getPrivateKey().getAlgorithm();
		String signatureAlgorithm;
		try {
			signatureAlgorithm = AOSignConstants.composeSignatureAlgorithmName(this.algorithm, keyType);
		} catch (final Exception e) {
			onSigningError(KeyStoreOperation.SIGN, new AOException(ErrorCode.Internal.INVALID_SIGNING_KEY));
			return;
		}

		new SignTask(
			this.signOperation,
			this.dataToSign,
			this.format,
			signatureAlgorithm,
			this.keyEntry,
			this.extraParams,
			this,
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
			onSigningError(KeyStoreOperation.LOAD_KEYSTORE, new PendingIntent.CanceledException("Se cancela la seleccion del almacen"));
			return;
		}
		msm.getPrivateKeyEntryAsynchronously(this);
	}

	@Override
	public void onKeyStoreError(KeyStoreOperation op, Throwable t) {
		this.signing = false;
		onSigningError(op, t);
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
				final PDFPasswordDialog pdfPasswordDialog = new PDFPasswordDialog(this,
				new SignTask(
						this.signOperation,
						this.dataToSign,
						this.format,
						this.algorithm,
						this.keyEntry,
						this.extraParams,
						this,
						this
				),
				this,
				t);

				pdfPasswordDialog.show();
			}
			catch (final Exception e1) {
				// Si falla el mostrar el error (posiblemente por no disponer de un contexto grafico para mostrarlo)
				// se mostrara en un toast
				Toast.makeText(getApplicationContext(), R.string.pdf_password_protected, Toast.LENGTH_SHORT).show();
				this.signing = false;
				onSigningError(KeyStoreOperation.SIGN, t);
			}
		}
		else if (t instanceof MSCBadPinException) {
			// Se reintenta la operacion de lectura de DNI indicando que el PIN es incorrecto
			loadKeyStore(this, t);
		} else {
			this.signing = false;
			onSigningError(KeyStoreOperation.SIGN, t);
		}
	}

	/**
	 * Registra en un archivo datos sobre una firma que se haya realizado.
	 * @param signType Tipo de firma: local, web o de lotes.
	 * @param fileName Nombre de archivo.
	 * @param appName Dominio o aplicaci;oacute;n desde la que se realiza la firma.
	 */
	protected void saveSignRecord(String signType, String fileName, String appName) {
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
			sb.append(this.signOperation);
			sb.append(";");
			sb.append(fileName);
			sb.append(";");
			sb.append(appName);
			sb.append("\n");
			pw.write(sb.toString());
			pw.close();
		} catch (IOException e) {
			Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.CANT_SAVE_SIGN_RECORD.toString(), e);
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
		CustomDialog cd = new CustomDialog(SignFragmentActivity.this, R.drawable.baseline_info_24, getString(title),
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
				sign(signOperation, dataToSign, format, algorithm, isLocalSign, extraParams);
			}
		});
		cd.show();
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(LocaleHelper.onAttach(base));
	}

	protected abstract void onSigningSuccess(final SignResult signature);

	protected abstract void onSigningError(final KeyStoreOperation op, final Throwable t);

}
