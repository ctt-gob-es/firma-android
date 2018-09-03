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

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;


/** Gestor simple de claves y certificados para dispositivos m&oacute;viles.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
public interface MobileKeyStoreManager {

    /** Inicia un proceso as&iacute;ncrono de selecci&oacute;n de una entrada que apunta a una clave privada.
     * @param e Clase a la que hay que notificar cuando se complete la selecci&oacute;n */
    void getPrivateKeyEntryAsynchronously(final PrivateKeySelectionListener e);

    /** Inicia un proceso as&iacute;ncrono de selecci&oacute;n de certificado.
     * @param e Clase a la que hay que notificar cuando se complete la selecci&oacute;n */
    void getCertificateChainAsynchronously(final CertificateSelectionListener e);

    /** Interfaz para clases que esperen una selecci&oacute;n as&iacute;ncrona de una clave privada. */
    interface PrivateKeySelectionListener {

        /** Notifica que se ha seleccionado una entrada que apunta a una clave privada.
         * @param kse Evento de selecci&oacute;n de una entrada que apunta a una clave privada */
        void keySelected(SelectCertificateEvent kse);
    }

    /** Interfaz para clases que esperen una selecci&oacute;n as&iacute;ncrona de un certificado. */
    interface CertificateSelectionListener {

        /** Notifica que se ha seleccionado un certificado.
         * @param kse Evento de selecci&oacute;n de un certificado. */
        void certificateSelected(SelectCertificateEvent kse);
    }

    /** Evento de selecci&oacute;n de una entrada que apunta a una clave privada. */
    class SelectCertificateEvent {

        private final PrivateKeyEntry pke;
        private final Certificate[] certChain;
        private final Throwable e;

        /** Construye un evento de selecci&oacute;n de certificado.
         * @param p Entrada que apunta al par una clave privada y la cadena de certificaci&oacute;n
         *          del certificado seleccionada */
        public SelectCertificateEvent(final PrivateKeyEntry p) {
            this.pke = p;
            this.certChain = p.getCertificateChain();
            this.e = null;
        }

        /** Construye un evento de selecci&oacute;n de certificado.
         * @param certChain Cadena de certificaci&oacute;n del certificado seleccionado. */
        public SelectCertificateEvent(final Certificate[] certChain) {
            this.pke = null;
            this.certChain = certChain;
            this.e = null;
        }

        /** Construye un evento de selecci&oacute;n fallida de un certificado.
         * @param t Causa del fallo en la selecci&oacute;n */
        SelectCertificateEvent(final Throwable t) {
            this.pke = null;
            this.certChain = null;
            this.e = t;
        }

        /** Obtiene la entrada que apunta a la entrada de certificado seleccionada.
         * @return Entrada del certificado y clave seleccionados.
         * @throws Throwable Si la obtenci&oacute;n de la clave privada produjo algun error */
        public PrivateKeyEntry getPrivateKeyEntry() throws Throwable {
            if (this.e != null) {
                throw this.e;
            }
            return this.pke;
        }

        /** Obtiene la cadena de certificaci&oacute;n del certificado seleccionado.
         * @return Cadena de certificaci&oacute;n del certificado seleccionados.
         * @throws Throwable Si la obtenci&oacute;n de la clave privada produjo algun error */
        public Certificate[] getCertChain() throws Throwable {
            if (this.e != null) {
                throw this.e;
            }
            return this.certChain;
        }
    }
}