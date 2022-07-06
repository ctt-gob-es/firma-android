package es.gob.afirma.android.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import es.gob.afirma.BuildConfig;
import es.gob.afirma.android.CheckConnectionsHelper;
import es.gob.afirma.android.Logger;
import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.misc.http.HttpError;
import es.gob.afirma.core.misc.http.UrlHttpManager;
import es.gob.afirma.core.misc.http.UrlHttpMethod;

/**
 * Gestor para las conexiones remotas de la aplicaci&oacute;n.
 */
public class AndroidHttpManager implements UrlHttpManager {

    /** Se&ntilde;ala si la aplicaci&oacute;n se encuentra en modo depuracion y deber&iacute;a
     * permitirse la conexi&oacute;n con dominios no seguros.
     */
    private static final boolean DEBUG = BuildConfig.DEBUG;

    /** Tiempo de espera por defecto para descartar una conexi&oacute;n HTTP. */
    private static final int DEFAULT_TIMEOUT = -1;

    private static final HostnameVerifier DEFAULT_HOSTNAME_VERIFIER = HttpsURLConnection.getDefaultHostnameVerifier();
    private static final SSLSocketFactory DEFAULT_SSL_SOCKET_FACTORY = HttpsURLConnection.getDefaultSSLSocketFactory();

    /** Contexto SSL. */
    private static final String SSL_CONTEXT = "SSL";//$NON-NLS-1$

    /** Esquema HTTPS. */
    private static final String HTTPS = "https"; //$NON-NLS-1$

