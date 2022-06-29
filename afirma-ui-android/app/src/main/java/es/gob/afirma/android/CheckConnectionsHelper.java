package es.gob.afirma.android;

import android.content.Context;

import es.gob.afirma.android.gui.AppConfig;

/**
 * Clase para la validacion de conexiones SSL y configuracion de lista de dominios seguros
 * Created by jose on 17/06/2022.
 */
public class CheckConnectionsHelper {

    /**
     * Indica si esta habilitada la validaci&oacute;n o no de conexiones SSL.
     * @param context Contexto padre.
     * @return {@code true} si esta habilitada la validaci&oacute;n de conexiones SSL.
     */
    public static boolean isValidateSSLConnections(Context context) {
        return AppConfig.isValidateSSLConnections(context);
    }

    /**
     * Configura si se debe validar o no las conexiones SSL.
     * @param validateSSLConnections Indica si se debe validar o no las conexiones SSL.
     */
    public static void configureValidateSSLConnections(boolean validateSSLConnections) {
        AppConfig.setValidateSSLConnections(validateSSLConnections);
    }

    /**
     * Devuelve los dominios de confianza configurados.
     * @param context Contexto padre.
     * @return Devuelve una cadena con los dominios configurados, separados por un salto de l&iacute;nea.
     */
    public static String getTrustedDomains(Context context) {
        return AppConfig.getTrustedDomains(context);
    }

    /**
     * Establece los dominios de confianza pasados por par&aacute;metro.
     * @param trustedDomains Dominios a configurar.
     */
    public static void setTrustedDomains(String trustedDomains) {
        AppConfig.setTrustedDomains(trustedDomains);
    }
}
