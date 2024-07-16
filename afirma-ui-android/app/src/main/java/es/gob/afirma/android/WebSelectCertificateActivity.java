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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.security.KeyChainException;
import android.view.KeyEvent;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.AndroidHttpManager;
import es.gob.afirma.android.crypto.CipherDataManager;
import es.gob.afirma.android.crypto.MobileKeyStoreManager;
import es.gob.afirma.android.crypto.SelectKeyAndroid41BugException;
import es.gob.afirma.android.gui.DownloadFileTask;
import es.gob.afirma.android.gui.MessageDialog;
import es.gob.afirma.android.gui.SendDataTask;
import es.gob.afirma.android.gui.SendDataTask.SendDataListener;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.misc.http.UrlHttpManagerFactory;
import es.gob.afirma.core.misc.protocol.ParameterException;
import es.gob.afirma.core.misc.protocol.ProtocolInvocationUriParser;
import es.gob.afirma.core.misc.protocol.UrlParametersToSelectCert;

/** Actividad dedicada a la firma de los datos recibidos en la entrada mediante un certificado
 * del almac&eacute;n central seleccionado por el usuario. */
public final class WebSelectCertificateActivity extends LoadKeyStoreFragmentActivity
                                                implements DownloadFileTask.DownloadDataListener,
                                                            SendDataListener,
                                                            MobileKeyStoreManager.CertificateSelectionListener {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	/** Juego de carateres UTF-8. */
	private static final String DEFAULT_URL_ENCODING = "UTF-8"; //$NON-NLS-1$

	private UrlParametersToSelectCert parameters;

    private DownloadFileTask downloadFileTask = null;

	private MessageDialog messageDialog;
	MessageDialog getMessageDialog() {
		return this.messageDialog;
	}

	private ProgressDialog progressDialog = null;
	void setProgressDialog(final ProgressDialog pd) {
		this.progressDialog = pd;
	}

	static {
		// Instalamos el gestor de descargas que deseamos utilizar en las invocaciones por
		// protocolo a la aplicacion
		UrlHttpManagerFactory.install(new AndroidHttpManager());
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent() == null || getIntent().getData() == null) {
			Logger.w(ES_GOB_AFIRMA, "No se han indicado parametros de entrada para la actividad");  //$NON-NLS-1$
			closeActivity();
			return;
		}

		// Si cargamos la actividad desde el carrusel de aplicaciones, redirigimos a la
		// pantalla principal
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
				== Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(this, HomeActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return;
		}

		// Si no estamos creando ahora la pantalla (por se una rotacion)
		if (savedInstanceState != null){
			Logger.i(ES_GOB_AFIRMA, "Se esta relanzando la actividad. Se omite volver a iniciar el proceso de firma");
			return;
		}

        Logger.d(ES_GOB_AFIRMA, "URI de invocacion: " + getIntent().getDataString()); //$NON-NLS-1$

        if (getIntent().getDataString() == null) {
            Logger.w(ES_GOB_AFIRMA, "Se ha invocado sin URL a la actividad de seleccion de certificado por protocolo. Se cierra la actividad"); //$NON-NLS-1$
            closeActivity();
            return;
        }

		try {
			this.parameters = ProtocolInvocationUriParser.getParametersToSelectCert(getIntent().getDataString(), true);
		}
		catch (final ParameterException e) {
			Logger.e(ES_GOB_AFIRMA, "Error en los parametros de firma", e); //$NON-NLS-1$
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true);
			return;
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA, "Error grave en el onCreate de WebSelectCertificateActivity", e); //$NON-NLS-1$
			e.printStackTrace();
			showErrorMessage(getString(R.string.error_bad_params));
			launchError(ErrorManager.ERROR_BAD_PARAMETERS, true);
			return;
		}

		processSelectionRequest();
	}

	/** Inicia el proceso de selecci&oacute;n de certificado con los parametros previamente configurados. */
	private void processSelectionRequest() {

	    // Si se nos pasa un identificador de fichero, entonces no se nos ha podido pasar toda la
        // configuracion a traves de la URL y habra que descargarla
        if (this.parameters.getFileId() != null) {
            Logger.i(ES_GOB_AFIRMA, "Se van a descargar la configuracion desde el servidor con el identificador: " + this.parameters.getFileId()); //$NON-NLS-1$
            this.downloadFileTask = new DownloadFileTask(
                    this.parameters.getFileId(),
                    this.parameters.getRetrieveServletUrl(),
                    this
            );
            this.downloadFileTask.execute();
        }

        // Si tenemos la configuracion completa, cargamos un certificado
        else {

            Logger.i(ES_GOB_AFIRMA, "Se inicia la seleccion de certificado"); //$NON-NLS-1$
            showProgressDialog(getString(R.string.dialog_msg_loading_keystore));
            loadKeyStore(this);
        }
	}

    @Override
    public synchronized void certificateSelected(final MobileKeyStoreManager.SelectCertificateEvent kse) {

        byte[] certificate;
        try {
            Certificate[] certChain = kse.getCertChain();
            if (certChain == null || certChain.length == 0) {
                throw new NullPointerException("No se obtuvo el certificado del almacen");
            }
            certificate = certChain[0].getEncoded();
        }
        catch (final KeyChainException e) {
            if ("4.1.1".equals(Build.VERSION.RELEASE) || "4.1.0".equals(Build.VERSION.RELEASE) || "4.1".equals(Build.VERSION.RELEASE)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                Logger.e(ES_GOB_AFIRMA, "Error al extraer el certificado en Android " + Build.VERSION.RELEASE + ": " + e); //$NON-NLS-1$ //$NON-NLS-2$
                onKeyStoreError(KeyStoreOperation.SELECT_CERTIFICATE, getString(R.string.error_android_4_1), new SelectKeyAndroid41BugException(e));
            }
            else {
                Logger.e(ES_GOB_AFIRMA, "No se pudo extraer el certificado del almacen: " + e); //$NON-NLS-1$
                onKeyStoreError(KeyStoreOperation.SELECT_CERTIFICATE, "No se pudo extraer el certificado del almacen", e);
            }
            return;
        }
        catch (final AOCancelledOperationException e) {
            Logger.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado: " + e); //$NON-NLS-1$
            onKeyStoreError(KeyStoreOperation.SELECT_CERTIFICATE, "El usuario no selecciono un certificado", new PendingIntent.CanceledException(e));
            return;
        }
        // Cuando se instala el certificado desde el dialogo de seleccion, Android da a elegir certificado
        // en 2 ocasiones y en la segunda se produce un "java.lang.AssertionError". Se ignorara este error.
        catch (final AssertionError e) {
            Logger.e(ES_GOB_AFIRMA, "Posible error al insertar un nuevo certificado en el almacen. No se hara nada", e); //$NON-NLS-1$
            return;
        }
        catch (final Throwable e) {
            Logger.e(ES_GOB_AFIRMA, "Error al recuperar el certificado seleccionado", e); //$NON-NLS-1$
            onKeyStoreError(KeyStoreOperation.SELECT_CERTIFICATE, "Error al recuperar la clave del certificado de firma", e); //$NON-NLS-1$
            return;
        }

        onSelectCertificateChainSuccess(certificate);
    }

    @Override
    public synchronized void onLoadingKeyStoreSuccess(final MobileKeyStoreManager msm) {

        // Si el usuario cancelo la insercion de PIN o cualquier otro dialogo del almacen
        if(msm == null){
            onKeyStoreError(KeyStoreOperation.LOAD_KEYSTORE, "El usuario cancelo la operacion durante la carga del almacen", new PendingIntent.CanceledException("Se cancela la seleccion del almacen"));
            return;
        }
        msm.getCertificateChainAsynchronously(this);
    }

	@Override
	public void onKeyStoreError(KeyStoreOperation op, String msg, Throwable t) {
		if (op == KeyStoreOperation.LOAD_KEYSTORE) {
			launchError(ErrorManager.ERROR_ESTABLISHING_KEYSTORE, true);
		}
		else if (op == KeyStoreOperation.SELECT_CERTIFICATE) {

			if (t instanceof SelectKeyAndroid41BugException) {
				launchError(ErrorManager.ERROR_PKE_ANDROID_4_1, true);
			}
			else if (t instanceof KeyChainException) {
				launchError(ErrorManager.ERROR_PKE, true);
			}
			else if (t instanceof PendingIntent.CanceledException) {
				Logger.e(ES_GOB_AFIRMA, "El usuario no selecciono un certificado", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false);
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error al recuperar el certificado", t); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_PKE, true);
			}
		}
		Logger.e(ES_GOB_AFIRMA, "Error desconocido", t); //$NON-NLS-1$
		launchError(ErrorManager.ERROR_SELECTING_CERTIFICATE, true);
	}


	@Override
	public void onStart() {
		super.onStart();
	}

	/** Env&iacute;a los datos indicado a un servlet. En caso de error, cierra la aplicaci&oacute;n.
	 * @param data Datos que se desean enviar. */
	private void sendData(final String data, final boolean critical) {
		new SendDataTask(
			this.parameters.getId(),
			this.parameters.getStorageServletUrl(),
			data,
			this,
			critical
		).execute();
	}

	/** Muestra un mensaje de error y lo env&iacute;a al servidor para que la p&aacute;gina Web
	 * tenga constancia de &eacute;l.
	 * @param errorId Identificador del error.
	 * @param critical <code>true</code> si debe mostrarse el error al usuario, <code>false</code>
	 *                    en caso contrario.
	 */
	private void launchError(final String errorId, final boolean critical) {
		try {
			sendData(URLEncoder.encode(ErrorManager.genError(errorId, null), DEFAULT_URL_ENCODING), critical);
		}
		catch (final UnsupportedEncodingException e) {
			// No puede darse, el soporte de UTF-8 es obligatorio
			Logger.e(ES_GOB_AFIRMA,
				"No se ha podido enviar la respuesta al servidor por error en la codificacion " + DEFAULT_URL_ENCODING, e //$NON-NLS-1$
			);
		}
		catch (final Throwable e) {
			Logger.e(ES_GOB_AFIRMA,
				"Error desconocido al enviar el error obtenido al servidor: " + e, e //$NON-NLS-1$
			);
		}
	}

	/** Muestra un mensaje de advertencia al usuario.
	 * @param message Mensaje que se desea mostrar. */
	private void showErrorMessage(final String message) {

		dismissProgressDialog();

		if (this.messageDialog == null) {
			this.messageDialog = MessageDialog.newInstance(message);
			this.messageDialog.setListener(new CloseActivityDialogAction());
			this.messageDialog.setDialogBuilder(this);
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					WebSelectCertificateActivity.this.getMessageDialog().show(getSupportFragmentManager(), "ErrorDialog"); //$NON-NLS-1$;
				}
				catch (final Exception e) {
					// Si falla el mostrar el error (posiblemente por no disponer de un contexto grafico para mostrarlo)
					// se mostrara en un Toast
					Toast.makeText(WebSelectCertificateActivity.this, message, Toast.LENGTH_LONG).show();
				}

			}
		});
	}

	private void showProgressDialog(final String message) {
		runOnUiThread(
			new Runnable() {
				@Override
				public void run() {
					try {
						setProgressDialog(ProgressDialog.show(WebSelectCertificateActivity.this, "", message, true)); //$NON-NLS-1$
					}
					catch (final Throwable e) {
						Logger.e(ES_GOB_AFIRMA, "No se ha podido mostrar el dialogo de progreso", e); //$NON-NLS-1$
					}
				}
			}
		);
	}

    @Override
    public synchronized void onDownloadingDataSuccess(final byte[] data) {
        Logger.i(ES_GOB_AFIRMA, "Se ha descargado correctamente la configuracion para la seleccion de un certificado"); //$NON-NLS-1$

        // Si hemos tenido que descargar los datos desde el servidor, los desciframos y llamamos
        // al dialogo de seleccion de certificados para la firma
        byte[] decipheredData;
        try {
            decipheredData = CipherDataManager.decipherData(data, this.parameters.getDesKey());
        } catch (final IOException e) {
            Logger.e(ES_GOB_AFIRMA, "Los datos proporcionados no est&aacute;n correctamente codificados en base 64", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        } catch (final GeneralSecurityException e) {
            Logger.e(ES_GOB_AFIRMA, "Error al descifrar los datos recuperados del servidor para la seleccion de certificado", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        } catch (final IllegalArgumentException e) {
            Logger.e(ES_GOB_AFIRMA, "Los datos recuperados no son un base64 valido", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        } catch (final Throwable e) {
            Logger.e(ES_GOB_AFIRMA, "Error desconocido durante el descifrado de los datos", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        }

        Logger.i(ES_GOB_AFIRMA, "Se han descifrado los datos y se inicia su analisis:\n" + new String(decipheredData)); //$NON-NLS-1$

        try {
            this.parameters = ProtocolInvocationUriParser.getParametersToSelectCert(decipheredData, true);
        } catch (final ParameterException e) {
            Logger.e(ES_GOB_AFIRMA, "Error en los parametros XML de configuracion de seleccion de certificado", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        } catch (final Throwable e) {
            Logger.e(ES_GOB_AFIRMA, "Error desconocido al analizar los datos descargados desde el servidor", e); //$NON-NLS-1$
            showErrorMessage(getString(R.string.error_bad_params));
            return;
        }

        // Iniciamos la seleccion de certificado
        processSelectionRequest();
    }

    @Override
    public synchronized void onDownloadingDataError(final String msg, final Throwable t) {
        Logger.e(ES_GOB_AFIRMA, "Error durante la descarga de la configuracion para la seleccion de certificado:" + msg, t); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        showErrorMessage(getString(R.string.error_server_connect));
    }

	public void onSelectCertificateChainSuccess(final byte[] certificate) {
		Logger.i(ES_GOB_AFIRMA, "Certificado recuperado correctamente. Se cifra el resultado.");

		// Ciframos si nos dieron clave privada, si no subimos los datos sin cifrar
		final String data;
		if (this.parameters.getDesKey() != null) {
			try {
				data = CipherDataManager.cipherData(certificate, this.parameters.getDesKey());
			}
			catch (final GeneralSecurityException e) {
				Logger.e(ES_GOB_AFIRMA, "Error en el cifrado del certificado", e); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_CIPHERING, true);
				return;
			}
			catch (final Throwable e) {
				Logger.e(ES_GOB_AFIRMA, "Error desconocido al cifrar el certificado", e); //$NON-NLS-1$
				launchError(ErrorManager.ERROR_CIPHERING, true);
				return;
			}
		}
		else {
			data = Base64.encode(certificate, true);
		}

        Logger.i(ES_GOB_AFIRMA, "Certificado cifrado. Se envia al servidor."); //$NON-NLS-1$
		try {
			sendData(data, true);
			Logger.i(ES_GOB_AFIRMA, "Certificado enviado."); //$NON-NLS-1$
		}
		catch (Throwable e) {
			onSendingDataError(e, true);
		}
	}

	@Override
	public void onSendingDataSuccess(final byte[] result, final boolean critical) {
		Logger.i(ES_GOB_AFIRMA, "Resultado del deposito de la firma: " + (result == null ? null : new String(result))); //$NON-NLS-1$
		closeActivity();
	}

	@Override
	public void onSendingDataError(final Throwable error, final boolean critical) {

		Logger.e(ES_GOB_AFIRMA, "Se ejecuta la funcion de error en el envio de datos", error); //$NON-NLS-1$

		if (critical) {
			dismissProgressDialog();
			showErrorMessage(getString(R.string.error_sending_data));
			return;
		}
		closeActivity();
	}

	/** Comprueba si esta abierto el di&aacute;logo de espera y lo cierra en dicho caso. */
	private void dismissProgressDialog() {
		if (this.progressDialog != null) {
			this.progressDialog.dismiss();
		}
	}

	/** Comprueba si esta abierto el di&aacute;logo de mensajes y lo cierra en dicho caso. */
	private void dismissMessageDialog() {
		if (this.messageDialog != null && this.messageDialog.isVisible()) {
					this.messageDialog.dismiss();
		}
	}

	/** Accion para el cierre de la actividad. */
	private class CloseActivityDialogAction implements DialogInterface.OnClickListener {

		CloseActivityDialogAction() {
			// Constructor vacio para evitar el sintetico
		}

		@Override
		public void onClick(final DialogInterface dialog, final int which) {
			closeActivity();
		}
	}

	void closeActivity() {
		// Cerramos a la fuerza para, en siguientes ejecuciones, no se vuelvan a cargar los mismos datos
		finishAffinity();
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_HOME) {
			launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		launchError(ErrorManager.ERROR_CANCELLED_OPERATION, false);
		super.onBackPressed();
	}

	@Override
	protected void onStop() {
		dismissProgressDialog();
		dismissMessageDialog();
		super.onStop();
	}

    @Override
    protected void onDestroy() {
        if (this.downloadFileTask != null) {
            Logger.d(ES_GOB_AFIRMA, "WebSelectCertificateActivity onDestroy: Cancelamos la descarga"); //$NON-NLS-1$
            try {
                this.downloadFileTask.cancel(true);
            }
            catch(final Exception e) {
                Logger.e(ES_GOB_AFIRMA, "No se ha podido cancelar el procedimiento de descarga de la configuracion", e); //$NON-NLS-1$
            }
        }
        super.onDestroy();
    }
}