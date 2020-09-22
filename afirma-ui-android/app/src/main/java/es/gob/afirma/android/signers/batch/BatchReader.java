/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 *
 * Este fichero es parte de Server triphase signer de ClienteFirma,
 * ha sido adaptado y modificado a partir de SignBatch.java para el funcionamiento
 * de la firma batch en android.
 *
 * Este software sigue los mismos criterios de licencia previos a su
 * modificación: https://github.com/ctt-gob-es/clienteafirma/
 *
 * Modificado por: Eduardo García <eduardo.l.g.g@gmail.com>
 * Fecha modificación: 10 sep 2020
 */

package es.gob.afirma.android.signers.batch;

import android.util.Log;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import es.gob.afirma.android.server.triphase.signer.SignBatchConfig;
import es.gob.afirma.android.signers.batch.signer.SignBatchXmlHandler;
import es.gob.afirma.android.signers.batch.signer.SingleSign;
import es.gob.afirma.android.signers.constants.SignAlgorithm;

public class BatchReader {
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

    protected List<SingleSign> signs;
    protected SignAlgorithm algorithm;
    protected long concurrentTimeout = Long.MAX_VALUE;
    protected boolean stopOnError = false;

    private String id;

    String getId() {
        return this.id;
    }

    void setId(final String i) {
        if (i != null) {
            this.id = i;
        }
    }

    public List<SingleSign> getSigns() {
        return signs;
    }

    /** Obtiene el algoritmo de firma.
     * @return Algoritmo de firma. */
    public SignAlgorithm getSignAlgorithm() {
        return this.algorithm;
    }

    public long getConcurrentTimeout() {
        return concurrentTimeout;
    }

    public boolean isStopOnError() {
        return stopOnError;
    }

    public void parse(final byte[] xml) throws IOException {
        if (xml == null || xml.length < 1) {
            throw new IllegalArgumentException(
                    "El XML de definicion de lote de firmas no puede ser nulo ni vacio" //$NON-NLS-1$
            );
        }

        // Definimos un manejador que extraera la informacion del XML
        final SignBatchXmlHandler handler = new SignBatchXmlHandler();

        try (final InputStream is = new ByteArrayInputStream(xml)) {

            final SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            final SAXParser saxParser = spf.newSAXParser();
            final XMLReader xmlReader = saxParser.getXMLReader();

            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(is));
        }
        catch (final Exception e) {
            Log.e(ES_GOB_AFIRMA,"Error al cargar el fichero XML de definicion de lote: " + e + //$NON-NLS-1$
                    "\n" + new String(xml, DEFAULT_CHARSET)); //$NON-NLS-1$
            throw new IOException("Error al cargar el fichero XML de definicion de lote: " + e, e); //$NON-NLS-1$
        }

        final SignBatchConfig config = handler.getBatchConfig();

        this.id = config.getId() != null ? config.getId() : UUID.randomUUID().toString();
        this.algorithm = config.getAlgorithm();
        this.concurrentTimeout = config.getConcurrentTimeout();
        this.stopOnError = config.isStopOnError();
        this.signs = config.getSingleSigns();

    }
}
