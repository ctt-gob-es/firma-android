/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 *
 * Este fichero es parte de Server triphase signer de ClienteFirma,
 * ha sido adaptado y modificado para el funcionamiento de la firma
 * batch en android.
 *
 * Este software sigue los mismos criterios de licencia previos a su
 * modificación: https://github.com/ctt-gob-es/clienteafirma/
 *
 * Modificado por: Eduardo García <eduardo.l.g.g@gmail.com>
 * Fecha modificación: 10 sep 2020
 */

package es.gob.afirma.android.signers.batch;

public class SingleSignConstants {

    /** Tipo de operaci&oacute;n de firma. */
    public enum SignSubOperation {

        /** Firma. */
        SIGN("sign"), //$NON-NLS-1$

        /** Cofirma. */
        COSIGN("cosign"), //$NON-NLS-1$

        /** Contrafirma. */
        COUNTERSIGN("countersign"); //$NON-NLS-1$

        private final String name;

        private SignSubOperation(final String n) {
            this.name = n;
        }

        @Override
        public String toString() {
            return this.name;
        }

        /** Obtiene el tipo de operaci&oacute;n de firma a partir de su nombre.
         * @param name Nombre del tipo de operaci&oacute;n de firma.
         * @return Tipo de operaci&oacute;n de firma. */
        public static SignSubOperation getSubOperation(final String name) {
            if (SIGN.toString().equalsIgnoreCase(name)) {
                return SIGN;
            }
            if (COSIGN.toString().equalsIgnoreCase(name)) {
                return COSIGN;
            }
            if (COUNTERSIGN.toString().equalsIgnoreCase(name)) {
                return COUNTERSIGN;
            }
            throw new IllegalArgumentException(
                    "Tipo de operacion (suboperation) de firma no soportado: " + name //$NON-NLS-1$
            );
        }
    }
}
