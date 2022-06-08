package es.gob.afirma.android.batch.client;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

import es.gob.afirma.android.Logger;
import es.gob.afirma.android.batch.TriphaseDataParser;
import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.Base64;
import es.gob.afirma.core.misc.http.HttpError;
import es.gob.afirma.core.misc.http.UrlHttpManagerFactory;
import es.gob.afirma.core.misc.http.UrlHttpMethod;
import es.gob.afirma.core.signers.AOPkcs1Signer;
import es.gob.afirma.core.signers.TriphaseData;
import es.gob.afirma.core.signers.TriphaseDataSigner;

public class BatchSigner {

    private static final String BATCH_JSON_PARAM = "json"; //$NON-NLS-1$
    private static final String BATCH_CRT_PARAM = "certs"; //$NON-NLS-1$
    private static final String BATCH_TRI_PARAM = "tridata"; //$NON-NLS-1$

    private static final String EQU = "="; //$NON-NLS-1$
    private static final String AMP = "&"; //$NON-NLS-1$

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /*
     * Procesa un lote de firmas.
     * Los lotes deben proporcionase definidos en un fichero JSON con un determinado esquema.
     * Puede ver dicho esquema y un ejemplo de petici&oacute;n
     * <a href="doc-files/batch-scheme.html">aqu&iacute;</a>.
     * @param batchB64 JSON de definici&oacute;n del lote de firmas.
     * @param batchPresignerUrl URL del servicio remoto de preproceso de lotes de firma.
     * @param batchPostSignerUrl URL del servicio remoto de postproceso de lotes de firma.
     * @param certificates Cadena de certificados del firmante.
     * @param pk Clave privada para realizar las firmas cliente.
     * @return Cadena JSON con el resultado de la firma del lote. La estructura presentar&aacute;
     * la estructura indicada <a href="doc-files/resultlog-scheme.html">aqu&iacute;</a>.
     * @throws IOException Si hay problemas de red o en el tratamiento de datos.
     * @throws CertificateEncodingException Si los certificados proporcionados no son v&aacute;lidos.
     * @throws AOException Si hay errores en las firmas cliente.
     * */
    public static String signJSON(final String batchB64,
                                  final String batchPresignerUrl,
                                  final String batchPostSignerUrl,
                                  final Certificate[] certificates,
                                  final PrivateKey pk) throws CertificateEncodingException,
            IOException,
            AOException, JSONException {
        if (batchB64 == null || batchB64.isEmpty()) {
            throw new IllegalArgumentException("El lote de firma no puede ser nulo ni vacio"); //$NON-NLS-1$
        }
        if (batchPresignerUrl == null || batchPresignerUrl.isEmpty()) {
            throw new IllegalArgumentException(
                    "La URL de preproceso de lotes no puede se nula ni vacia" //$NON-NLS-1$
            );
        }
        if (batchPostSignerUrl == null || batchPostSignerUrl.isEmpty()) {
            throw new IllegalArgumentException(
                    "La URL de postproceso de lotes no puede ser nula ni vacia" //$NON-NLS-1$
            );
        }
        if (certificates == null || certificates.length < 1) {
            throw new IllegalArgumentException(
                    "La cadena de certificados del firmante no puede ser nula ni vacia" //$NON-NLS-1$
            );
        }

        final String batchUrlSafe = batchB64.replace("+", "-").replace("/", "_");  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
        byte[] ret;

        try {
            ret = UrlHttpManagerFactory.getInstalledManager().readUrl(
                    batchPresignerUrl + "?" + //$NON-NLS-1$
                            BATCH_JSON_PARAM + EQU + batchUrlSafe + AMP +
                            BATCH_CRT_PARAM + EQU + getCertChainAsBase64(certificates),
                    UrlHttpMethod.POST
            );
        } catch (final HttpError e) {
            Logger.e("El servicio de firma devolvio un  error durante la prefirma", e.getResponseDescription()); //$NON-NLS-1$
            throw e;
        }

        final TriphaseData td1 = TriphaseDataParser.parseFromJSON(ret);

        // El cliente hace los PKCS#1 generando TD2, que envia de nuevo al servidor
        final TriphaseData td2 = TriphaseDataSigner.doSign(
                new AOPkcs1Signer(),
                getAlgorithmForJSON(batchB64),
                pk,
                certificates,
                td1,
                null // Sin ExtraParams para el PKCS#1 en lotes
        );

        // Llamamos al servidor de nuevo para el postproceso
        try {
            ret = UrlHttpManagerFactory.getInstalledManager().readUrl(
                    batchPostSignerUrl + "?" + //$NON-NLS-1$
                            BATCH_JSON_PARAM + EQU + batchUrlSafe + AMP +
                            BATCH_CRT_PARAM + EQU + getCertChainAsBase64(certificates) + AMP +
                            BATCH_TRI_PARAM + EQU +
                            Base64.encode(TriphaseDataParser.triphaseDataToJsonString(td2).getBytes(DEFAULT_CHARSET), true),
                    UrlHttpMethod.POST
            );
        } catch (final HttpError e) {
            Logger.e("El servicio de firma devolvio un  error durante la postfirma", e.getResponseDescription()); //$NON-NLS-1$
            throw e;
        }

        return new String(ret, DEFAULT_CHARSET);
    }

    private static String getCertChainAsBase64(final Certificate[] certChain) throws CertificateEncodingException {
        final StringBuilder sb = new StringBuilder();
        for (final Certificate cert : certChain) {
            sb.append(Base64.encode(cert.getEncoded(), true));
            sb.append(";"); //$NON-NLS-1$
        }
        final String ret = sb.toString();

        // Quitamos el ";" final
        return ret.substring(0, ret.length()-1);
    }

    /**
     * Obtiene el algoritmo para la petici&oacute;n de formato JSON
     * @param batch datos de la petici&oacute;n
     * @return algoritmo a usar
     * @throws IOException error en caso de que no se lea correctamente la petici&oacute;n
     */
    private static String getAlgorithmForJSON(final String batch) throws IOException, JSONException {

        JSONObject jsonObject;
        final String convertedJson = new String(Base64.decode(batch), DEFAULT_CHARSET);
        try {
            jsonObject = new JSONObject(convertedJson);
        }catch (final JSONException jsonEx){
            Logger.e("Error al parsear JSON", jsonEx.toString()); //$NON-NLS-1$
            throw new JSONException(
                    "El JSON de definicion de lote de firmas no esta formado correctamente" //$NON-NLS-1$
            );
        }

        if (jsonObject.has("algorithm")){ //$NON-NLS-1$
            return jsonObject.getString("algorithm"); //$NON-NLS-1$
        }

        throw new IllegalArgumentException(
                "El nodo 'signbatch' debe contener al manos el atributo de algoritmo" //$NON-NLS-1$
        );
    }
}
