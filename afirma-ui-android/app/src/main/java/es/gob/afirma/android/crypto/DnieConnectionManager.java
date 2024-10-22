package es.gob.afirma.android.crypto;

import android.nfc.Tag;

import es.gob.afirma.android.Logger;
import es.gob.jmulticard.connection.ApduConnection;

/**
 * Instancia &uacute;nica que almacenar&aacute; los distintos elementos para la conexi&oacute;n
 * con el DNIe (Tag de la tarjeta, CallbackHandler, conexion, referencia al almac&eacute;n...)
 */
public class DnieConnectionManager {

    private static final String ES_GOB_AFIRMA = "es.gob.afirma";

    private static DnieConnectionManager instance = null;

    public static DnieConnectionManager getInstance() {
        if (instance == null) {
            instance = new DnieConnectionManager();
        }
        return instance;
    }

    private CachePasswordCallback canPasswordCallback;
    private CachePasswordCallback pinPasswordCallback;
    private AndroidDnieNFCCallbackHandler callbackHandler;
    private ApduConnection nfcConnection;
    private Tag discoveredTag;

    private DnieConnectionManager() {
        this.canPasswordCallback = null;
        this.pinPasswordCallback = null;
        this.callbackHandler = null;
        this.nfcConnection = null;
        this.discoveredTag = null;
    }

    /**
     * Recupera el CallbackHandler la gesti&oacute;n de las solicitudes de CAN y PIN.
     * @return CallbackHandler configurado.
     */
    public AndroidDnieNFCCallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    /**
     * Establece el CallbackHandler para la gesti&oacute;n de las solicitudes de CAN y PIN.
     * @param  callbackHandler que se debe utilizar.
     */
    public void setCallbackHandler(AndroidDnieNFCCallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    /**
     * Establece la conexi&oacute;n NFC establecida con el DNIe.
     * @param nfcConnection Conexi&oacute;n con la tarjeta.
     */
    public void setNfcConnection(ApduConnection nfcConnection) {
        this.nfcConnection = nfcConnection;
    }

    /**
     * Recupera la cach&eacute; con el CAN de la tarjeta.
     * @return Cach&eacute; con el CAN de la tarjeta.
     */
    public CachePasswordCallback getCanPasswordCallback() {
        return canPasswordCallback;
    }

    /**
     * Establece la cach&eacute; con el CAN de la tarjeta.
     * @param canPasswordCallback Cach&eacute; con el CAN de la tarjeta.
     */
    public void setCanPasswordCallback(CachePasswordCallback canPasswordCallback) {
        this.canPasswordCallback = canPasswordCallback;
    }

    public CachePasswordCallback getPinPasswordCallback() {
        return pinPasswordCallback;
    }

    /**
     * Establece la cach&eacute; con el PIN de la tarjeta.
     * @param pinPasswordCallback Cach&eacute; con el PIN de la tarjeta.
     */
    public void setPinPasswordCallback(CachePasswordCallback pinPasswordCallback) {
        this.pinPasswordCallback = pinPasswordCallback;
    }

    /**
     * Recupera el Tag NFC del DNIe.
     * @return  Tag NFC del DNIe.
     */
    public Tag getDiscoveredTag() {
        return this.discoveredTag;
    }

    /**
     * Establece el Tag NFC del DNIe.
     * @param discoveredTag Tag NFC del DNIe.
     */
    public void setDiscoveredTag(Tag discoveredTag) {
        this.discoveredTag = discoveredTag;
    }

    /**
     * Resetea la configuraci&oacute;n establecida para que se deba volver a iniciar la
     * comunicaci&oacute;n con el DNIe. El CAN se mantiene ya que se interpreta que se desea seguir
     * usando el mismo DNIe.
     */
    public void reset() {
        this.callbackHandler = null;
        if (this.nfcConnection != null) {
            try {
                this.nfcConnection.close();
            }
            catch (Exception e) {
                Logger.w(ES_GOB_AFIRMA, "Error al cerrar la conexion con la tarjeta", e);
            }
            this.nfcConnection = null;
        }
    }

    /**
     * Reinicia el CAN almacenado.
     */
    public void clearCan() {
        if (this.canPasswordCallback != null) {
            this.canPasswordCallback.clearPassword();
            this.canPasswordCallback = null;
        }
    }

    /**
     * Reinicia el PIN almacenado.
     */
    public void clearPin() {
        if (this.pinPasswordCallback != null) {
            this.pinPasswordCallback.clearPassword();
            this.pinPasswordCallback = null;
        }
    }
}
