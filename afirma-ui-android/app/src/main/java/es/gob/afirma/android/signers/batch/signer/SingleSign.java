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
 * Modificado por: Eduardo García <eduardo.l.g.g@gmail.com>
 * Fecha modificación: 10 sep 2020
 */
package es.gob.afirma.android.signers.batch.signer;

import android.provider.ContactsContract;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import es.gob.afirma.android.signers.constants.SignFormat;
import es.gob.afirma.android.signers.batch.SingleSignConstants;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.android.signers.batch.signer.SingleSign.ProcessResult.Result;
import es.gob.afirma.android.signers.batch.SingleSignConstants.SignSubOperation;

/** Firma electr&oacute;nica &uacute;nica dentro de un lote.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class SingleSign {

    private static final String PROP_ID = "SignatureId"; //$NON-NLS-1$

    private static final String XML_ATTRIBUTE_ID = "Id"; //$NON-NLS-1$

    private static final String XML_ELEMENT_DATASOURCE = "datasource"; //$NON-NLS-1$
    private static final String XML_ELEMENT_FORMAT = "format"; //$NON-NLS-1$
    private static final String XML_ELEMENT_SUBOPERATION = "suboperation"; //$NON-NLS-1$
    private static final String XML_ELEMENT_SIGNSAVER = "signsaver"; //$NON-NLS-1$
    private static final String XML_ELEMENT_SIGNSAVER_CLASSNAME = "class"; //$NON-NLS-1$
    private static final String XML_ELEMENT_SIGNSAVER_CONFIG = "config"; //$NON-NLS-1$
    private static final String XML_ELEMENT_EXTRAPARAMS = "extraparams"; //$NON-NLS-1$

    private static final String HTTP_SCHEME = "http://"; //$NON-NLS-1$
    private static final String HTTPS_SCHEME = "https://"; //$NON-NLS-1$
    private static final String FTP_SCHEME = "ftp://"; //$NON-NLS-1$

    private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

    private Properties extraParams;

    private static String allowedSources = "base64;http://*;https://*;ftp://*";
    public enum DatasourceTypes {
        BASE64, HTTP, HTTPS, FTP
    };
    public DatasourceTypes datasourceType;
    private String dataSource;

    private SignFormat format;

    private final String id;

    private SingleSignConstants.SignSubOperation subOperation;

    private SignSaver signSaver;

    private ProcessResult processResult = new ProcessResult(Result.NOT_STARTED, null);

    /** Crea una definici&oacute;n de tarea de firma electr&oacute;nica &uacute;nica.
     * @param id Identificador de la firma. */
    SingleSign(final String id) {
        this.id =  id;
        this.extraParams = new Properties();
        // El identificador de la firma debe transmitirse al firmador trifasico a traves
        // de los extraParams para que este lo utilice y asi podamos luego asociar la
        // firma con los datos a los que corresponden
        this.extraParams.put(PROP_ID, getId());
    }

    /** Crea una definici&oacute;n de tarea de firma electr&oacute;nica &uacute;nica.
     * @param id Identificador de la firma.
     * @param dataSrc Datos a firmar.
     * @param fmt Formato de firma.
     * @param subOp Tipo de firma a realizar.
     * @param xParams Opciones adicionales de la firma.
     * @param ss Objeto para guardar la firma una vez completada. */
    public SingleSign(final String id,
                      final String dataSrc,
                      final SignFormat fmt,
                      final SignSubOperation subOp,
                      final Properties xParams,
                      final SignSaver ss) {

        if (dataSrc == null) {
            throw new IllegalArgumentException(
                    "El origen de los datos a firmar no puede ser nulo" //$NON-NLS-1$
            );
        }

        if (fmt == null) {
            throw new IllegalArgumentException(
                    "El formato de firma no puede ser nulo" //$NON-NLS-1$
            );
        }

        if (ss == null) {
            throw new IllegalArgumentException(
                    "El objeto de guardado de firma no puede ser nulo" //$NON-NLS-1$
            );
        }

        this.dataSource = dataSrc;
        this.format = fmt;

        this.id = id != null ? id : UUID.randomUUID().toString();

        // El identificador de la firma debe transmitirse al firmador trifasico a traves
        // de los extraParams para que este lo utilice y asi podamos luego asociar la
        // firma con los datos a los que corresponden
        this.extraParams = xParams != null ? xParams : new Properties();
        this.extraParams.put(PROP_ID, getId());

        this.subOperation = subOp;
        this.signSaver = ss;
    }

    void save(final byte[] dataToSave) throws IOException {
        this.signSaver.saveSign(this, dataToSave);
    }

    public String getDataSource() {
        return dataSource;
    }

    /**
     * Recupera los par&aacute;metros de configuraci&oacute;n del formato de firma.
     * @return Configuraci&oacute;n del formato de firma.
     */
    public Properties getExtraParams() {
        return this.extraParams;
    }

    /**
     * Recupera el formato de firma.
     * @return Formato de firma.
     */
    public SignFormat getSignFormat() {
        return this.format;
    }

    public String getRetrieveServerUrl() {
        return this.signSaver.getConfig().getProperty("URL");
    }

    public String getSignId() {
        String signId = this.signSaver.getConfig().getProperty("fileId");
        return signId != null && !signId.isEmpty() ? signId : getId();
    }

    public SignSubOperation getSubOperation() {
        return this.subOperation;
    }

    void setExtraParams(final Properties extraParams) {
        // El identificador de la firma debe transmitirse al firmador trifasico a traves
        // de los extraParams para que este lo utilice y asi podamos luego asociar la
        // firma con los datos a los que corresponden
        this.extraParams = extraParams != null ? extraParams : new Properties();
        this.extraParams.put(PROP_ID, getId());
    }

    void setDataSource(final String dataSource) {
        this.dataSource = dataSource;
    }

    void setFormat(final SignFormat format) {
        this.format = format;
    }

    void setSubOperation(final SignSubOperation subOperation) {
        this.subOperation = subOperation;
    }

    void setSignSaver(final SignSaver signSaver) {
        this.signSaver = signSaver;
    }

    public void checkDatasource() {
        if (dataSource == null) {
            throw new IllegalArgumentException(
                    "el origen de los datos no puede ser nulo" //$non-nls-1$
            );
        }
        for (final String allowed : SingleSign.allowedSources.split(";")) {
            if ("base64".equals(allowed) && Base64.isBase64(dataSource)) { //$non-nls-1$
                datasourceType = DatasourceTypes.BASE64;
                return;
            }
            if (allowed.endsWith("*")) { //$non-nls-1$
                if (dataSource.startsWith(allowed.replace("*", ""))) { //$non-nls-1$ //$non-nls-2$
                    for (DatasourceTypes t : DatasourceTypes.values())
                        if (dataSource.startsWith(t.name().toLowerCase())) {
                            datasourceType = t;
                            return;
                        }
                }
            } else {
                if (dataSource.equals(allowed)) {
                    return;
                }
            }
        }
        throw new SecurityException("origen de datos no valido"); //$non-nls-1$
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(" <singlesign "); //$NON-NLS-1$
        sb.append(XML_ATTRIBUTE_ID);
        sb.append("=\""); //$NON-NLS-1$
        sb.append(getId());
        sb.append("\">\n  <"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_DATASOURCE);
        sb.append(">"); //$NON-NLS-1$
        sb.append(this.dataSource);
        sb.append("</"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_DATASOURCE);
        sb.append(">\n  <"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_FORMAT);
        sb.append(">"); //$NON-NLS-1$
        sb.append(getSignFormat().toString());
        sb.append("</"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_FORMAT);
        sb.append(">\n  <"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_SUBOPERATION);
        sb.append(">"); //$NON-NLS-1$
        sb.append(getSubOperation().toString());
        sb.append("</"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_SUBOPERATION);
        sb.append(">\n  <"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_EXTRAPARAMS);
        sb.append(">"); //$NON-NLS-1$
        try {
            sb.append(AOUtil.properties2Base64(getExtraParams()));
        }
        catch (final IOException e) {
            LOGGER.severe(
                    "Error convirtiendo los parametros adicionales de la firma '" + getId() + "' a Base64: " + e //$NON-NLS-1$ //$NON-NLS-2$
            );
        }
        sb.append("</"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_EXTRAPARAMS);
        sb.append(">\n  <"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_SIGNSAVER);
        sb.append(">\n   <"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_SIGNSAVER_CLASSNAME);
        sb.append(">"); //$NON-NLS-1$
        sb.append(this.signSaver.getClass().getName());
        sb.append("</"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_SIGNSAVER_CLASSNAME);
        sb.append(">\n   <"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_SIGNSAVER_CONFIG);
        sb.append(">"); //$NON-NLS-1$
        try {
            sb.append(AOUtil.properties2Base64(this.signSaver.getConfig()));
        }
        catch (final IOException e) {
            LOGGER.severe(
                    "Error convirtiendo la configuracion del objeto de guardado de la firma '" + getId() + "' a Base64: " + e //$NON-NLS-1$ //$NON-NLS-2$
            );
        }
        sb.append("</"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_SIGNSAVER_CONFIG);
        sb.append(">\n  </"); //$NON-NLS-1$
        sb.append(XML_ELEMENT_SIGNSAVER);
        sb.append(">\n </singlesign>"); //$NON-NLS-1$

        return sb.toString();
    }

    /** Realiza el proceso de prefirma, incluyendo la descarga u obtenci&oacute;n de datos.
     * @param certChain Cadena de certificados del firmante.
     * @param algorithm Algoritmo de firma.
     * @return Nodo <code>firma</code> del XML de datos trif&aacute;sicos (sin ninguna etiqueta
     *         antes ni despu&eacute;s).
     * @throws AOException Si hay problemas en la propia firma electr&oacute;nica.
     * @throws IOException Si hay problemas en la obtenci&oacute;n, tratamiento o gradado de datos. */
