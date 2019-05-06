package es.gob.afirma.android.crypto;

import java.security.cert.Certificate;

/** Resultado de una operaci&oacute;n de firma.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class SignResult {

    private final byte[] sign;
    private final Certificate signingCert;

    SignResult(final byte[] s, final Certificate cert) {
        this.sign = s != null ? s.clone() : null;
        this.signingCert = cert;
    }

    public byte[] getSignature() {
        return this.sign != null ? sign.clone() : null;
    }

    public Certificate getSigningCertificate() {
        return this.signingCert;
    }

}
