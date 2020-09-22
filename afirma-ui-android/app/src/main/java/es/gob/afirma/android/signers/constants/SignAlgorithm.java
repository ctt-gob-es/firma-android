/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 *
 * Este fichero es parte de Server triphase signer de ClienteFirma,
 * ha sido adaptado y modificado a partir de la clase SingleSignConstants
 * para el funcionamiento de la firma batch en android.
 *
 * Este software sigue los mismos criterios de licencia previos a su
 * modificación: https://github.com/ctt-gob-es/clienteafirma/
 *
 * Modificado por: Eduardo García <eduardo.l.g.g@gmail.com>
 * Fecha modificación: 10 sep 2020
 */

package es.gob.afirma.android.signers.constants;

import es.gob.afirma.core.signers.AOSignConstants;

public enum SignAlgorithm {

    /** SHA1withRSA. */
    SHA1WITHRSA(AOSignConstants.SIGN_ALGORITHM_SHA1WITHRSA),

    /** SHA256withRSA. */
    SHA256WITHRSA(AOSignConstants.SIGN_ALGORITHM_SHA256WITHRSA),

    /** SHA284withRSA. */
    SHA384WITHRSA(AOSignConstants.SIGN_ALGORITHM_SHA384WITHRSA),

    /** SHA512withRSA. */
    SHA512WITHRSA(AOSignConstants.SIGN_ALGORITHM_SHA512WITHRSA);

    private final String name;

    private SignAlgorithm(final String n) {
        this.name = n;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /** Obtiene el algoritmo de firma a partir de su nombre.
     * @param name Nombre del algoritmo de firma.
     * @return Algoritmo firma. */
    public static SignAlgorithm getAlgorithm(final String name) {
        if (SHA1WITHRSA.toString().equalsIgnoreCase(name)) {
            return SHA1WITHRSA;
        }
        if (SHA256WITHRSA.toString().equalsIgnoreCase(name)) {
            return SHA256WITHRSA;
        }
        if (SHA384WITHRSA.toString().equalsIgnoreCase(name)) {
            return SHA384WITHRSA;
        }
        if (SHA512WITHRSA.toString().equalsIgnoreCase(name)) {
            return SHA512WITHRSA;
        }
        throw new IllegalArgumentException(
                "Tipo de algoritmo de firma no soportado: " + name //$NON-NLS-1$
        );
    }
}