//    String doPreProcess(final X509Certificate[] certChain,
//                        final SignAlgorithm algorithm) throws IOException,
//            AOException {
//        return SingleSignPreProcessor.doPreProcess(this, certChain, algorithm);
//    }

    /** Obtiene la tarea de preproceso de firma para ser ejecutada en paralelo.
     * @param certChain Cadena de certificados del firmante.
     * @param algorithm Algoritmo de firma.
     * @return Tarea de preproceso de firma para ser ejecutada en paralelo. */
//    Callable<String> getPreProcessCallable(final X509Certificate[] certChain,
//                                           final SingleSignConstants.SignAlgorithm algorithm) {
//        return new Callable<String>() {
//            @Override
//            public String call() throws IOException, AOException {
//                return doPreProcess(certChain, algorithm);
//            }
//        };
//    }

    /** Realiza el proceso de postfirma, incluyendo la subida o guardado de datos.
     * @param certChain Cadena de certificados del firmante.
     * @param td Datos trif&aacute;sicos relativos <b>&uacute;nicamente</b> a esta firma.
     *           Debe serializarse como un XML con esta forma (ejemplo):
     *           <pre>
     *            &lt;xml&gt;
     *             &lt;firmas&gt;
     *              &lt;firma Id="53820fb4-336a-47ee-b7ba-f32f58e5cfd6"&gt;
     *               &lt;param n="PRE"&gt;MYICXDAYBgk[...]GvykA=&lt;/param&gt;
     *               &lt;param n="PK1"&gt;dC2dIILB9HV[...]xT1bY=&lt;/param&gt;
     *               &lt;param n="NEED_PRE"&gt;true&lt;/param&gt;
     *              &lt;/firma&gt;
     *             &lt;/firmas&gt;
     *            &lt;/xml&gt;
     *           </pre>
     * @param algorithm Algoritmo de firma.
     * @param batchId Identificador del lote de firma.
     * @throws AOException Si hay problemas en la propia firma electr&oacute;nica.
     * @throws IOException Si hay problemas en la obtenci&oacute;n, tratamiento o gradado de datos.
     * @throws NoSuchAlgorithmException Si no se soporta alg&uacute;n algoritmo necesario. */
