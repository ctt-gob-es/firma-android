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


import java.util.Dictionary;
import java.util.Hashtable;

final class ErrorManager {

	private static final String ERROR_NUMBER = "%#%"; //$NON-NLS-1$
	private static final String ERROR_MESSAGE = "%MSG%"; //$NON-NLS-1$
	private static final String ERROR_TEMPLATE = ERROR_NUMBER + ":=" + ERROR_MESSAGE; //$NON-NLS-1$

	private static final String GENERIC_ERROR = "Error generico"; //$NON-NLS-1$

	static final String ERROR_MISSING_OPERATION_NAME     = "ERR-00"; //$NON-NLS-1$
	static final String ERROR_UNSUPPORTED_OPERATION_NAME = "ERR-01"; //$NON-NLS-1$
	static final String ERROR_MISSING_DATA               = "ERR-02"; //$NON-NLS-1$
	static final String ERROR_BAD_XML                    = "ERR-03"; //$NON-NLS-1$
	static final String ERROR_BAD_CERTIFICATE            = "ERR-04"; //$NON-NLS-1$
	static final String ERROR_MISSING_DATA_ID            = "ERR-05"; //$NON-NLS-1$
	static final String ERROR_INVALID_DATA_ID            = "ERR-06"; //$NON-NLS-1$
	static final String ERROR_INVALID_DATA               = "ERR-07"; //$NON-NLS-1$
	static final String ERROR_MISSING_SERVLET      		 = "ERR-08"; //$NON-NLS-1$
	static final String ERROR_INVALID_SERVLET        	 = "ERR-09"; //$NON-NLS-1$
	static final String ERROR_NOT_SUPPORTED_FORMAT       = "ERR-10"; //$NON-NLS-1$
	static final String ERROR_CANCELLED_OPERATION        = "ERR-11"; //$NON-NLS-1$
	static final String ERROR_CODING_BASE64				 = "ERR-12"; //$NON-NLS-1$
	static final String ERROR_PKE       				 = "ERR-13"; //$NON-NLS-1$
	static final String ERROR_SIGNING       			 = "ERR-14"; //$NON-NLS-1$
	static final String ERROR_INVALID_CIPHER_KEY         = "ERR-15"; //$NON-NLS-1$
	static final String ERROR_CIPHERING			         = "ERR-16"; //$NON-NLS-1$
	static final String ERROR_NO_CERT_SELECTED			 = "ERR-17"; //$NON-NLS-1$
	static final String ERROR_COMMUNICATING_WITH_WEB	 = "ERR-18"; //$NON-NLS-1$
	static final String ERROR_PKE_ANDROID_4_1			 = "ERR-21"; //$NON-NLS-1$
	static final String ERROR_BAD_PARAMETERS			 = "ERR-22"; //$NON-NLS-1$
	static final String ERROR_ESTABLISHING_KEYSTORE		 = "ERR-23"; //$NON-NLS-1$
	static final String ERROR_MSC_PIN					 = "ERR-24"; //$NON-NLS-1$
	static final String ERROR_SELECTING_CERTIFICATE		 = "ERR-25"; //$NON-NLS-1$
	static final String ERROR_SAVING_DATA			 = "ERR-26"; //$NON-NLS-1$


	private static final Dictionary<String, String> ERRORS = new Hashtable<>();
	static {
		ERRORS.put(ERROR_MISSING_OPERATION_NAME, "No se ha indicado codigo de operacion"); //$NON-NLS-1$
		ERRORS.put(ERROR_UNSUPPORTED_OPERATION_NAME, "Codigo de operacion no soportado"); //$NON-NLS-1$
		ERRORS.put(ERROR_MISSING_DATA, "No se han proporcionado los datos de la operacion"); //$NON-NLS-1$
		ERRORS.put(ERROR_BAD_XML, "Se ha recibido un XML mal formado"); //$NON-NLS-1$
		ERRORS.put(ERROR_BAD_CERTIFICATE, "Se ha recibido un certificado corrupto"); //$NON-NLS-1$
		ERRORS.put(ERROR_MISSING_DATA_ID, "No se ha proporcionado un identificador para los datos"); //$NON-NLS-1$
		ERRORS.put(ERROR_INVALID_DATA_ID, "El identificador para los datos es invalido"); //$NON-NLS-1$
		ERRORS.put(ERROR_INVALID_DATA, "Los datos solicitados o enviados son invalidos"); //$NON-NLS-1$
		ERRORS.put(ERROR_MISSING_SERVLET, "No se ha proporcionado el sevlet para la comunicacion de los datos"); //$NON-NLS-1$
		ERRORS.put(ERROR_INVALID_SERVLET, "La ruta del servlet es invalida"); //$NON-NLS-1$
		ERRORS.put(ERROR_NOT_SUPPORTED_FORMAT, "Se ha configurado un formato de firma no soportado"); //$NON-NLS-1$
		ERRORS.put(ERROR_CANCELLED_OPERATION, "Operacion cancelada"); //$NON-NLS-1$
		ERRORS.put(ERROR_CODING_BASE64, "Error en la codificacion del base 64"); //$NON-NLS-1$
		ERRORS.put(ERROR_PKE, "No se pudo recuperar la clave del certificado"); //$NON-NLS-1$
		ERRORS.put(ERROR_SIGNING, "Ocurrio un error en la operacion de firma"); //$NON-NLS-1$
		ERRORS.put(ERROR_INVALID_CIPHER_KEY, "La clave de cifrado proporcionada no es valida"); //$NON-NLS-1$
		ERRORS.put(ERROR_CIPHERING, "Error durante el proceso de cifrado de los datos"); //$NON-NLS-1$
		ERRORS.put(ERROR_NO_CERT_SELECTED, "No se selecciono ningun certificado de firma"); //$NON-NLS-1$
		ERRORS.put(ERROR_COMMUNICATING_WITH_WEB, "Error de comunicacion con el servicio"); //$NON-NLS-1$
		ERRORS.put(ERROR_PKE_ANDROID_4_1, "Android 4.1 y 4.1.1 no permiten que los nombres de certificados contengan caracteres especiales (espacios, guiones...). Modifique el alias de sus certificados al importarlos para evitar este error."); //$NON-NLS-1$
		ERRORS.put(ERROR_BAD_PARAMETERS, "No se ha realizado correctamente la invocacion de la aplicacion"); //$NON-NLS-1$
		ERRORS.put(ERROR_ESTABLISHING_KEYSTORE, "No se ha podido establecer un almacen de certificados para su uso"); //$NON-NLS-1$
		ERRORS.put(ERROR_MSC_PIN, "Error en la firma. Compruebe que el PIN de su dispositivo criptografico es correcto. Varios intentos incorrectos pueden bloquearlo."); //$NON-NLS-1$
		ERRORS.put(ERROR_SELECTING_CERTIFICATE, "Ocurrio un error en la seleccion del certificado"); //$NON-NLS-1$
		ERRORS.put(ERROR_SAVING_DATA, "Error al guardar los datos en el dispositivo"); //$NON-NLS-1$
	}

	private ErrorManager() {
		// No instanciable
	}

	static String genError(final String number) {
		return genError(number, null);
	}

	static String genError(final String number, final String msg) {
		return
				ERROR_TEMPLATE.replace(ERROR_NUMBER, number).replace(
						ERROR_MESSAGE,
						msg != null ? msg : ERRORS.get(number) != null ? ERRORS.get(number) : GENERIC_ERROR
						);
	}
}