    private static final TrustManager[] DUMMY_TRUST_MANAGER = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkClientTrusted(final X509Certificate[] certs, final String authType) { /* No hacemos nada */ }
                @Override
                public void checkServerTrusted(final X509Certificate[] certs, final String authType) {  /* No hacemos nada */  }

            }
    };

    @Override
    public byte[] readUrl(String url, UrlHttpMethod method) throws IOException {
        return readUrl(url, DEFAULT_TIMEOUT, null, null, method);
    }

    @Override
    public byte[] readUrl(String url, int timeout, String contentType, String accept, UrlHttpMethod method) throws IOException {
        final Properties headers = new Properties();
        if (contentType != null) {
            headers.setProperty("Content-Type", contentType); //$NON-NLS-1$
        }
        if (accept != null) {
            headers.setProperty("Accept", accept); //$NON-NLS-1$
        }
        return readUrl(url, timeout, method, headers);
    }

    @Override
    public byte[] readUrl(String url, int timeout, UrlHttpMethod method, Properties requestProperties) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("La URL a leer no puede ser nula"); //$NON-NLS-1$
        }

        String urlParameters = null;
        String request = null;
        if (UrlHttpMethod.POST.equals(method)) {
            final StringTokenizer st = new StringTokenizer(url, "?"); //$NON-NLS-1$
            request = st.nextToken();
            urlParameters = st.nextToken();
        }

        final URL uri = new URL(request != null ? request : url);

        boolean debugMode = DEBUG && uri.getProtocol().equals(HTTPS);

        final boolean validateSSLConnections = CheckConnectionsHelper.isValidateSSLConnections(null);
        final boolean isSecureDomain = checkIsSecureDomain(uri);

        if (debugMode || (!validateSSLConnections || isSecureDomain)) {
            disableSslChecks();
        }

        final HttpURLConnection conn = (HttpURLConnection) uri.openConnection(Proxy.NO_PROXY);

        conn.setRequestMethod(method.toString());

        if (requestProperties != null) {
            for (final String key : requestProperties.keySet().toArray(new String[0])) {
                conn.addRequestProperty(key, requestProperties.getProperty(key)); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        if (timeout != DEFAULT_TIMEOUT) {
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
        }

        if (urlParameters != null) {
            conn.setDoOutput(true);
            final OutputStreamWriter writer = new OutputStreamWriter(
                    conn.getOutputStream()
            );
            writer.write(urlParameters);
            writer.flush();
            writer.close();
        }

        conn.connect();
        final int resCode = conn.getResponseCode();
        final String statusCode = Integer.toString(resCode);
        if (statusCode.startsWith("4") || statusCode.startsWith("5")) { //$NON-NLS-1$ //$NON-NLS-2$
            if (debugMode) {
                enableSslChecks();
            }
            throw new HttpError(resCode, conn.getResponseMessage(), url);
        }

        final InputStream is = conn.getInputStream();
        final byte[] data = readDataFromInputStream(is);
        is.close();

        if (debugMode) {
            enableSslChecks();
        }

        return data;
    }

    /** Habilita las comprobaciones de certificados en conexiones SSL dej&aacute;ndolas con su
     * comportamiento por defecto. */
    private static void enableSslChecks() {
        HttpsURLConnection.setDefaultSSLSocketFactory(DEFAULT_SSL_SOCKET_FACTORY);
        HttpsURLConnection.setDefaultHostnameVerifier(DEFAULT_HOSTNAME_VERIFIER);
    }

    /** Deshabilita las comprobaciones de certificados en conexiones SSL, acept&aacute;dose entonces
     * cualquier certificado. */
    private static void disableSslChecks()  {
        SSLContext sc;
        try {
            sc = SSLContext.getInstance(SSL_CONTEXT);
            sc.init(null, DUMMY_TRUST_MANAGER, new java.security.SecureRandom());
        }
        catch(final Exception e) {
            Logger.w("es.gob.afirma", //$NON-NLS-1$
                    "No se ha podido ajustar la confianza SSL, es posible que no se pueda completar la conexion: " + e //$NON-NLS-1$
            );
            return;
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier(
				new HostnameVerifier() {
					@Override
					public boolean verify(final String hostname, final SSLSession session) {
						return true;
					}
				}
		);
    }

    /** Lee el contenido de un InputStream, abortando la operaci&oacute;n si en alg&uacute;n momento se
     * detiene la tarea.
     * @param is Flujo de datos de entrada.
     * @return Datos le&iacute;dos del flujo.
     * @throws IOException Cuando ocurre un error durante la lectura.
     * @throws AOCancelledOperationException Cuando se cancela la tarea. */
    private byte[] readDataFromInputStream(final InputStream is) throws IOException {

        int n;
        final byte[] buffer = new byte[1024];
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((n = is.read(buffer)) > 0) {
            baos.write(buffer, 0, n);
        }

        return baos.toByteArray();
    }

    /**
     * Comprueba si el dominio de una URL se considera seguro con respecto a una lista de dominios
     * seguros establecidos a traves de la propiedad determinado por la constante JAVA_PARAM_SECURE_DOMAINS_LIST.
     * @param url URL que se desea comprobar.
     * @return {@code true} si el dominio de la URL es seguro, {@code false} en caso contrario.
     */
    private static boolean checkIsSecureDomain(final URL url) {

        final String secureDomainsList = CheckConnectionsHelper.getTrustedDomains(null);
        if (secureDomainsList != null && !secureDomainsList.isEmpty()) {
            final String urlHost = url.getHost().toLowerCase(Locale.ROOT);
            final String [] secureDomainsArray = secureDomainsList.split("\n"); //$NON-NLS-1$
            for (final String secureDomain : secureDomainsArray) {
                // Caso 1 - Dominios con * al principio y al final. Ej: *.redsara.*
                final String replSecureDomain = secureDomain.trim().toLowerCase(Locale.ROOT)
                        .replace("*","");  //$NON-NLS-1$//$NON-NLS-2$
                if (secureDomain.startsWith("*") && secureDomain.endsWith("*")) { //$NON-NLS-1$ //$NON-NLS-2$
                    if (urlHost.contains(replSecureDomain)) {
                        return true;
                    }
                }
                // Caso 2 - Dominios con * solo al principio
                else if (secureDomain.startsWith("*")) {  //$NON-NLS-1$
                    if (urlHost.endsWith(replSecureDomain)) {
                        return true;
                    }
                }
                // Caso 3 - Dominios con * solo al final
                else if (secureDomain.endsWith("*")) { //$NON-NLS-1$
                    if (urlHost.startsWith(replSecureDomain)) {
                        return true;
                    }
                }
                // Caso 4 - Dominios sin *
                else if (urlHost.equals(replSecureDomain)) {
                    return true;
                }
            }
        }
        return false;
    }
}
