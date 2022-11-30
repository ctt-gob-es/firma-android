package es.gob.afirma.android.gui;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Clase para la gesti&oacute;n de la configuraci&oacute;n de la aplicaci&oacute;n.
 */
public final class AppConfig {

    private static final String PREFERENCES_CONTEXT = "afirma.client"; //$NON-NLS-1$

    /** Clave de la preferencia que indicar que se trata de la primera ejecuci&oacute;n */
    private final static String PREFERENCE_KEY_FIRST_EXECUTION = "firstExecution";

    /** Clave de la preferencia que indica si debe usarse NFC para conectar con el DNIe 3.0 */
    private static final String PREFERENCE_KEY_USE_NFC = "useNfc";

    /** Nombre de la propiedad a establecer a <code>true</code> para habilitar las comprobaciones
     * de confianza SSL en las peticiones. Si se establece a <code>false</code> o no se establece,
     * no se realizar&aacute;n comprobaciones. */
    public static final String JAVA_PARAM_VALIDATE_SSL_CHECKS = "validateSslChecks"; //$NON-NLS-1$

    /** Lista de dominios seguros para conexiones HTTPS. */
    public static final String JAVA_PARAM_SECURE_DOMAINS_LIST = "secureDomainsList"; //$NON-NLS-1$

    private static SharedPreferences sharedPref;

    /** Inicializa el manejador de preferencias.
     * @param context Contexto padre. */
    private static void init(final Context context) {
        if (sharedPref == null) {
            sharedPref = context.getSharedPreferences(PREFERENCES_CONTEXT, Context.MODE_PRIVATE);
        }
    }

    public static boolean isUseNfcConnection(Context context) {
        return getPreference(context, PREFERENCE_KEY_USE_NFC, false);
    }

    public static void setUseNfcConnection(boolean useNfcConnection) {
        setPreference(PREFERENCE_KEY_USE_NFC, useNfcConnection);
    }

    public static boolean isFirstExecution(Context context) {
        return getPreference(context, PREFERENCE_KEY_FIRST_EXECUTION, true);
    }

    public static void setFirstExecution(boolean firstExecution) {
        setPreference(PREFERENCE_KEY_FIRST_EXECUTION, firstExecution);
    }

    public static boolean isValidateSSLConnections(Context context) {
        return getPreference(context, JAVA_PARAM_VALIDATE_SSL_CHECKS, true);
    }

    public static void setValidateSSLConnections(boolean validateSSL) {
        setPreference(JAVA_PARAM_VALIDATE_SSL_CHECKS, validateSSL);
    }

    public static String getTrustedDomains(Context context) {
        return getPreference(context, JAVA_PARAM_SECURE_DOMAINS_LIST, "");
    }

    public static void setTrustedDomains(String secureDomains) {
        if (secureDomains == null || secureDomains.isEmpty()) {
            removePreference(JAVA_PARAM_SECURE_DOMAINS_LIST);
        } else {
            setPreference(JAVA_PARAM_SECURE_DOMAINS_LIST, secureDomains);
        }

    }

    /**
     * Recupera una preferencia de la aplicaci&oacute;n.
     * @param context Contexto sobre el que se ejecuta la operaci&oacute;n.
     * @param key Clave de identificacion de la preferencia.
     * @param defaultValue Valor a devolver por defecto en caso de no estar establecida.
     * @return Valor de la preferencia o el valor por defecto si no estaba establecida.
     */
    private static boolean getPreference(Context context, final String key, final boolean defaultValue) {
        if (sharedPref == null && context == null) {
            return defaultValue;
        }
        init(context);
        return sharedPref.getBoolean(key, defaultValue);
    }

    /**
     * Recupera una preferencia de la aplicaci&oacute;n.
     * @param context Contexto sobre el que se ejecuta la operaci&oacute;n.
     * @param key Clave de identificacion de la preferencia.
     * @param defaultValue Valor a devolver por defecto en caso de no estar establecida.
     * @return Valor de la preferencia o el valor por defecto si no estaba establecida.
     */
    private static String getPreference(Context context, final String key, final String defaultValue) {
        if (sharedPref == null && context == null) {
            return defaultValue;
        }
        init(context);
        return sharedPref.getString(key, defaultValue);
    }

    /**
     * Establece una preferencia de la aplicacion.
     * @param key Clave de identificacion de la preferencia.
     * @param value Valor de la preferencia.
     */
    private static void setPreference(final String key, final boolean value) {
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Establece una preferencia de la aplicacion.
     * @param key Clave de identificacion de la preferencia.
     * @param value Valor de la preferencia.
     */
    private static void setPreference(final String key, final String value) {
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Elimina una preferencia de la aplicacion.
     * @param key Clave de identificacion de la preferencia.
     */
    private static void removePreference(final String key) {
        final SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(key);
        editor.apply();
    }
}
