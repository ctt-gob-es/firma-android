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

import android.content.ActivityNotFoundException;
import android.os.AsyncTask;

import java.io.IOException;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.Locale;
import java.util.Properties;

import es.gob.afirma.android.Logger;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.AOUnsupportedSignFormatException;
import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.core.signers.AOSigner;
import es.gob.afirma.core.signers.AOSignerFactory;
import es.gob.afirma.core.signers.CounterSignTarget;

/**
 * Tarea que ejecuta una firma electr&oacute;nica a trav&eacute;s de un AOSigner.
 * La operaci&oacute;n puede ser firma simple, cofirma o contrafirma (de nodos hoja
 * o todo el &aacute;rbol) seg&uacute;n se indique.
 * La firma es una operaci&oacute;n que necesariamente debe ejecutarse en segundo
 * plano ya que las firmas trif&aacute;sicas hacen conexiones de red.
 * @author Carlos Gamuci
 */
public class SignTask extends AsyncTask<Void, Void, SignResult>{

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private static final String COUNTERSIGN_TARGET_KEY = "target"; //$NON-NLS-1$

	private static final String COUNTERSIGN_TARGET_TREE = "tree"; //$NON-NLS-1$

	private static final String SIGN_FORMAT_AUTO = "AUTO"; //$NON-NLS-1$

	public static final String OP_SIGN = "sign";
	public static final String OP_COSIGN = "cosign";
	public static final String OP_COUNTERSIGN = "countersign";

	private final String op;
	private final byte[] data;
	private final String format;
	private final String algorithm;
	private final PrivateKeyEntry pke;
	private final Properties extraParams;

	private final SignListener signListener;

	private Throwable t;

