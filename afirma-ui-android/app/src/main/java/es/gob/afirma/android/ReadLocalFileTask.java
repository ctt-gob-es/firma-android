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

import android.os.AsyncTask;

import java.io.FileInputStream;

import es.gob.afirma.core.misc.AOUtil;

final class ReadLocalFileTask extends AsyncTask<String, Void, Object> {

	private final ReadLocalFileListener listener;

	interface ReadLocalFileListener {
		void setData(final Object data);
	}

	ReadLocalFileTask(final ReadLocalFileListener list) {
		if (list == null) {
			throw new IllegalArgumentException("Es obligatorio indicar a quien trasladar los datos leidos"); //$NON-NLS-1$
		}
		this.listener = list;
	}

	@Override
	protected Object doInBackground(final String... files) {
		final byte[] dataToSign;
		try {
			final FileInputStream fis = new FileInputStream(files[0]);
			dataToSign = AOUtil.getDataFromInputStream(fis);
			fis.close();
		}
		catch(final OutOfMemoryError e) {
			Logger.e("es.gob.afirma", "El fichero a firmar es demasiado grande: " + e); //$NON-NLS-1$ //$NON-NLS-2$
			return e;
		}
		catch (final Exception e) {
			Logger.e("es.gob.afirma", "Error leyendo el fichero a firmar: " + e); //$NON-NLS-1$ //$NON-NLS-2$
			return e;
		}
		return dataToSign;
	}

	@Override
	protected void onPostExecute(final Object data) {
		super.onPostExecute(data);
		this.listener.setData(data);
	}

}
