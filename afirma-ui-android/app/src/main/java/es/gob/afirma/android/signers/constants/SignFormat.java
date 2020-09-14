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

/** Formato de firma. */
public enum SignFormat {

    /** CAdES. */
    CADES(AOSignConstants.SIGN_FORMAT_CADES),

    /** CAdES ASiC. */
    CADES_ASIC(AOSignConstants.SIGN_FORMAT_CADES_ASIC_S),

    /** XAdES. */
    XADES(AOSignConstants.SIGN_FORMAT_XADES),

    /** XAdES ASiC. */
    XADES_ASIC(AOSignConstants.SIGN_FORMAT_XADES_ASIC_S),

    /** PAdES. */
    PADES(AOSignConstants.SIGN_FORMAT_PADES),

    /** FacturaE. */
    FACTURAE(AOSignConstants.SIGN_FORMAT_FACTURAE),

    /** PKCS#1. */
    PKCS1(AOSignConstants.SIGN_FORMAT_PKCS1);

    private final String name;

    private SignFormat(final String n) {
        this.name = n;
    }

    @Override
    public String toString() {
        return this.name;
    }

    /** Obtiene el formato de firma a partir de su nombre.
     * @param name Nombre del formato de firma.
     * @return Formato firma. */
    public static SignFormat getFormat(final String name) {
        if (name != null) {
            if (CADES.toString().equalsIgnoreCase(name.trim())) {
                return CADES;
            }
            if (XADES.toString().equalsIgnoreCase(name.trim())) {
                return XADES;
            }
            if (PADES.toString().equalsIgnoreCase(name.trim())) {
                return PADES;
            }
            if (FACTURAE.toString().equalsIgnoreCase(name.trim())) {
                return FACTURAE;
            }
        }
        throw new IllegalArgumentException(
                "Tipo de formato de firma no soportado: " + name //$NON-NLS-1$
        );
    }
}
