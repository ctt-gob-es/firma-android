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
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.util.HashMap;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.CachePasswordCallback;
import es.gob.afirma.android.crypto.DnieConnectionManager;
import es.gob.afirma.android.crypto.InitializingNfcCardException;
import es.gob.afirma.android.crypto.KeyStoreManagerListener;
import es.gob.afirma.android.crypto.LoadKeyStoreManagerTask;
import es.gob.afirma.android.crypto.LoadNfcKeyStoreManagerTask;
import es.gob.afirma.android.crypto.LoadingCertificateException;
import es.gob.afirma.android.crypto.MSCBadPinException;
import es.gob.afirma.android.crypto.SmartCardConnectionException;
import es.gob.afirma.android.crypto.UnsupportedNfcCardException;
import es.gob.afirma.android.errors.AppErrorCode;
import es.gob.afirma.android.gui.ChooseCertTypeDialog;
import es.gob.jmulticard.card.dnie.InvalidAccessCodeException;

/** Esta actividad abstracta integra las funciones necesarias para la cargar de un almacen de
 * certificados del dispositivo. La actividad integra la l&oacute;gica necesaria para utilizar
 * DNIe 3.0 v&iacute;a NFC, DNIe 2.0/3.0 a trav&eacute;s de lector de tarjetas y el almac&eacute;n
 * de Android. */
public class LoadKeyStoreFragmentActivity extends FragmentActivity implements NfcAdapter.ReaderCallback {

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	/** C&oacute;digo de solicitud de la habilitaci&oacute;n del NFC del dispositivo. */
	private final static int REQUEST_CODE_ENABLE_NFC = 2002;   // The request code

	/** C&oacute;digo para la peticion del CAN y el PIN del DNIe. */
	public final static int REQUEST_DNIE_PARAMS = 2005;   // The request code

	public final static String ERROR_UNSUPPORTED_NFC = "errorUnsupportedNFC";

	public final static String ERROR_CAN_VALIDATION_NFC = "errorCANValidation";

	public final static String ERROR_PIN_VALIDATION_NFC = "errorPINValidation";

    public final static String ERROR_READING_CARD = "errorReadingCard";

    public final static String ERROR_LOADING_CERTS = "errorLoadingCerts";

	private static final String ACTION_USB_PERMISSION = "es.gob.afirma.android.USB_PERMISSION"; //$NON-NLS-1$

	/** Indica si las claves cargadas se usar&aacute;n s&oacute;lo para autenticaci&oacute;n. */
	private boolean onlyAuthenticationOperation = true;

	private UsbManager usbManager = null;

    private UsbDevice usbDevice = null;

	protected static KeyStoreManagerListener ksmListener;

	protected boolean isDNIeCert = false;

    /**
     * Vista sobre la que se pueden mostrar layouts, como el de detecci&oacute;n de tarjetas
     * mediante NFC.
     */
    private View overlay;

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
		registerReceiver(this.mUsbReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
		this.usbManager.requestPermission(this.usbDevice, mPermissionIntent);
	}

	protected void setOnlyAuthenticationOperation(boolean onlyAuthenticationOperation) {
		this.onlyAuthenticationOperation = onlyAuthenticationOperation;
	}

