/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 10 sep 2020
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.signers.batch;

import java.io.IOException;
import java.util.Properties;

import es.gob.afirma.android.signers.batch.signer.SignSaver;
import es.gob.afirma.android.signers.batch.signer.SingleSign;

/**
 * Implementación de SignSaverHttpPost para android.
 */
public class SignSaverHttpPost implements SignSaver {
    private Properties config;
    private boolean initialized = false;

    @Override
    public void saveSign(SingleSign sign, byte[] dataToSave) throws IOException {
        // No es necesario
        throw new IOException("Esta operación no es válida");
    }

    @Override
    public void rollback(SingleSign sign) {
        // TODO
    }

    @Override
    public void init(Properties config) {
        this.config = config;
        this.initialized = true;
    }

    @Override
    public Properties getConfig() {
        return this.config;
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }
}
