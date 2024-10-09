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

import static es.gob.afirma.android.NFCDetectorActivity.INTENT_EXTRA_CAN_VALUE;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import java.util.HashMap;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.DnieConnectionManager;
import es.gob.afirma.android.crypto.InitializingNfcCardException;
import es.gob.afirma.android.crypto.KeyStoreManagerListener;
import es.gob.afirma.android.crypto.LoadKeyStoreManagerTask;
import es.gob.afirma.android.crypto.LoadNfcKeyStoreManagerTask;
import es.gob.afirma.android.crypto.LoadingCertificateException;
import es.gob.afirma.android.crypto.UnsupportedNfcCardException;
import es.gob.afirma.android.gui.ChooseCertTypeDialog;
import es.gob.jmulticard.android.callbacks.CachePasswordCallback;

/** Esta actividad abstracta integra las funciones necesarias para la cargar de un almacen de
 * certificados del dispositivo. La actividad integra la l&oacute;gica necesaria para utilizar
 * DNIe 3.0 v&iacute;a NFC, DNIe 2.0/3.0 a trav&eacute;s de lector de tarjetas y el almac&eacute;n
 * de Android. */
public class LoadKeyStoreFragmentActivity extends AppCompatActivity {

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	public static final String INTENT_EXTRA_PIN_VALUE = "pinValue"; //$NON-NLS-1$

	/** C&oacute;digo de solicitud de detecci&oacute;n de tarjeta por NFC. */
	private final static int REQUEST_CODE_DETECT_NFC_CARD = 2001;
	/** C&oacute;digo de solicitud de la habilitaci&oacute;n del NFC del dispositivo. */
	private final static int REQUEST_CODE_ENABLE_NFC = 2002;   // The request code

	public final static int REQUEST_KEYSTORE = 2004;   // The request code

	public final static int REQUEST_NFC_PARAMS = 2005;   // The request code

	public final static int REQUEST_NFC_KEYSTORE = 2006;   // The request code

	public final static String ERROR_LOADING_NFC_KEYSTORE = "errorLoadingNFCKeystore";   // The request code

	private static final String ACTION_USB_PERMISSION = "es.gob.afirma.android.USB_PERMISSION"; //$NON-NLS-1$

	private UsbManager usbManager = null;

    private UsbDevice usbDevice = null;