//    void doPostProcess(final X509Certificate[] certChain,
//                       final TriphaseData td,
//                       final SingleSignConstants.SignAlgorithm algorithm,
//                       final String batchId) throws IOException,
//            AOException,
//            NoSuchAlgorithmException {
//        SingleSignPostProcessor.doPostProcess(
//                this, certChain, td, algorithm, batchId
//        );
//    }

    /** Obtiene la tarea de postproceso de firma para ser ejecutada en paralelo.
     * @param certChain Cadena de certificados del firmante.
     * @param td Datos trif&aacute;sicos relativos <b>&uacute;nicamente</b> a esta firma.
     *           Debe serializarse como un XML con esta forma (ejemplo):
     *           <pre>
     *            &lt;xml&gt;
     *             &lt;firmas&gt;
     *              &lt;firma Id="53820fb4-336a-47ee-b7ba-f32f58e5cfd6"&gt;
     *               &lt;param n="PRE"&gt;MYICXDAYBgk[...]GvykA=&lt;/param&gt;
     *               &lt;param n="PK1"&gt;dC2dIILB9HV[...]xT1bY=&lt;/param&gt;
     *               &lt;param n="NEED_PRE"&gt;true&lt;/param&gt;
     *              &lt;/firma&gt;
     *             &lt;/firmas&gt;
     *            &lt;/xml&gt;
     *           </pre>
     * @param algorithm Algoritmo de firma.
     * @param batchId Identificador del lote de firma.
     * @return Tarea de postproceso de firma para ser ejecutada en paralelo. */
