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
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import java.util.HashMap;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.InitializingNfcCardException;
import es.gob.afirma.android.crypto.KeyStoreManagerListener;
import es.gob.afirma.android.crypto.LoadKeyStoreManagerTask;
import es.gob.afirma.android.crypto.LoadNfcKeyStoreManagerTask;
import es.gob.afirma.android.crypto.LoadingCertificateException;
import es.gob.afirma.android.crypto.UnsupportedNfcCardException;
import es.gob.jmulticard.android.callbacks.CachePasswordCallback;

/** Esta actividad abstracta integra las funciones necesarias para la cargar de un almacen de
 * certificados del dispositivo. La actividad integra la l&oacute;gica necesaria para utilizar
 * DNIe 3.0 v&iacute;a NFC, DNIe 2.0/3.0 a trav&eacute;s de lector de tarjetas y el almac&eacute;n
 * de Android. */
public abstract class LoadKeyStoreFragmentActivity extends FragmentActivity
                                                   implements  KeyStoreManagerListener {

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	/** C&oacute;digo de solicitud de detecci&oacute;n de tarjeta por NFC. */
	private final static int REQUEST_CODE_DETECT_NFC_CARD = 2001;
	/** C&oacute;digo de solicitud de la habilitaci&oacute;n del NFC del dispositivo. */
	private final static int REQUEST_CODE_ENABLE_NFC = 2002;   // The request code

    private static final String ACTION_USB_PERMISSION = "es.gob.afirma.android.USB_PERMISSION"; //$NON-NLS-1$

    private CachePasswordCallback passwordCallback = null;

	private UsbManager usbManager = null;

    private UsbDevice usbDevice = null;

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {

					Logger.d(ES_GOB_AFIRMA, "Comprobamos el permiso de acceso al lector USB"); //$NON-NLS-1$

					// Si no se concedio el permiso
					if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						LoadKeyStoreFragmentActivity.this.setUsbManager(null);
						LoadKeyStoreFragmentActivity.this.setUsbDevice(null);
						Logger.d(ES_GOB_AFIRMA, "Permiso denegado para el acceso a USB"); //$NON-NLS-1$
					}

					// Ya sea con dispositivo o sin el, se continua la ejecucion cargando el almacen
					new LoadKeyStoreManagerTask(
							LoadKeyStoreFragmentActivity.this,
							LoadKeyStoreFragmentActivity.this,
							LoadKeyStoreFragmentActivity.this.getUsbDevice(),
							LoadKeyStoreFragmentActivity.this.getUsbManager()
					).execute();
				}
			}
			else {
				new LoadKeyStoreManagerTask(
						LoadKeyStoreFragmentActivity.this,
						LoadKeyStoreFragmentActivity.this
				).execute();
			}
		}
	};

    protected void setPasswordCallback(CachePasswordCallback passwordCallback) {
        this.passwordCallback = passwordCallback;
    }
    protected CachePasswordCallback getPasswordCallback() {
        return passwordCallback;
    }
    UsbManager getUsbManager() {
        return this.usbManager;
    }
    void setUsbManager(final UsbManager usbMgr) {
        this.usbManager = usbMgr;
    }
    UsbDevice getUsbDevice() {
        return this.usbDevice;
    }
    void setUsbDevice(final UsbDevice usbDev) {
        this.usbDevice = usbDev;
    }

	private void askForUsbPermission(){
		final PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(this.mUsbReceiver, filter);
		this.usbManager.requestPermission(this.usbDevice, mPermissionIntent);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

		// Si volvemos de la pantalla de insercion de CAN y deteccion de tarjeta NFC
		if (requestCode == REQUEST_CODE_DETECT_NFC_CARD) {
			// Si el usuario inserto el CAN, lo guardamos y cargamos un almacen via NFC (que tendra
			// una gestion de errores distinta).
			// Si no se inserto el CAN o no se detecto un almacen nfc, nos aseguramos de que no
			// haya ningun CAN cacheado y cargamos el resto de almacenes.
			if (resultCode == RESULT_OK) {
				this.passwordCallback = data != null ? (CachePasswordCallback) data.getSerializableExtra("pc") : null;
				loadNfcKeyStore();
			}
			else {
				runOnUiThread(
						new Runnable() {
							@Override
							public void run() {
								Toast.makeText(LoadKeyStoreFragmentActivity.this, R.string.nfc_cancelled, Toast.LENGTH_LONG).show();
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
								Toast.makeText(LoadKeyStoreFragmentActivity.this, R.string.nfc_no_detected, Toast.LENGTH_LONG).show();
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
	 * Carga el almac&eacute;n de certificados (que podr&oacute;a ser una tarjeta inteligente
	 * compatible situada al alcance del NFC).
	 * @param context Contexto de la actividad.
	 */
	protected void loadKeyStore(Context context) {
		// Si tenemos habilitado el uso de NFC, buscamos una tarjeta; si no, cargamos directamente
		// el almacen en cuestion (que puede ser una tarjeta previamente buscada)
		if (NfcHelper.isNfcPreferredConnection(context)) {

			// Comprobamos si se configuro el uso de NFC
			// Si el NFC esta activado, lanzamos una actividad para detectar el DNIe 3.0
			if (NfcHelper.isNfcServiceEnabled(context)) {
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

	/**
	 * Inicia el proceso de carga de certificados para firmar.
	 */
	protected void loadKeyStore() {

		// Buscamos si hay dispositivos CCID USB conectados
		final UsbManager usbMgr = (UsbManager) getSystemService(Context.USB_SERVICE);
		final HashMap<String, UsbDevice> devices = usbMgr != null ?
                usbMgr.getDeviceList() : new HashMap<String, UsbDevice>();

		for (final UsbDevice dev : devices.values()) {
			if (dev.getDeviceClass() == 0 && dev.getInterface(0).getInterfaceClass() == UsbConstants.USB_CLASS_CSCID) {
				this.usbManager = usbMgr;
				this.usbDevice = dev;
				break;
			}
		}

		// Si es igual a null es que no hay un CCID conectado
		if (this.usbManager == null) {
			Logger.i(ES_GOB_AFIRMA, "No hay dispositivos CCID USB conectados"); //$NON-NLS-1$
			// Cargamos el almacen de certificados normalmente
			new LoadKeyStoreManagerTask(this, this).execute();
		}

		//Si no, pedimos acceso al dispositivo
		else {
			Logger.i(ES_GOB_AFIRMA, "Se han detectado dispositivos CCID USB conectados"); //$NON-NLS-1$
			askForUsbPermission();
		}
	}

	/**
	 * Inicia el proceso de carga de certificados para firmar usando un almacen
	 * por conexion NFC.
	 */
	public void loadNfcKeyStore() {
		new LoadNfcKeyStoreManagerTask(this, this, this.passwordCallback).execute();
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
            onKeyStoreError(KeyStoreOperation.LOAD_KEYSTORE, msg, t);
		}
	}

    /**
     * Cuando se propduce un error al operar con el almacen de certificados.
     * @param op Operaci&oacute;n en la cual se produjo el error.
     * @param msg Mensaje de error.
     * @param t Error que origin&oacute; el problema.
     */
	public abstract void onKeyStoreError(KeyStoreOperation op, String msg, Throwable t);

	/** Operaci&oacute;n de firma. */
	protected enum KeyStoreOperation {
		/** Operaci&oacute;n de firma. */
		SIGN,
		/** Operaci&oacute;n de carga de almac&eacute;n. */
		LOAD_KEYSTORE,
		/** Operacion de seleccion de certificado. */
		SELECT_CERTIFICATE
	}
}