	protected boolean isOnlyAuthenticationOperation() {
		return this.onlyAuthenticationOperation;
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

		// Si volvemos despues de pedirle al usuario que habilite el NFC
		if (requestCode == REQUEST_CODE_ENABLE_NFC) {
			// Si el usuario habilito el NFC, intentaremos leer una tarjeta NFC
			// Si no, cargamos directamente los certificados
			if(NfcHelper.isNfcServiceEnabled(this)) {
				final Intent stepsSignDNIe = new Intent(this, IntroUseDnieActivity.class);
				startActivityForResult(stepsSignDNIe, REQUEST_DNIE_PARAMS);
			}
			else {
				runOnUiThread(
						new Runnable() {
							@Override
							public void run() {
								Toast.makeText(getApplicationContext(), R.string.nfc_no_detected, Toast.LENGTH_SHORT).show();
							}
						}
				);
				loadKeyStore(this, null);
			}
			return;
		}
		else if (requestCode == REQUEST_DNIE_PARAMS) {
			if (resultCode == RESULT_OK) {
				String can = data.getStringExtra(getString(R.string.extra_can));
				String pin = data.getStringExtra(getString(R.string.extra_pin));
                if (can != null && pin != null) {
					DnieConnectionManager.getInstance().setCanPasswordCallback(new CachePasswordCallback(can.toCharArray()));
					DnieConnectionManager.getInstance().setPinPasswordCallback(new CachePasswordCallback(pin.toCharArray()));
					searchNewNfcCard();
				}
			}
			else {
				ksmListener.onLoadingKeyStoreError(new PendingIntent.CanceledException("Operacion cancelada"));
			}
			return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}


    private NfcAdapter nfcAdapter = null;

    private static final int TECH_FLAGS = NfcAdapter.FLAG_READER_NFC_A
            | NfcAdapter.FLAG_READER_NFC_B
            | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            | NfcAdapter.FLAG_READER_NFC_F
            | NfcAdapter.FLAG_READER_NFC_V;

	/**
	 * Busca una nueva tarjeta NFC. El resultado de la busqueda se obtiene en el
	 * metodo onActivityResult().
	 */
	public void searchNewNfcCard() {

        // Mostramos la vista de deteccion de la tarjeta NFC
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        showNfcDetectionLayout();
                        Logger.i(ES_GOB_AFIRMA, "Mostramos el overlay");
                    }
                }
        );

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        this.nfcAdapter.enableReaderMode(this, this, TECH_FLAGS, new Bundle());
    }

    private OnBackPressedCallback overlayBackCallback;

    /**
     * Muestra la pantalla de lectura del DNIe.
     */
    private void showNfcDetectionLayout() {

        // Creamos el callback para la captura del evento Atras en la pantalla. Si ya existia,
        // lo activamos
        if (overlayBackCallback == null) {
            overlayBackCallback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    cancel();
                }
            };
            // Asociamos el evento a esta actividad
            getOnBackPressedDispatcher().addCallback(this, overlayBackCallback);
        } else {
            overlayBackCallback.setEnabled(true);
        }

        // Creamos el panel
        FrameLayout content = findViewById(android.R.id.content);
        overlay = getLayoutInflater().inflate(R.layout.activity_detect_nfc, content, false);

        // Establecemos el comportamiento
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setFocusableInTouchMode(true);
        Button cancelButton = (Button) overlay.findViewById(R.id.cancelSearchBtn);
        cancelButton.setOnClickListener((view) -> {
            cancel();
        });

        // Agregamos el panel
        getWindow().addContentView(
                overlay,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );


        overlay.bringToFront();
        overlay.setElevation(1000f);
        ViewCompat.setZ(overlay, 1000f);
        overlay.setVisibility(View.VISIBLE);
        overlay.setAlpha(1f);

        // Fuerza relayout
        content.post(() -> {
            overlay.requestLayout();
            overlay.invalidate();
        });

    }

    /**
     * Oculta la pantalla de lectura del DNIe.
     */
    private void hideNfcDetectionLayout() {
        if (overlay != null) {
            ((ViewGroup) overlay.getParent()).removeView(overlay);
            overlay = null;
        }

        if (overlayBackCallback != null) {
            // Desactiva el callback para que el back vuelva a su flujo normal
            overlayBackCallback.setEnabled(false);
        }

    }

    /**
     * Cancela la operaci&oacute;n con el almac&eacute;n seleccionado y permite volver a elegir
     * almac&eacute;n. Si s&oacute;lo hab&iacute;a un almac&eacute;n, cancela la operacion por
     * completo.
     */
    public void cancel() {

        // Oculta el dialogo de deteccion
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        hideNfcDetectionLayout();
                    }
                }
        );

        // Si se puede usar DNIe, se permite seleccionar el almacen. Si no, se cancela todo
        if (NfcHelper.isNfcPreferredConnection(this)) {
            loadKeyStore(this, null);
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (this.nfcAdapter != null) {
            Bundle options = new Bundle();
            this.nfcAdapter.enableReaderMode(this, this, TECH_FLAGS, options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (this.nfcAdapter != null) {
            this.nfcAdapter.disableReaderMode(this);
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        Log.i(ES_GOB_AFIRMA, "Tag detectado: " + tag.toString());

        // Oculta el dialogo de deteccion
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        hideNfcDetectionLayout();
                    }
                }
        );

        // Opera con a tarjeta
        try {
            prepareCardConnection(tag);
        } catch (IOException e) {
            Log.e(ES_GOB_AFIRMA, "Fallo en el intento de conexion", e);
            onLoadingKeyStoreError(new InitializingNfcCardException("No se pudo conectar a la tarjeta por NFC"));
        }
    }

    private void prepareCardConnection(Tag tag) throws IOException {

        IsoDep mIsoDep = IsoDep.get(tag);
        mIsoDep.connect();

        DnieConnectionManager.getInstance().setIsoDepConnection(mIsoDep);

        loadNfcKeyStore();
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
	 * Carga el almac&eacute;n de certificados (que podr&iacute;a ser una tarjeta inteligente
	 * compatible situada al alcance del NFC).
	 * @param t Error a mostrar en caso de que exista (ej: MSCBadPinException)
	 * @param context Contexto de la actividad.
	 */
	protected void loadKeyStore(Context context, Throwable t) {
		// Si tenemos habilitado el uso de NFC, se pregunta al usuario si firmar con DNIe o el almacen de certificados; si no, cargamos directamente
		// el almacen en cuestion (que puede ser una tarjeta previamente buscada)
		if (NfcHelper.isNfcPreferredConnection(context)) {
			if (t != null && t instanceof MSCBadPinException) {
				requestNFCKeystore(t);
			} else {
				LoadKeyStoreFragmentActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						ChooseCertTypeDialog certTypeDialog = new ChooseCertTypeDialog(
								LoadKeyStoreFragmentActivity.this,
								new ChooseCertTypeDialog.ChooseCertTypeListener() {
									@Override
									public void certTypeChoosed(int certType) {
										if (certType == ChooseCertTypeDialog.CERT_TYPE_DNIE) {
											isDNIeCert = true;
											requestNFCKeystore(t);
										} else if (certType == ChooseCertTypeDialog.CERT_TYPE_LOCAL) {
											isDNIeCert = false;
											loadKeyStore();
										} else {
											LoadKeyStoreFragmentActivity.this.ksmListener.onKeyStoreError(
													KeyStoreOperation.SELECT_CERTIFICATE,
													new PendingIntent.CanceledException());
										}
									}
								});
						certTypeDialog.setModeAuthentication(LoadKeyStoreFragmentActivity.this.isOnlyAuthenticationOperation());
						certTypeDialog.show();
					}
				});
			}
		}
		else {
			loadKeyStore();
		}
	}

	protected void requestNFCKeystore(Throwable t) {
		// Comprobamos si se configuro el uso de NFC
		// Si el NFC esta activado, lanzamos una actividad para detectar el DNIe por NFC
		if (NfcHelper.isNfcServiceEnabled(getApplicationContext())) {
			final Intent stepsSignDNIe = new Intent(this, IntroUseDnieActivity.class);
			if (t != null && t instanceof MSCBadPinException) {
				stepsSignDNIe.putExtra(ERROR_PIN_VALIDATION_NFC, true);
			}
			startActivityForResult(stepsSignDNIe, REQUEST_DNIE_PARAMS);
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
	private void loadNfcKeyStore() {
		new LoadNfcKeyStoreManagerTask(ksmListener, this).execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void onLoadingKeyStoreError(final Throwable t) {

        Log.e(ES_GOB_AFIRMA, "Error al cargar el almacen", t);

        // Si el error de carga es un error de conexion NFC, volvemos a mostrar el dialogo,
        // si no, indicamos un error en la firma.
        if (t instanceof UnsupportedNfcCardException) {
            DnieConnectionManager.getInstance().reset();
            DnieConnectionManager.getInstance().clearCan();
            DnieConnectionManager.getInstance().clearPin();
            final Intent stepsSignDNIe = new Intent(this, IntroUseDnieActivity.class);
            stepsSignDNIe.putExtra(ERROR_UNSUPPORTED_NFC, true);
            startActivityForResult(stepsSignDNIe, REQUEST_DNIE_PARAMS);
        }
        else if (t instanceof InvalidAccessCodeException) {
            DnieConnectionManager.getInstance().reset();
            DnieConnectionManager.getInstance().clearCan();
            DnieConnectionManager.getInstance().clearPin();
            final Intent stepsSignDNIe = new Intent(this, IntroUseDnieActivity.class);
            stepsSignDNIe.putExtra(ERROR_CAN_VALIDATION_NFC, true);
            startActivityForResult(stepsSignDNIe, REQUEST_DNIE_PARAMS);
        }
        else if (t instanceof InitializingNfcCardException) {
            final Intent stepsSignDNIe = new Intent(this, StepsInsertDataDnieActivity.class);
            stepsSignDNIe.putExtra(getString(R.string.extra_smartcard_error), AppErrorCode.ThirdParty.ERROR_INITIALIZING_CARD.getCode());
            stepsSignDNIe.putExtra(getString(R.string.extra_can), DnieConnectionManager.getInstance().getCanPasswordCallback().getPassword());
            stepsSignDNIe.putExtra(getString(R.string.extra_pin), DnieConnectionManager.getInstance().getPinPasswordCallback().getPassword());
            startActivityForResult(stepsSignDNIe, REQUEST_DNIE_PARAMS);
        }

        // Si fallo la conexion con la tarjeta, lo reintentamos
        else if (t instanceof SmartCardConnectionException) {
            final Intent stepsSignDNIe = new Intent(this, StepsInsertDataDnieActivity.class);
            stepsSignDNIe.putExtra(getString(R.string.extra_smartcard_error), AppErrorCode.Hardware.SMARTCARD_CONNECTION_LOST.getCode());
            stepsSignDNIe.putExtra(getString(R.string.extra_can), DnieConnectionManager.getInstance().getCanPasswordCallback().getPassword());
            stepsSignDNIe.putExtra(getString(R.string.extra_pin), DnieConnectionManager.getInstance().getPinPasswordCallback().getPassword());
            startActivityForResult(stepsSignDNIe, REQUEST_DNIE_PARAMS);
        }
        // Si es un error en la carga, lo reintentamos
        else if (t instanceof LoadingCertificateException) {
            final Intent stepsSignDNIe = new Intent(this, StepsInsertDataDnieActivity.class);
            stepsSignDNIe.putExtra(getString(R.string.extra_smartcard_error), AppErrorCode.ThirdParty.ERROR_LOADING_CERTIFICATES.getCode());
            stepsSignDNIe.putExtra(getString(R.string.extra_can), DnieConnectionManager.getInstance().getCanPasswordCallback().getPassword());
            stepsSignDNIe.putExtra(getString(R.string.extra_pin), DnieConnectionManager.getInstance().getPinPasswordCallback().getPassword());
            startActivityForResult(stepsSignDNIe, REQUEST_DNIE_PARAMS);
        }
        // Si se ha cancelado la operacion y esta disponible el uso de mas de un almacen, permitimos
        // seleccionar almacen. Si no, damos por hecho que el usuario quiere cancelar.
        else if (t instanceof PendingIntent.CanceledException) {
            if (NfcHelper.isNfcPreferredConnection(this)) {
                DnieConnectionManager.getInstance().reset();
                DnieConnectionManager.getInstance().clearCan();
                DnieConnectionManager.getInstance().clearPin();
                loadKeyStore(this, null);
            } else {
                ksmListener.onKeyStoreError(KeyStoreOperation.SELECT_CERTIFICATE, t);
            }
        }
        else {
            ksmListener.onKeyStoreError(KeyStoreOperation.LOAD_KEYSTORE, t);
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
