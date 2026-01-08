package es.gob.afirma.android.crypto;

import android.nfc.tech.IsoDep;

import es.gob.afirma.android.Logger;
import es.gob.afirma.android.errors.AppErrorCode;
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
    private ApduConnection nfcConnection;
    private IsoDep isoDep;

    private DnieConnectionManager() {
        this.canPasswordCallback = null;
        this.pinPasswordCallback = null;
        this.nfcConnection = null;
        this.isoDep = null;
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
     * Devuelve la conexi&oacute;n ISO DEP con la tarjeta.
     */
    public IsoDep getIsoDepConnection() {
        return this.isoDep;
    }

    /**
     * Establece el la conexi&oacute;n ISO DEP ya establicida con la tarjeta.
     * @param isoDep Conex&oacute;n ISO DEP.
     */
    public void setIsoDepConnection(IsoDep isoDep) {
        this.isoDep = isoDep;
    }

    /**
     * Resetea la configuraci&oacute;n establecida para que se deba volver a iniciar la
     * comunicaci&oacute;n con el DNIe. El CAN se mantiene ya que se interpreta que se desea seguir
     * usando el mismo DNIe.
     */
    public void reset() {
        if (this.nfcConnection != null) {
            try {
                this.nfcConnection.close();
            }
            catch (Exception e) {
                Logger.w(ES_GOB_AFIRMA, AppErrorCode.Hardware.RESET_NFC.toString(), e);
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