//    Callable<CallableResult> getPostProcessCallable(final X509Certificate[] certChain,
//                                                    final TriphaseData td,
//                                                    final SingleSignConstants.SignAlgorithm algorithm,
//                                                    final String batchId) {
//        return new Callable<CallableResult>() {
//            @Override
//            public CallableResult call() {
//                try {
//                    doPostProcess(certChain, td, algorithm, batchId);
//                }
//                catch(final Exception e) {
//                    return new CallableResult(getId(), e);
//                }
//                return new CallableResult(getId());
//            }
//        };
//
//    }

//    Callable<CallableResult> getSaveCallable(final TempStore ts, final String batchId) {
//        return new Callable<CallableResult>() {
//            @Override
//            public CallableResult call() {
//                try {
//                    save(ts.retrieve(SingleSign.this, batchId));
//                }
//                catch(final Exception e) {
//                    return new CallableResult(getId(), e);
//                }
//                return new CallableResult(getId());
//            }
//        };
//    }

    /**
     * Recupera el identificador asignado en el lote a la firma.
     * @return Identificador.
     */
    public String getId() {
        return this.id;
    }

//    /**
//     * Recupera los datos que se deben procesar.
//     * @param stored {@code} true, indica que en caso de tratarse de datos remotos, estos ya
//     * estar&aacute;n cargados en un temporal y deben tomarse de este; {@code false} indica
//     * que se deber&aacute;n cargar los datos desde la fuente y, en caso de ser remotos, se
//     * crear&aacute; un temporal para ellos.
//     * @return Datos.
//     * @throws IOException Cuando no se pueden obtener los datos en caso de que estos sean remotos.
//     */
//    public byte[] getData(final boolean stored) throws IOException {
//        // Si se nos solicita un fichero remoto, calculamos cual seria el fichero
//        // temporal que le corresponderia
//        String tempResource = null;
//        if (this.dataSource.startsWith(HTTP_SCHEME) || this.dataSource.startsWith(HTTPS_SCHEME) || this.dataSource.startsWith(FTP_SCHEME)) {
//            try {
//                tempResource = getTempFileName(this.dataSource, this.id);
//            }
//            catch (final Exception e) {
//                LOGGER.warning("No se puede calcular el nombre de un temporal para un recurso remoto: " + e); //$NON-NLS-1$
//                tempResource = null;
//            }
//        }
//
//        // Si se indica que este fichero ya se almaceno
//        // y deberia haber un recurso local, lo cargamos
//        byte[] data = null;
//        if (stored && tempResource != null) {
//            try {
//                final TempStore tempStore = TempStoreFactory.getTempStore();
//                data = tempStore.retrieve(tempResource);
//                tempStore.delete(tempResource);
//            }
//            catch (final Exception e) {
//                LOGGER.warning(String.format("No se puede recuperar el recurso temporal %0s, se cargara de la fuente original: " + e, tempResource)); //$NON-NLS-1$
//            }
//        }
//
//        // Si no, lo descargamos de la fuente original
//        if (data == null) {
//            checkDataSource(this.dataSource);
//            data = DataDownloader.downloadData(this.dataSource);
//        }
//
//        // Finalmente, si se habia indicado que no habia recurso temporal
//        // pero deberia haberlo, lo creamos
//        if (!stored && tempResource != null) {
//            TempStoreFactory.getTempStore().store(data, tempResource);
//        }
//
//        return data;
//    }

    private static String getTempFileName(final String source, final String signId) throws NoSuchAlgorithmException {
        return Base64.encode(MessageDigest.getInstance("SHA-1").digest((source + signId).getBytes()), true); //$NON-NLS-1$
    }

    void setProcessResult(final ProcessResult pResult) {
        this.processResult = pResult;
    }

    ProcessResult getProcessResult() {
        this.processResult.setId(getId());
        return this.processResult;
    }

    void rollbackSave() {
        this.signSaver.rollback(this);
    }

    static class CallableResult {

        private final String signId;
        private final Exception exception;

        CallableResult(final String id) {
            this.signId = id;
            this.exception = null;
        }

        CallableResult(final String id, final Exception e) {
            this.signId = id;
            this.exception = e;
        }

        boolean isOk() {
            return this.exception == null;
        }

        Exception getError() {
            return this.exception;
        }

        String getSignatureId() {
            return this.signId;
        }
    }

    static final class ProcessResult {

        enum Result {
            NOT_STARTED,
            DONE_AND_SAVED,
            DONE_BUT_NOT_SAVED_YET,
            DONE_BUT_SAVED_SKIPPED,
            DONE_BUT_ERROR_SAVING,
            ERROR_PRE,
            ERROR_POST,
            SKIPPED,
            SAVE_ROLLBACKED;
        }

        private final Result result;
        private final String description;
        private String signId;

        boolean wasSaved() {
            return Result.DONE_AND_SAVED.equals(this.result);
        }

        static final ProcessResult PROCESS_RESULT_OK_UNSAVED = new ProcessResult(Result.DONE_BUT_NOT_SAVED_YET, null);
        static final ProcessResult PROCESS_RESULT_SKIPPED    = new ProcessResult(Result.SKIPPED,                null);
        static final ProcessResult PROCESS_RESULT_DONE_SAVED = new ProcessResult(Result.DONE_AND_SAVED,         null);
        static final ProcessResult PROCESS_RESULT_ROLLBACKED = new ProcessResult(Result.SAVE_ROLLBACKED,        null);

        ProcessResult(final Result r, final String d) {
            if (r == null) {
                throw new IllegalArgumentException(
                        "El resultado no puede ser nulo" //$NON-NLS-1$
                );
            }
            this.result = r;
            this.description = d != null ? d : ""; //$NON-NLS-1$
        }

        @Override
        public String toString() {
            return "<signresult id=\"" + this.signId + "\" result=\"" + this.result + "\" description=\"" + this.description + "\"/>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }

        void setId(final String id) {
            this.signId = id;
        }

        public Result getResult() {
            return this.result;
        }
    }

}
