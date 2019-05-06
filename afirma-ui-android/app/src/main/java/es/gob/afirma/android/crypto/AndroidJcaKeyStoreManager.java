/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.android.crypto;

import android.util.Log;

import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;

/** Gestor simple de claves y certificados para dispositivos Android 2 y 3.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
public final class AndroidJcaKeyStoreManager implements MobileKeyStoreManager {

	private PrivateKeyEntry pke = null;
	private Exception pkeException = null;

	/** Construye un gestor simple de claves y certificados a partir de un almac&eacute;n JCE/JCA.
	 * @param alias Alias preseleccionado
	 * @param ks KeyStore origen, debe estar previamente inicializado y cargado. */
	public AndroidJcaKeyStoreManager(final String alias, final KeyStore ks) {

		if (ks == null) {
			throw new IllegalArgumentException("El almacen de claves es nulo"); //$NON-NLS-1$
		}

		if (alias == null) {
			throw new IllegalArgumentException("El alias seleccionado es nulo"); //$NON-NLS-1$
		}

		Log.i("es.go.afirma.android", "Alias seleccionado: " + alias); //$NON-NLS-1$ //$NON-NLS-2$

		try {
			//Pasamos null para que utiliza el callback handler definido en vez de el password callback
			this.pke = (PrivateKeyEntry) ks.getEntry(alias, null);
		}
		catch (final Exception e) {
			Log.e("es.gob.afirma", "Error obteniendo la entrada a la clave privada: " + e); //$NON-NLS-1$ //$NON-NLS-2$
			this.pkeException = e;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void getPrivateKeyEntryAsynchronously(final PrivateKeySelectionListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("La clase a notificar la seleccion de clave no puede ser nula"); //$NON-NLS-1$
		}
		if (this.pkeException != null) {
			listener.keySelected(new SelectCertificateEvent(this.pkeException));
		}
		else {
			listener.keySelected(new SelectCertificateEvent(this.pke));
		}
	}

	@Override
	public void getCertificateChainAsynchronously(CertificateSelectionListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("La clase a notificar la seleccion de certificado no puede ser nula"); //$NON-NLS-1$
		}
		if (this.pkeException != null) {
			listener.certificateSelected(new SelectCertificateEvent(this.pkeException));
		}
		else {
			listener.certificateSelected(new SelectCertificateEvent(this.pke.getCertificateChain()));
		}
	}
}