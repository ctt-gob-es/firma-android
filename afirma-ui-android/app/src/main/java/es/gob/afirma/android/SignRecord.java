package es.gob.afirma.android;

/** Clase que contiene los datos le&iacute;dos en el registro de firmas para mostrarlos en la vista. */
public class SignRecord {

    private String signDate;
    private String signType;
    private String appName;
    private String signOperation;

    public SignRecord(String signDate, String signType, String appName, String signOperation) {
        this.signDate = signDate;
        this.signType = signType;
        this.appName = appName;
        this.signOperation = signOperation;
    }

    public String getSignDate() {
        return signDate;
    }

    public String getSignType() {
        return signType;
    }

    public String getAppName() {
        return appName;
    }

    public String getSignOperation() {
        return signOperation;
    }

}
