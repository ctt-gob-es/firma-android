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

import java.io.IOException;
import java.util.Properties;

import es.gob.afirma.android.Logger;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.misc.http.UrlHttpManagerImpl;
import es.gob.afirma.core.misc.http.UrlHttpMethod;

/** Tarea para el env&iacute;o de datos al servidor de intercambio. Si la entrega de estos datos es
 * cr&iacute;tica para la correcta ejecuc&iacute;n del procedimiento, la tarea tratar&aacute; de
 * finalizar la actividad.
 * @author Carlos Gamuci */
public final class SendDataTask extends BasicHttpTransferDataTask {

	private static final String METHOD_OP_PUT = "put"; //$NON-NLS-1$

	private static final String SYNTAX_VERSION = "1_0"; //$NON-NLS-1$

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private final String id;
	private final String servletUrl;
	private final Properties properties;
	private final String dataB64;
	private final SendDataListener listener;
	private final boolean critical;
	private Throwable error = null;

	/** Crea la tarea con los datos necesarios para el intercambio, permitiendo que se indique si la
	 * entrega de estos datos es un proceso cr&iacute;tico para la ejecuci&oacute;n del procedimiento.
	 * @param id Identificador del intercambio.
	 * @param servletUrl URL del servlet para la subida de datos.
	 * @param dataB64 Datos en base 64 que se desean enviar.
	 * @param listener Clase a la que se notifica el resultado del env&iacute;o de datos
	 * @param critical {@code true} si el procedimiento es cr&iacute;tico, {@code false} en caso contrario.
	 */
	public SendDataTask(final String id, final String servletUrl, final String dataB64,
						final SendDataListener listener, final boolean critical) {
		this(id, servletUrl, dataB64, null, listener, critical);
	}

	/** Crea la tarea con los datos necesarios para el intercambio, permitiendo que se indique si la
	 * entrega de estos datos es un proceso cr&iacute;tico para la ejecuci&oacute;n del procedimiento.
	 * @param id Identificador del intercambio.
	 * @param servletUrl URL del servlet para la subida de datos.
	 * @param dataB64 Datos en base 64 que se desean enviar.
	 * @param properties Propiedades adicionales.
	 * @param listener Clase a la que se notifica el resultado del env&iacute;o de datos
	 * @param critical {@code true} si el procedimiento es cr&iacute;tico, {@code false} en caso contrario.
	 */
	public SendDataTask(final String id, final String servletUrl, final String dataB64,
						final Properties properties, final SendDataListener listener, final boolean critical) {
		this.id = id;
		this.servletUrl = servletUrl;
		this.dataB64 = dataB64;
		this.properties = new Properties();
		if (properties != null)
			this.properties.putAll(properties);
		this.listener = listener;
		this.critical = critical;
	}

	@Override
	protected byte[] doInBackground(final Void... arg0) {

		final byte[] result;
		try {
			final StringBuilder url = new StringBuilder(this.servletUrl);
			url.append("?op=").append(METHOD_OP_PUT); //$NON-NLS-1$
			url.append("&v=").append(SYNTAX_VERSION); //$NON-NLS-1$
			url.append("&id=").append(this.id); //$NON-NLS-1$
			url.append("&dat=").append(this.dataB64); //$NON-NLS-1$

			// Llamamos al servicio para guardar los datos
			result = readUrl(url.toString(), UrlHttpManagerImpl.DEFAULT_TIMEOUT, UrlHttpMethod.POST, properties);
		}
		catch (final IOException e) {
			Logger.e(ES_GOB_AFIRMA, "No se pudo conectar con el servidor intermedio para el envio de datos: " + e); //$NON-NLS-1$
			this.error = e;
			return null;
		}
		catch (final AOCancelledOperationException e) {
			Logger.e(ES_GOB_AFIRMA, "Se cancelo el envio de datos: " + e); //$NON-NLS-1$
			this.error = e;
			return null;
		}

		return result;
	}

	@Override
	protected void onPostExecute(final byte[] result) {
		super.onPostExecute(result);

		if (result != null) {
			this.listener.onSendingDataSuccess(result, this.critical);
		} else {
			this.listener.onSendingDataError(this.error, this.critical);
		}

	}

	/** Listener para el manejo del resultado devuelto por la tarea de envio de datos al servidor. */
	public interface SendDataListener {

		/** Llamada cuando el env&iacute;o termino satisfactoriamente.
		 * @param result Resultado del servidor
		 * @param critical <code>true</code> si es un resultado cr&iacute;tico, <code>false</code>
		 *                 en caso contrario */
		void onSendingDataSuccess(byte[] result, boolean critical);

		/** Llamada cuando el env&iacute;o termin&oacute; en error
		 * @param error Error que se ha producido
		 * @param critical <code>true</code> si es un error cr&iacute;tico, <code>false</code>
		 *                 en caso contrario */
		void onSendingDataError(Throwable error, boolean critical);

	}

}
