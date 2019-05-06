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

    /**
     * Recupera una preferencia de la aplicaci&oacute;n.
     * @param context Contexto sobre el que se ejecuta la operaci&oacute;n.
     * @param key Clave de identificacion de la preferencia.
     * @param defaultValue Valor a devolver por defecto en caso de no estar establecida.
     * @return Valor de la preferencia o el valor por defecto si no estaba establecida.
     */
    private static boolean getPreference(Context context, final String key, final boolean defaultValue) {
        init(context);
        return sharedPref.getBoolean(key, defaultValue);
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
}
