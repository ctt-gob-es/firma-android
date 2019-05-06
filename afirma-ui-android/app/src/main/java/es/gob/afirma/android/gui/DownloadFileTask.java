/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.android.gui;

import android.util.Log;

import java.io.IOException;
import java.net.URL;

import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.misc.http.UrlHttpMethod;

/** Tarea para la descarga de un fichero del servidor intermedio. */
public final class DownloadFileTask extends BasicHttpTransferDataTask {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	/** Juego de carateres UTF-8. */
	private static final String DEFAULT_URL_ENCODING = "UTF-8"; //$NON-NLS-1$

	private static final String METHOD_OP_GET = "get"; //$NON-NLS-1$

	private static final String SYNTAX_VERSION = "1_0"; //$NON-NLS-1$

	private static final String ERROR_PREFIX = "ERR-"; //$NON-NLS-1$

	private static final String ERROR_NO_DATA = ERROR_PREFIX + "06"; //$NON-NLS-1$

	private static final int MAX_DOWNLOAD_TRIES = 10;

	private final String fileId;
	private final URL retrieveServletUrl;
	private final DownloadDataListener ddListener;

	private String errorMessage = null;
	private Throwable errorThowable = null;

	private int downloadTries = 0;

	/** Crea una tarea para la descarga de un fichero del servidor intermedio.
	 * @param fileId Identificadod del fichero en el servidor intermedio
	 * @param retrieveServletUrl URL del servidor intermedio
	 * @param ddListener Clase a la que hay que notificar el resultado de la descraga */
	public DownloadFileTask(final String fileId, final URL retrieveServletUrl, final DownloadDataListener ddListener) {
		this.fileId = fileId;
		this.retrieveServletUrl = retrieveServletUrl;
		this.ddListener = ddListener;
	}

	@Override
	protected byte[] doInBackground(final Void... arg0) {

		Log.i(ES_GOB_AFIRMA, " -- DownloadFileTask doInBackgroung"); //$NON-NLS-1$

		Log.i(ES_GOB_AFIRMA, "Descargando datos de servidor remoto"); //$NON-NLS-1$

		final byte[] data;
		try {
			final StringBuilder url = new StringBuilder(this.retrieveServletUrl.toExternalForm());
			url.append("?op=").append(METHOD_OP_GET); //$NON-NLS-1$
			url.append("&v=").append(SYNTAX_VERSION); //$NON-NLS-1$
			url.append("&id=").append(this.fileId); //$NON-NLS-1$

			Log.i(ES_GOB_AFIRMA, "URL: " + url); //$NON-NLS-1$

			// Llamamos al servicio para guardar los datos
			data = this.readUrl(url.toString(), UrlHttpMethod.POST);

			if (ERROR_PREFIX.equalsIgnoreCase(new String(data, 0, 4, DEFAULT_URL_ENCODING))) {

				// Si el problema es que no hay datos, lo reeintentamos hasta un maximo de veces
				if (new String(data).startsWith(ERROR_NO_DATA)) {

					Log.i(ES_GOB_AFIRMA, "Los datos no estaban disponibles en servidor"); //$NON-NLS-1$

					if (this.downloadTries < MAX_DOWNLOAD_TRIES) {
						this.downloadTries++;
						try {
							Thread.sleep(2000);
						}
						catch (final Exception e) {
							Log.i(ES_GOB_AFIRMA, "No se pudo realizar la espera entre tiempos de descarga"); //$NON-NLS-1$
						}
						return doInBackground(arg0);
					}
				}

				this.errorMessage = "El servidor devolvio el siguiente error al descargar los datos: " + new String(data, DEFAULT_URL_ENCODING); //$NON-NLS-1$
				this.errorThowable = new IOException(this.errorMessage);
				return null;
			}
		}
		catch (final IOException e) {
			this.errorMessage = "No se pudo conectar con el servidor intermedio"; //$NON-NLS-1$
			this.errorThowable = e;
			return null;
		}
		catch (final AOCancelledOperationException e) {
			this.errorMessage = "Se cancelo la descarga de los datos"; //$NON-NLS-1$
			this.errorThowable = e;
			return null;
		}
		catch (final Throwable e) {
			this.errorMessage = "Error desconocido durante la descarga de datos: " + e; //$NON-NLS-1$
			this.errorThowable = e;
			return null;
		}

		// Comprobamos que la tarea no se haya cancelado
		if (isCancelled()) {
			this.errorMessage = "Se ha cancelado la tarea de descarga"; //$NON-NLS-1$
			this.errorThowable = new AOCancelledOperationException(this.errorMessage);
			return null;
		}

		Log.i(ES_GOB_AFIRMA, "Descarga de datos finalizada"); //$NON-NLS-1$

		return data;
	}

	@Override
	protected void onPostExecute(final byte[] result) {

		if (result != null) {
			this.ddListener.onDownloadingDataSuccess(result);
		}
		else if (this.errorMessage != null) {
			this.ddListener.onDownloadingDataError(this.errorMessage, this.errorThowable);
		}
		else {
			Log.e(ES_GOB_AFIRMA, "La actividad de descarga ha finalizado sin obtener resultados"); //$NON-NLS-1$
		}
	}

	/** Interfaz para la notificaci&oacute;n de finalizaci&oacute;n de la
	 * tarea de descarga del fichero. */
	public interface DownloadDataListener {

		/** Procesa los datos obtenidos de la descarga del fichero.
		 * @param data Datos obtenidos de la descarga del fichero */
		void onDownloadingDataSuccess(byte[] data);

		/** Se ejecuta al producirse un error durante la descarga de datos.
		 * @param msg Mensaje del error
		 * @param t Error producido */
		void onDownloadingDataError(String msg, Throwable t);
	}
}