	protected static KeyStoreManagerListener ksmListener;

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
							ksmListener,
							LoadKeyStoreFragmentActivity.this,
							LoadKeyStoreFragmentActivity.this.getUsbDevice(),
							LoadKeyStoreFragmentActivity.this.getUsbManager()
					).execute();
				}
			}
			else {
				new LoadKeyStoreManagerTask(
						ksmListener,
						LoadKeyStoreFragmentActivity.this
				).execute();
			}
		}
	};

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
		final PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
		final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(this.mUsbReceiver, filter);
		this.usbManager.requestPermission(this.usbDevice, mPermissionIntent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_load_ks);

		String can = getIntent().getStringExtra(INTENT_EXTRA_CAN_VALUE);
		String pin = getIntent().getStringExtra(INTENT_EXTRA_PIN_VALUE);

		if (can != null && pin != null) {
			DnieConnectionManager.getInstance().setCanPasswordCallback(new CachePasswordCallback(can.toCharArray()));
			DnieConnectionManager.getInstance().setPinPasswordCallback(new CachePasswordCallback(pin.toCharArray()));
			searchNewNfcCard();
		}
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

				CachePasswordCallback canPasswordCallback = data != null
						? (CachePasswordCallback) data.getSerializableExtra(NFCDetectorActivity.INTENT_EXTRA_PASSWORD_CALLBACK)
						: null;
				DnieConnectionManager.getInstance().setCanPasswordCallback(canPasswordCallback);
				loadNfcKeyStore(canPasswordCallback);
			}
			else {
				// Si no se puede cargar el almacen y se establecido un CAN y DNI de DNIE, puede que
				// el problema es que alguno sea erroneo, asi que lo borramos
				DnieConnectionManager.getInstance().clearCan();
				DnieConnectionManager.getInstance().clearPin();

				runOnUiThread(
						new Runnable() {
							@Override
							public void run() {
								Toast.makeText(LoadKeyStoreFragmentActivity.this, R.string.nfc_cancelled, Toast.LENGTH_LONG).show();
							}
						}
				);

				if (resultCode == RESULT_CANCELED) {
					Logger.w(ES_GOB_AFIRMA, "Se cancelo el dialogo de insercion de CAN");
				}
				else {
					Logger.w(ES_GOB_AFIRMA, "No se detecto tarjeta NFC");
				}

				loadKeyStore();
			}
		}
		// Si volvemos despues de pedirle al usuario que habilite el NFC
		else if (requestCode == REQUEST_CODE_ENABLE_NFC) {
			// Si el usuario habilito el NFC, intentaremos leer una tarjeta NFC
			// Si no, cargamos directamente los certificados
			if(NfcHelper.isNfcServiceEnabled(this)) {
				final Intent stepsSignDNIe = new Intent(this, IntroSignDnieActivity.class);
				startActivity(stepsSignDNIe);
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
		} else if (requestCode == REQUEST_KEYSTORE) {
			// Depende de la eleccion del usuario se solicitara el almacen de un tipo u otro
			if (resultCode == REQUEST_NFC_KEYSTORE) {
				requestNFCKeystore();
			} else {
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
		CachePasswordCallback canPasswordCallback = DnieConnectionManager.getInstance().getCanPasswordCallback();
		if (canPasswordCallback != null) {
			intentNFC.putExtra(NFCDetectorActivity.INTENT_EXTRA_CAN_VALUE, canPasswordCallback.getPassword());
		}
		startActivityForResult(intentNFC, REQUEST_CODE_DETECT_NFC_CARD);
	}

	/**
	 * Abre el dialogo del sistema para la configuracion de NFC. El resultado de si se ha
	 * activado o no se determina en el onActivityResult.
	 */
	private void openNfcSystemSettings() {
		Toast.makeText(getApplicationContext(), R.string.enable_nfc, Toast.LENGTH_SHORT).show();
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
		// Si tenemos habilitado el uso de NFC, se pregunta al usuario si firmar con DNIe o el almacen de certificados; si no, cargamos directamente
		// el almacen en cuestion (que puede ser una tarjeta previamente buscada)
		if (NfcHelper.isNfcPreferredConnection(context)) {
			final Intent intentRequestKeystore = new Intent(this, ChooseCertTypeDialog.class);
			startActivityForResult(intentRequestKeystore, REQUEST_KEYSTORE);
		}
		else {
			loadKeyStore();
		}
	}

	protected void requestNFCKeystore() {
		// Comprobamos si se configuro el uso de NFC
		// Si el NFC esta activado, lanzamos una actividad para detectar el DNIe 3.0
		if (NfcHelper.isNfcServiceEnabled(getApplicationContext())) {
			final Intent stepsSignDNIe = new Intent(this, IntroSignDnieActivity.class);
			startActivity(stepsSignDNIe);
		}
		// Si el NFC no esta activado, se le solicita al usuario activarlo
		else {
			openNfcSystemSettings();
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
			new LoadKeyStoreManagerTask(ksmListener, this).execute();
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
	private void loadNfcKeyStore(CachePasswordCallback canPasswordCallback) {
		new LoadNfcKeyStoreManagerTask(ksmListener, this, canPasswordCallback).execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void onLoadingKeyStoreError(final String msg, final Throwable t) {

		// Si el error de carga es un error de conexion NFC, volvemos a mostrar el dialogo,
		// si no, indicamos un error en la firma.
		if (t instanceof UnsupportedNfcCardException) {
			final Intent stepsSignDNIe = new Intent(this, IntroSignDnieActivity.class);
			stepsSignDNIe.putExtra(ERROR_LOADING_NFC_KEYSTORE, getString(R.string.unsupported_card));
			startActivity(stepsSignDNIe);
		}
		else if (t instanceof InitializingNfcCardException) {
			final Intent stepsSignDNIe = new Intent(this, IntroSignDnieActivity.class);
			stepsSignDNIe.putExtra(ERROR_LOADING_NFC_KEYSTORE, getString(R.string.nfc_card_initializing_error));
			startActivity(stepsSignDNIe);
		}
		else if (t instanceof LoadingCertificateException) {
			loadKeyStore();
		}
		else {
            ksmListener.onKeyStoreError(KeyStoreOperation.LOAD_KEYSTORE, msg, t);
		}
	}

	/** Operaci&oacute;n de firma. */
	public enum KeyStoreOperation {
		/** Operaci&oacute;n de firma. */
		SIGN,
		/** Operaci&oacute;n de carga de almac&eacute;n. */
		LOAD_KEYSTORE,
		/** Operacion de seleccion de certificado. */
		SELECT_CERTIFICATE
	}
}
