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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.security.KeyChainException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import java.security.KeyStore.PrivateKeyEntry;
import java.util.HashMap;
import java.util.Properties;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.InitializingNfcCardException;
import es.gob.afirma.android.crypto.KeyStoreManagerListener;
import es.gob.afirma.android.crypto.LoadKeyStoreManagerTask;
import es.gob.afirma.android.crypto.LoadNfcKeyStoreManagerTask;
import es.gob.afirma.android.crypto.LoadingCertificateException;
import es.gob.afirma.android.crypto.MobileKeyStoreManager;
import es.gob.afirma.android.crypto.MobileKeyStoreManager.KeySelectedEvent;
import es.gob.afirma.android.crypto.MobileKeyStoreManager.PrivateKeySelectionListener;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.android.crypto.SignTask;
import es.gob.afirma.android.crypto.SignTask.SignListener;
import es.gob.afirma.android.crypto.UnsupportedNfcCardException;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.misc.protocol.UrlParametersToSign.Operation;
import es.gob.jmulticard.android.callbacks.CachePasswordCallback;

/** Esta actividad abstracta integra las funciones necesarias para la ejecuci&oacute;n de
 * operaciones de firma en una actividad. La actividad integra la l&oacute;gica necesaria para
 * utilizar DNIe 3.0 v&iacute;a NFC, DNIe 2.0/3.0 a trav&eacute;s de lector de tarjetas y el
 * almac&eacute;n de Android. */
