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


/**
 * Excepci&oacute;n que identifica un error en la inserci&oacute;n del PIN de una tarjeta
 * cript&oacute;grafica interna.
 * @author Carlos Gamuci
 */
public class MSCBadPinException extends Exception {

	/** Serial ID. */
	private static final long serialVersionUID = -6530167992401963061L;

	/**
	 * Crea una excepci&oacute;n asociada a la inserci&oacute;n de un PIN incorrecto
	 * para el uso de una tarjeta criptogr&aacute;fica interna.
	 * @param msg Mensaje.
	 * @param t Excepci&oacute;n que caus&oacute; a esta.
	 */
	public MSCBadPinException(final String msg, final Throwable t) {
		super(msg, t);
	}

}
