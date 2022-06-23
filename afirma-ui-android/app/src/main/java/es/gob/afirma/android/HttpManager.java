package es.gob.afirma.android;

import java.io.IOException;

import es.gob.afirma.core.misc.http.UrlHttpManager;
import es.gob.afirma.core.misc.http.UrlHttpManagerFactory;
import es.gob.afirma.core.misc.http.UrlHttpManagerImpl;
import es.gob.afirma.core.misc.http.UrlHttpMethod;

public class HttpManager {
    private final UrlHttpManager urlManager;

    /**
     * Construye el objeto para el acceso a los recursos y servicios.
     */
    public HttpManager() {
        this.urlManager = UrlHttpManagerFactory.getInstalledManager();
    }

    /**
     * Accede a un recurso o servicio remoto.
     * @param url URL del recurso/servicio.
     * @param method M&eacute;todo HTTP de acceso.
     * @return Contenido del recurso o resultado del servicio.
     * @throws IOException Cuando ocurre un error durante la recuperaci&oacute;n del resultado.
     */
    public byte[] readUrl(final String url, final UrlHttpMethod method) throws IOException {
        return this.urlManager.readUrl(url, method);
    }

    /**
     * Accede a un recurso o servicio remoto.
     * @param url URL del recurso/servicio.
     * @param method M&eacute;todo HTTP de acceso.
     * @return Contenido del recurso o resultado del servicio.
     * @throws IOException Cuando ocurre un error durante la recuperaci&oacute;n del resultado.
     */
    public byte[] readUrl(final StringBuilder url, final UrlHttpMethod method) throws IOException {
        return this.urlManager.readUrl(url.toString(), method);
    }

    /**
     * Configura si deben respetarse o no las comprobaciones de seguirdad en las conexiones HTTPS.
     * @param secure {@code true} si deben respetarse las comprobaciones, {@code false} en caso
     * contrario.
     */
    public static void setSecureConnections(final boolean secure) {
        System.setProperty(
                UrlHttpManagerImpl.JAVA_PARAM_DISABLE_SSL_CHECKS,
                Boolean.toString(!secure));
    }
}
