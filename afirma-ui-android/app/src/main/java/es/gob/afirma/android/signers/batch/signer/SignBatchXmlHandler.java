/*
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
package es.gob.afirma.android.signers.batch.signer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.Properties;

import es.gob.afirma.android.signers.constants.SignAlgorithm;
import es.gob.afirma.android.signers.constants.SignFormat;
import es.gob.afirma.android.signers.batch.SingleSignConstants;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.android.server.triphase.signer.SignBatchConfig;

public class SignBatchXmlHandler extends DefaultHandler {

    private static final String NODE_NAME_SIGNBATCH = "signbatch"; //$NON-NLS-1$

    private static final String NODE_NAME_SINGLESIGN = "singlesign"; //$NON-NLS-1$

    private static final String NODE_NAME_DATASOURCE = "datasource"; //$NON-NLS-1$

    private static final String NODE_NAME_FORMAT = "format"; //$NON-NLS-1$

    private static final String NODE_NAME_SUBOPERATIONS = "suboperation"; //$NON-NLS-1$

    private static final String NODE_NAME_EXTRAPARAMS = "extraparams"; //$NON-NLS-1$

    private static final String NODE_NAME_SIGNSAVER = "signsaver"; //$NON-NLS-1$

    private static final String NODE_NAME_CLASS = "class"; //$NON-NLS-1$

    private static final String NODE_NAME_CONFIG = "config"; //$NON-NLS-1$

    private static final String ATTR_ID = "Id"; //$NON-NLS-1$

    private static final String ATTR_STOPONERROR = "stoponerror"; //$NON-NLS-1$

    private static final String ATTR_ALGORITHM = "algorithm"; //$NON-NLS-1$

    private static final String ATTR_CONCURRENT_TIMEOUT = "concurrenttimeout"; //$NON-NLS-1$

    private static final int UNDEFINED = 0;

    private static final int ELEMENT_SIGNBATCH = 1;
    private static final int ELEMENT_SINGLESIGN = 2;
    private static final int ELEMENT_DATASOURCE = 3;
    private static final int ELEMENT_FORMAT = 4;
    private static final int ELEMENT_SUBOPERATION = 5;
    private static final int ELEMENT_EXTRAPARAMS = 6;
    private static final int ELEMENT_CLASS = 7;
    private static final int ELEMENT_SIGNSAVER = 8;
    private static final int ELEMENT_CONFIG = 9;

    private int parent;

    private SignBatchConfig batchConfig;

    private SingleSign currentSign;

    private int currentElement;

    private SignSaver currentSignSaver;

    private CharArrayWriter acumulateContent;

    @Override
    public void startDocument() throws SAXException {
        this.batchConfig = new SignBatchConfig();
        this.parent = UNDEFINED;
        this.currentElement = UNDEFINED;
        this.currentSignSaver = null;
        this.acumulateContent = new CharArrayWriter();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

        switch (localName) {
            case NODE_NAME_SIGNBATCH:
                if (this.parent != UNDEFINED) {
                    throw new SAXException(String.format("El nodo %s debe ser el primero del XML", NODE_NAME_SIGNBATCH)); //$NON-NLS-1$
                }
                this.parent = ELEMENT_SIGNBATCH;
                this.currentElement = ELEMENT_SIGNBATCH;

                // Algoritmo de firma (Obligatorio)
                if (attributes != null && attributes.getValue(ATTR_ALGORITHM) != null) {
                    this.batchConfig.setAlgorithm(SignAlgorithm.getAlgorithm(attributes.getValue(ATTR_ALGORITHM)));
                }
                else {
                    throw new SAXException(String.format("El nodo %s debe contener al menos el atributo de algoritmo", NODE_NAME_SIGNBATCH)); //$NON-NLS-1$
                }
                // Para en caso de error (Opcional). Por defecto, true
                if (attributes.getValue(ATTR_STOPONERROR) != null) {
                    this.batchConfig.setStopOnError(Boolean.parseBoolean(attributes.getValue(ATTR_STOPONERROR)));
                }
                // Tiempo de espera (Opcional). Por defecto, 0
                if (attributes.getValue(ATTR_CONCURRENT_TIMEOUT) != null) {
                    try {
                        this.batchConfig.setConcurrentTimeout(Long.parseLong(attributes.getValue(ATTR_CONCURRENT_TIMEOUT)));
                    }
                    catch (final Exception e) {
                        throw new SAXException("Se ha establecido un valor invalido para la espera maxima", e); //$NON-NLS-1$
                    }
                }
                // Identificador de lote (Opcional)
                if (attributes.getValue(ATTR_ID) != null) {
                    this.batchConfig.setId(attributes.getValue(ATTR_ID));
                }
                break;
            case NODE_NAME_SINGLESIGN:
                if (this.parent != ELEMENT_SIGNBATCH) {
                    throw new SAXException(String.format("El nodo %s debe estar contenido en un nodo %s", NODE_NAME_SINGLESIGN, NODE_NAME_SIGNBATCH)); //$NON-NLS-1$
                }
                this.parent = ELEMENT_SINGLESIGN;
                this.currentElement = ELEMENT_SINGLESIGN;
                if (attributes == null || attributes.getValue(ATTR_ID) == null) {
                    throw new SAXException(String.format("No se ha indicar el atributo %s de una de las firmas", ATTR_ID)); //$NON-NLS-1$
                }
                this.currentSign = new SingleSign(attributes.getValue(ATTR_ID));
                break;
            case NODE_NAME_DATASOURCE:
                if (this.parent != ELEMENT_SINGLESIGN) {
                    throw new SAXException(String.format("El nodo %s debe estar contenido en un nodo %s", NODE_NAME_DATASOURCE, NODE_NAME_SINGLESIGN)); //$NON-NLS-1$
                }
                this.currentElement = ELEMENT_DATASOURCE;
                break;
            case NODE_NAME_FORMAT:
                if (this.parent != ELEMENT_SINGLESIGN) {
                    throw new SAXException(String.format("El nodo %s debe estar contenido en un nodo %s", NODE_NAME_FORMAT, NODE_NAME_SINGLESIGN)); //$NON-NLS-1$
                }
                this.currentElement = ELEMENT_FORMAT;
                break;
            case NODE_NAME_SUBOPERATIONS:
                if (this.parent != ELEMENT_SINGLESIGN) {
                    throw new SAXException(String.format("El nodo %s debe estar contenido en un nodo %s", NODE_NAME_SUBOPERATIONS, NODE_NAME_SINGLESIGN)); //$NON-NLS-1$
                }
                this.currentElement = ELEMENT_SUBOPERATION;
                break;
            case NODE_NAME_EXTRAPARAMS:
                if (this.parent != ELEMENT_SINGLESIGN) {
                    throw new SAXException(String.format("El nodo %s debe estar contenido en un nodo %s", NODE_NAME_EXTRAPARAMS, NODE_NAME_SINGLESIGN)); //$NON-NLS-1$
                }
                this.currentElement = ELEMENT_EXTRAPARAMS;
                break;
            case NODE_NAME_SIGNSAVER:
                if (this.parent != ELEMENT_SINGLESIGN) {
                    throw new SAXException(String.format("El nodo %s debe estar contenido en un nodo %s", NODE_NAME_SIGNSAVER, NODE_NAME_SINGLESIGN)); //$NON-NLS-1$
                }
                this.parent = ELEMENT_SIGNSAVER;
                this.currentElement = ELEMENT_SIGNSAVER;
                break;
            case NODE_NAME_CLASS:
                if (this.parent != ELEMENT_SIGNSAVER) {
                    throw new SAXException(String.format("El nodo %s debe estar contenido en un nodo %s", NODE_NAME_CLASS, NODE_NAME_CONFIG)); //$NON-NLS-1$
                }
                this.currentElement = ELEMENT_CLASS;
                break;
            case NODE_NAME_CONFIG:
                if (this.parent != ELEMENT_SIGNSAVER) {
                    throw new SAXException(String.format("El nodo %s debe estar contenido en un nodo %s", NODE_NAME_CONFIG, NODE_NAME_CONFIG)); //$NON-NLS-1$
                }
                this.currentElement = ELEMENT_CONFIG;
                break;

            default:
                throw new SAXException("Nodo no reconocido: " + localName); //$NON-NLS-1$
        }
    }

    @Override
    public void characters(final char ch[], final int start, final int length) throws SAXException {
        this.acumulateContent.write(ch, start, length);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {

        // Realizamos la accion concreta para el elemento actual. Sin embargo, esta variable
        // solo tendra el valor adecuado cuando el nodo no contuviese otro dentro. En caso
        // contrario, estaria como indefinido. Asi pues, los nodos indefinidos tendran que
        // tratarse segun el nombre del elemento.
        switch (this.currentElement) {
            case UNDEFINED:
                switch (localName) {
                    case NODE_NAME_SIGNBATCH:
                        this.parent = UNDEFINED;
                        break;
                    case NODE_NAME_SINGLESIGN:
                        this.parent = ELEMENT_SIGNBATCH;
                        if (this.currentSignSaver == null || !this.currentSignSaver.isInitialized()) {
                            throw new SAXException("La clase de guardado no se ha inicializado para la firma " + this.currentSign.getId()); //$NON-NLS-1$
                        }
                        this.batchConfig.addSingleSign(this.currentSign);
                        this.currentSign = null;
                        this.currentSignSaver = null;
                        break;
                    case NODE_NAME_SIGNSAVER:
                        this.parent = ELEMENT_SINGLESIGN;
                        break;
                    default:
                        break;
                }
                break;
            case ELEMENT_DATASOURCE:
                this.currentSign.setDataSource(new String(this.acumulateContent.toCharArray()).trim());
                break;
            case ELEMENT_FORMAT:
                this.currentSign.setFormat(SignFormat.getFormat(new String(this.acumulateContent.toCharArray()).trim()));
                break;
            case ELEMENT_SUBOPERATION:
                this.currentSign.setSubOperation(SingleSignConstants.SignSubOperation.getSubOperation(new String(this.acumulateContent.toCharArray()).trim()));
                break;
            case ELEMENT_EXTRAPARAMS:
                Properties extraParams = new Properties();
                if (this.acumulateContent.size() > 0) {
                    try {
                        extraParams = AOUtil.base642Properties(new String(this.acumulateContent.toCharArray()).trim());
                    } catch (final IOException e) {
                        throw new SAXException("ExtraParams mal codificados en la firma " + this.currentSign.getId(), e); //$NON-NLS-1$
                    }
                }
                this.currentSign.setExtraParams(extraParams);
                break;
            case ELEMENT_CLASS:
                try {
                    this.currentSignSaver = (SignSaver) Class.forName(new String(this.acumulateContent.toCharArray()).trim()).newInstance();
                } catch (final Exception e) {
                    throw new SAXException("No se pudo cargar la clase de guardado para la firma " + this.currentSign.getId(), e); //$NON-NLS-1$
                }
                this.currentSign.setSignSaver(this.currentSignSaver);
                break;
            case ELEMENT_CONFIG:
                if (this.currentSignSaver == null) {
                    throw new SAXException("No se definio la clase de guardado para la firma " + this.currentSign.getId()); //$NON-NLS-1$
                }
                Properties config = new Properties();
                if (this.acumulateContent.size() > 0) {
                    try {
                        config = AOUtil.base642Properties(new String(this.acumulateContent.toCharArray()).trim());
                    } catch (final IOException e) {
                        throw new SAXException("La configuracion de la clase de guardado esta mal codificada en la firma " + this.currentSign.getId(), e); //$NON-NLS-1$
                    }
                }
                this.currentSignSaver.init(config);
                break;
            default:
                break;
        }
        this.currentElement = UNDEFINED;
        this.acumulateContent.reset();
    }

    public SignBatchConfig getBatchConfig() {
        return this.batchConfig;
    }
}