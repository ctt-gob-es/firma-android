package es.gob.afirma.android.batch;

/** Resultado de una operaci&oacute;n de firma por lotes.
 * @author Jos&eacute; Montero */
public final class SignBatchResult {

    private final String sign;

    SignBatchResult(String sign) {
        this.sign = sign;
    }

    public String getSignature() {
        return sign;
    }
}