	/** Construye la tarea encargada de realizar la operaci&oacute;n criptogr&aacute;fica.
	 * @param op Identificador de la operaci&oacute;n.
	 * @param data Datos a firma o firma a cofirma/contrafirmar.
	 * @param format Formato de firma.
	 * @param algorithm Algoritmo de firma.
	 * @param pke Clave privada para la firma.
	 * @param extraParams Par&aacute;metros adicionales para la configuraci&oacute;n de la firma.
	 * @param signListener Manejador para el tratamiento del resultado de la firma. */
	public SignTask(final String op,
					final byte[] data,
					final String format,
					final String algorithm,
					final PrivateKeyEntry pke,
					final Properties extraParams,
					final SignListener signListener) {

		this.op = op;
		this.data = data;
		this.format = format;
		this.algorithm = algorithm;
		this.pke = pke;
		this.extraParams = extraParams;
		this.signListener = signListener;
		this.t = null;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected SignResult doInBackground(final Void... params) {

		// Obtenemos el manejador de firma apropiado
		final AOSigner signer = getSupportedCompatibleSigner(this.format, this.op, this.data);
		if (signer == null) {
			this.t = new AOUnsupportedSignFormatException(
				"No se ha indicado un formato de firma soportado o el fichero indicado no se reconoce como fichero de firma" //$NON-NLS-1$
			);
			return null;
		}

		// Generacion de la firma
		byte[] sign = null;
		try {
			// Ejecutamos la operacion pertinente. Si no se indico nada, por defecto, el metodo
			// que devuelve la operacion indica que es firma
			switch (this.op.toLowerCase(Locale.ENGLISH)) {
			case OP_SIGN:
				sign = signer.sign(
						this.data,
						this.algorithm,
						this.pke.getPrivateKey(),
						this.pke.getCertificateChain(),
						this.extraParams
						);
				break;
			case OP_COSIGN:
				sign = signer.cosign(
						this.data,
						this.algorithm,
						this.pke.getPrivateKey(),
						this.pke.getCertificateChain(),
						this.extraParams
						);
				break;
			case OP_COUNTERSIGN:
				CounterSignTarget target = CounterSignTarget.LEAFS;
				if (this.extraParams.containsKey(COUNTERSIGN_TARGET_KEY)) {
					final String targetValue = this.extraParams.getProperty(COUNTERSIGN_TARGET_KEY).trim();
					if (COUNTERSIGN_TARGET_TREE.equals(targetValue)) {
						target = CounterSignTarget.TREE;
					}
				}

				sign = signer.countersign(
					this.data,
					this.algorithm,
					target,
					null,
					this.pke.getPrivateKey(),
					this.pke.getCertificateChain(),
					this.extraParams
				);
				break;
				default:
				throw new IllegalStateException("Tipo de operacion de firma no soportado: " + this.op); //$NON-NLS-1$
			}
		}
		catch (final AOException e) {
			if (e.getCause() instanceof AOException && e.getCause().getCause() instanceof ActivityNotFoundException) {
				// Solo se dara este error (hasta la fecha) cuando se intente cargar el dialogo de PIN de
				// una tarjeta criptografica
				Logger.e(ES_GOB_AFIRMA, "Se ha intentado cargar el dialogo de PIN de una tarjeta criptografica: " + e); //$NON-NLS-1$
				this.t = new MSCBadPinException("Se inserto un PIN incorrecto para la tarjeta critografica", e); //$NON-NLS-1$
			}
			else {
				Logger.e(ES_GOB_AFIRMA, "Error durante la operacion de firma: " + e, e); //$NON-NLS-1$
				this.t = e;
			}
		}
		catch (final Exception e) {
			Logger.e(ES_GOB_AFIRMA, "Error en la firma: " + e, e); //$NON-NLS-1$
			this.t = e;
		}

		// En caso de error, "sign" es nulo y despues se utiliza la excepcion para notificar el problema
		return new SignResult(sign, this.pke.getCertificate());
	}

	private static AOSigner getSupportedCompatibleSigner(final String format, final String operation, final byte[] signature) {

		// La firma XAdES monofasica no esta soportada en Android, asi que pasamos a firma XAdES trifasica
		if (format.toLowerCase(Locale.ENGLISH).startsWith(AOSignConstants.SIGN_FORMAT_XADES.toLowerCase(Locale.ENGLISH))) {
			return AOSignerFactory.getSigner(AOSignConstants.SIGN_FORMAT_XADES_TRI);
		}
		// Si se indica el formato AUTO, intentaremos identificar el formato de la firma
		else if (format.toLowerCase(Locale.ENGLISH).startsWith(AOSignConstants.SIGN_FORMAT_FACTURAE.toLowerCase(Locale.ENGLISH))) {
			return AOSignerFactory.getSigner(AOSignConstants.SIGN_FORMAT_FACTURAE_TRI);
		}
		// Si se indica el formato AUTO, intentaremos identificar el formato de la firma
		else if (format.equalsIgnoreCase(SIGN_FORMAT_AUTO) && (OP_COSIGN.equalsIgnoreCase(operation) || OP_COUNTERSIGN.equalsIgnoreCase(operation))) {
			try {
				return AOSignerFactory.getSigner(signature);
			}
			catch (final IOException e) {
				Logger.e(ES_GOB_AFIRMA, "No se ha podido identificar el formato de la firma, se devolvera un manejador nulo: " + e, e); //$NON-NLS-1$
				return null;
			}
		}
		// En cualquier otro caso obtenemos el manejador del formato indicado
		return AOSignerFactory.getSigner(format);
	}

	@Override
	protected void onPostExecute(final SignResult result) {
		super.onPostExecute(result);

		if (result == null || result.getSignature() == null) {
			this.signListener.onSignError(this.t);
		} else {
			this.signListener.onSignSuccess(result);
		}
	}

	/** Interfaz que debe implementar el manejador del resultado de la operaci&oacute;n de firma.
	 * @author Carlos Gamuci. */
	public interface SignListener {

		/** Gestiona el resultado de la operaci&oacute;n de firma cuando termina correctamente.
		 * @param signature Firma/cofirma/contrafirma generada. */
		void onSignSuccess(SignResult signature);

		/** Gestiona un error en la operaci&oacute;n de firma.
		 * @param t Excepcion o error lanzada en la operaci&oacute;n de firma. */
		void onSignError(Throwable t);
	}
}
