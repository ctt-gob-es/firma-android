package es.gob.afirma.android;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;

import es.gob.afirma.android.gui.AppConfig;

/**
 * Clase para la detecci&oacute;n de NFC.
 * Created by carlos on 08/07/2016.
 */
public class NfcHelper {

    /**
     * Comprueba si el dispositivo cuenta con NFC.
     * @param context Contexto de la aplicaci&oacute;n.
     * @return {@code true} si el dispositivo tiene NFC, {@code false} en caso contrario.
     */
    public static boolean isNfcServiceAvailable(final Context context) {
        final NfcManager manager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        return manager != null && manager.getDefaultAdapter() != null;
    }

    /**
     * Comprueba si el dispositivo tiene habilitado NFC.
     * @param context Contexto de la aplicaci&oacute;n.
     * @return {@code true} si el dispositivo tiene habilitado NFC, {@code false} en caso contrario.
     */
    static boolean isNfcServiceEnabled(final Context context) {
        final NfcManager manager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        if (manager == null) {
            return false;
        }

        final NfcAdapter adapter = manager.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    /**
     * Indica si esta habilitado el uso de NFC para DNIe 3.0.
     * @param context Contexto sobre el que se ejecuta la operaci&oacute;n.
     * @return {@code true} si el usuario habilit&oacute; el uso de DNIe 3.0 a trav&eacute;s de NFC.
     */
    public static boolean isNfcPreferredConnection(Context context) {
        return AppConfig.isUseNfcConnection(context);
    }

    /** Configura si debe intentarse utilizar el DNIe 3.0 mediante NFC.
     * @param preferred Indica si se debe usar NFC o no. */
    public static void configureNfcAsPreferredConnection(boolean preferred) {
        AppConfig.setUseNfcConnection(preferred);
    }
}
