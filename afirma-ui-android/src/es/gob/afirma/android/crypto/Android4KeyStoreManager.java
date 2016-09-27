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

import android.app.Activity;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.util.Log;

import java.security.KeyStore.PrivateKeyEntry;

import es.gob.afirma.core.AOCancelledOperationException;

/** Gestor simple de claves y certificados para dispositivos Android 4.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
public final class Android4KeyStoreManager implements MobileKeyStoreManager {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

    private final Activity activity;

    Activity getActivity() {
        return this.activity;
    }

    /** Construye un gestor simple de claves y certificados para dispositivos Android 4.
     * @param act Actividad padre de la aplicaci&oacute;n padre */
    public Android4KeyStoreManager(final Activity act) {
        if (act == null) {
            throw new IllegalArgumentException(
        		"Es necesaria una actividad padre para mostrar los dialogos de seleccion de certificado" //$NON-NLS-1$
            );
        }
        this.activity = act;
    }

    /** {@inheritDoc} */
    @Override
    public void getPrivateKeyEntryAsynchronously(final PrivateKeySelectionListener pksl) {
        if (pksl == null) {
            throw new IllegalArgumentException("La clase a notificar la seleccion de clave no puede ser nula"); //$NON-NLS-1$
        }
        KeyChain.choosePrivateKeyAlias(
    		this.activity,
    		new KeyChainAliasCallback() {
		        /** {@inheritDoc} */
		        @Override
		        public void alias(final String alias) {
		        	if (alias != null) {
			            try {
			                pksl.keySelected(
		                		new KeySelectedEvent(
	                				new PrivateKeyEntry(
	            						KeyChain.getPrivateKey(
	        								Android4KeyStoreManager.this.getActivity(),
			                                alias
		                                ),
			                            KeyChain.getCertificateChain(
		                            		Android4KeyStoreManager.this.getActivity(),
			                                alias
		                                )
	                                )
	            				)
	                		);
			            }
			            catch (final Throwable e) {
			            	Log.e(ES_GOB_AFIRMA, "Error en la obtencion de claves: " + e); //$NON-NLS-1$
			                pksl.keySelected(new KeySelectedEvent(e));
			            }
		        	}
		        	else {
		        		pksl.keySelected(new KeySelectedEvent(new AOCancelledOperationException("El usuario no selecciono un certificado"))); //$NON-NLS-1$
		        	}
		        }
    		},
			new String[] { "RSA" }, // KeyTypes (El paquete "android.security.keystore" no esta disponible hasta el API 23) //$NON-NLS-1$
            null, // Issuers
            null, // Host
            -1, // Port
            null // Alias
        );
    }

}