public abstract class SignFragmentActivity extends FragmentActivity implements KeyStoreManagerListener,
		                                                                       PrivateKeySelectionListener,
		                                                                       SignListener {

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	/** C&oacute;digo de solicitud de detecci&oacute;n de tarjeta por NFC. */
	private final static int REQUEST_CODE_DETECT_NFC_CARD = 2001;
	/** C&oacute;digo de solicitud de la habilitaci&oacute;n del NFC del dispositivo. */
	private final static int REQUEST_CODE_ENABLE_NFC = 2002;   // The request code

	private Operation signOperation;
	private byte[] dataToSign;
	private String format = null;
	private String algorithm = null;
	private Properties extraParams = null;

	private CachePasswordCallback passwordCallback = null;

	private UsbManager usbManager = null;
	UsbManager getUsbManager() {
		return this.usbManager;
	}
	void setUsbManager(final UsbManager usbMgr) {
		this.usbManager = usbMgr;
	}

	private UsbDevice usbDevice = null;
	UsbDevice getUsbDevice() {
		return this.usbDevice;
	}
	void setUsbDevice(final UsbDevice usbDev) {
		this.usbDevice = usbDev;
	}

	private static final String ACTION_USB_PERMISSION = "es.gob.afirma.android.USB_PERMISSION"; //$NON-NLS-1$

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {

					Log.d(ES_GOB_AFIRMA, "Comprobamos el permiso de acceso al lector USB"); //$NON-NLS-1$

					// Si no se concedio el permiso
					if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						SignFragmentActivity.this.setUsbManager(null);
						SignFragmentActivity.this.setUsbDevice(null);
						Log.d(ES_GOB_AFIRMA, "Permiso denegado para el acceso a USB"); //$NON-NLS-1$
					}

					// Ya sea con dispositivo o sin el, se continua la ejecucion cargando el almacen
					new LoadKeyStoreManagerTask(
							SignFragmentActivity.this,
							SignFragmentActivity.this,
							SignFragmentActivity.this.getUsbDevice(),
							SignFragmentActivity.this.getUsbManager()
					).execute();
				}
			}
			else {
				new LoadKeyStoreManagerTask(
						SignFragmentActivity.this,
						SignFragmentActivity.this
				).execute();
			}
		}
	};

	private void askForUsbPermission(){
		final PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(this.mUsbReceiver, filter);
		this.usbManager.requestPermission(this.usbDevice, mPermissionIntent);
	}

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

		Log.i(ES_GOB_AFIRMA, " -- SignFragmentActivity sign");

		if (signOperation == null) {
			throw new IllegalArgumentException("No se han indicado la operacion de firma");
		}
		try {
			this.signOperation = Operation.valueOf(signOperation);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(String.format(
							"Operacion de firma no valida. Debe ser: %1s, %2s o %3s.",
							Operation.SIGN.toString(), Operation.COSIGN.toString(), Operation.COUNTERSIGN.toString()
					));
		}
		if (data == null) {
			throw new IllegalArgumentException("No se han indicado los datos a firmar");
		}
		if (format == null) {
			throw new IllegalArgumentException("No se han indicado los datos a firmar");
		}
		if (algorithm == null) {
			throw new IllegalArgumentException("No se han indicado los datos a firmar");
		}

		this.dataToSign = data;
		this.format = format;
		this.algorithm = algorithm;
		this.extraParams = extraParams;

		// Si tenemos habilitado el uso de NFC, buscamos una tarjeta; si no, cargamos directamente
		// el almacen en cuestion (que puede ser una tarjeta previamente buscada)
		if (NfcHelper.isNfcPreferredConnection(this)) {

			// Comprobamos si se configuro el uso de NFC
			// Si el NFC esta activado, lanzamos una actividad para detectar el DNIe 3.0
			if (NfcHelper.isNfcServiceEnabled(this)) {
				searchNewNfcCard();
			}
			// Si el NFC no esta activado, se le solicita al usuario activarlo
			else {
				openNfcSystemSettings();
			}
		}
		else {
			loadKeyStore();
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

		Log.i(ES_GOB_AFIRMA, " -- SignFragmentActivity onActivityResult");

		// Si volvemos de la pantalla de insercion de CAN y deteccion de tarjeta NFC
		if (requestCode == REQUEST_CODE_DETECT_NFC_CARD) {

			Log.i(ES_GOB_AFIRMA, "-- SignFragmentActivity onActivityResult  - Respuesta de actividad NFC");

			// Si el usuario inserto el CAN, lo guardamos y cargamos un almacen via NFC (que tendra
			// una gestion de errores distinta).
			// Si no se inserto el CAN o no se detecto un almacen nfc, nos aseguramos de que no
			// haya ningun CAN cacheado y cargamos el resto de almacenes.
			if (resultCode == RESULT_OK) {
				this.passwordCallback = data != null ? (CachePasswordCallback)data.getSerializableExtra("pc") : null;
				loadNfcKeyStore();
			}
			else {
				runOnUiThread(
						new Runnable() {
							@Override
							public void run() {
								Toast.makeText(SignFragmentActivity.this, R.string.nfc_cancelled, Toast.LENGTH_LONG).show();
							}
						}
				);
				if (passwordCallback != null) {
					this.passwordCallback.clearPassword();
					this.passwordCallback = null;
				}
				loadKeyStore();
			}
		}
		// Si volvemos despues de pedirle al usuario que habilite el NFC
		else if (requestCode == REQUEST_CODE_ENABLE_NFC) {
			// Si el usuario habilito el NFC, intentaremos leer una tarjeta NFC
			// Si no, cargamos directamente los certificados
			if(NfcHelper.isNfcServiceEnabled(this)) {
				final Intent intentNFC = new Intent(this, NFCDetectorActivity.class);
				startActivityForResult(intentNFC, REQUEST_CODE_DETECT_NFC_CARD);
			}
			else {
				runOnUiThread(
						new Runnable() {
							@Override
							public void run() {
								Toast.makeText(SignFragmentActivity.this, R.string.nfc_no_detected, Toast.LENGTH_LONG).show();
							}
						}
				);
				loadKeyStore();
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}


	/**
	 * Busca una nueva tarjeta NFC. El resultado de la busqueda se obtiene en el
	 * metodo onActivityResult().
	 */
	public void searchNewNfcCard() {

		Log.i(ES_GOB_AFIRMA, " -- SignFragmentActivity searchNewNfcCard");

		final Intent intentNFC = new Intent(this, NFCDetectorActivity.class);
		if (this.passwordCallback != null) {
			intentNFC.putExtra(NFCDetectorActivity.INTENT_EXTRA_CAN_VALUE, this.passwordCallback.getPassword());
		}
		startActivityForResult(intentNFC, REQUEST_CODE_DETECT_NFC_CARD);
	}

	/**
	 * Abre el dialogo del sistema para la configuracion de NFC. El resultado de si se ha
	 * activado o no se determina en el onActivityResult.
	 */
	private void openNfcSystemSettings() {
		Toast.makeText(getApplicationContext(), R.string.enable_nfc, Toast.LENGTH_LONG).show();
		startActivityForResult(
				new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS),
				REQUEST_CODE_ENABLE_NFC);
	}

	/**
	 * Inicia el proceso de carga de certificados para firmar.
	 */
	public void loadKeyStore() {

		Log.i(ES_GOB_AFIRMA, " -- SignFragmentActivity loadKeyStore");

		// Buscamos si hay dispositivos CCID USB conectados
		final UsbManager usbMgr = (UsbManager) getSystemService(Context.USB_SERVICE);
		final HashMap<String, UsbDevice> devices = usbMgr.getDeviceList();
		for (final UsbDevice dev : devices.values()) {
			if (dev.getDeviceClass() == 0 && dev.getInterface(0).getInterfaceClass() == UsbConstants.USB_CLASS_CSCID) {
				this.usbManager = usbMgr;
				this.usbDevice = dev;
				break;
			}
		}

		// Si es igual a null es que no hay un CCID conectado
		if (this.usbManager == null) {
			Log.i(ES_GOB_AFIRMA, "No hay dispositivos CCID USB conectados"); //$NON-NLS-1$
			// Cargamos el almacen de certificados normalmente
			new LoadKeyStoreManagerTask(this, this).execute();
		}

		//Si no, pedimos acceso al dispositivo
		else {
			Log.i(ES_GOB_AFIRMA, "Se han detectado dispositivos CCID USB conectados"); //$NON-NLS-1$
			askForUsbPermission();
		}
	}

	/**
	 * Inicia el proceso de carga de certificados para firmar usando un almacen
	 * por conexion NFC.
	 */
	public void loadNfcKeyStore() {

		Log.i(ES_GOB_AFIRMA, " -- SignFragmentActivity loadNfcKeyStore");

		new LoadNfcKeyStoreManagerTask(this, this, this.passwordCallback).execute();
	}

	@Override
	public synchronized void keySelected(final KeySelectedEvent kse) {

		Log.i(ES_GOB_AFIRMA, " -- SignFragmentActivity keySelected");

		PrivateKeyEntry pke;
		try {
			pke = kse.getPrivateKeyEntry();
		}
		catch (final KeyChainException e) {
			if ("4.1.1".equals(Build.VERSION.RELEASE) || "4.1.0".equals(Build.VERSION.RELEASE) || "4.1".equals(Build.VERSION.RELEASE)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Log.e(ES_GOB_AFIRMA, "Error al extraer la clave en Android " + Build.VERSION.RELEASE + ": " + e); //$NON-NLS-1$ //$NON-NLS-2$
				onSigningError(SigningSubOperation.SELECT_CERTIFICATE, getString(R.string.error_android_4_1), new SelectKeyAndroid41BugException(e));
			}
			else {
				Log.e(ES_GOB_AFIRMA, "No se pudo extraer la clave privada del certificado: " + e); //$NON-NLS-1$
				onSigningError(SigningSubOperation.SELECT_CERTIFICATE, "No se pudo extraer la clave privada del certificado", e);
			}
			return;
		}
		catch (final AOCancelledOperationException e) {
			Log.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado: " + e); //$NON-NLS-1$
			onSigningError(SigningSubOperation.SELECT_CERTIFICATE, "El usuario no selecciono un certificado", new PendingIntent.CanceledException(e));
			return;
		}
		// Cuando se instala el certificado desde el dialogo de seleccion, Android da a elegir certificado
		// en 2 ocasiones y en la segunda se produce un "java.lang.AssertionError". Se ignorara este error.
		catch (final java.lang.AssertionError e) {
			Log.e(ES_GOB_AFIRMA, "Posible error al insertar un nuevo certificado en el almacen. No se hara nada", e); //$NON-NLS-1$
			return;
		}
		catch (final Throwable e) {
			Log.e(ES_GOB_AFIRMA, "Error al recuperar la clave del certificado de firma", e); //$NON-NLS-1$
			onSigningError(SigningSubOperation.SELECT_CERTIFICATE, "Error al recuperar la clave del certificado de firma", e); //$NON-NLS-1$
			return;
		}


		Log.i(ES_GOB_AFIRMA, "================ Borramos el CAN");

		// Ya cargado el certificado, eliminamos el CAN de memoria y el objeto para que se vuelva a pedir
		if (this.passwordCallback != null) {
			this.passwordCallback.clearPassword();
			this.passwordCallback = null;
		}

		try {
			doSign(pke);
		}
		catch (final Exception e) {
			onSigningError(SigningSubOperation.SIGN, "Error durante la operacion de firma", e);
		}
	}

	@Override
	public synchronized void onLoadingKeyStoreSuccess(final MobileKeyStoreManager msm) {

		Log.i(ES_GOB_AFIRMA, " -- SignFragmentActivity onLoadingKeystoreSuccess");

		// Si el usuario cancelo la insercion de PIN o cualquier otro dialogo del almacen
		if(msm == null){
			onSigningError(SigningSubOperation.LOAD_KEYSTORE, "El usuario cancelo la operacion durante la carga del almacen", new PendingIntent.CanceledException("Se cancela la seleccion del almacen"));
			return;
		}
		msm.getPrivateKeyEntryAsynchronously(this);
	}

	@Override
	public void onLoadingKeyStoreError(final String msg, final Throwable t) {

		// Si el error de carga es un error de conexion NFC, volvemos a mostrar el dialogo,
		// si no, indicamos un error en la firma.
		if (t instanceof UnsupportedNfcCardException) {
			Toast.makeText(this, R.string.unsupported_card, Toast.LENGTH_SHORT).show();
			searchNewNfcCard();
		}
		else if (t instanceof InitializingNfcCardException) {
			Toast.makeText(this, R.string.nfc_card_initializing_error, Toast.LENGTH_SHORT).show();
			searchNewNfcCard();
		}
		else if (t instanceof LoadingCertificateException) {
			loadKeyStore();
		}
		else {
			onSigningError(SigningSubOperation.LOAD_KEYSTORE, msg, t);
		}
	}


	private void doSign(final PrivateKeyEntry keyEntry) {

		Log.i(ES_GOB_AFIRMA, " -- SignFragmentActivity doSign");

		if (keyEntry == null) {
			onSigningError(SigningSubOperation.SIGN, "No se pudo extraer la clave privada del certificado", new Exception());
			return;
		}
		new SignTask(
			this,
			this.signOperation,
			this.dataToSign,
			this.format,
			this.algorithm,
			keyEntry,
			this.extraParams,
			this
		).execute();
	}

	@Override
	public void onSignSuccess(final byte[] signature) {
		onSigningSuccess(signature);
	}

	protected abstract void onSigningSuccess(final byte[] signature);

	@Override
	public void onSignError(final Throwable t) {
		onSigningError(SigningSubOperation.SIGN, "Error en el proceso de firma", t);
	}

	protected abstract void onSigningError(final SigningSubOperation op, final String msg, final Throwable t);

	/** Operaci&oacute;n de firma. */
	protected enum SigningSubOperation {
		/** Operaci&oacute;n de firma. */
		SIGN,
		/** Operaci&oacute;n de carga de almac&eacute;n. */
		LOAD_KEYSTORE,
		/** Operacion de seleccion de certificado. */
		SELECT_CERTIFICATE
	}
}
