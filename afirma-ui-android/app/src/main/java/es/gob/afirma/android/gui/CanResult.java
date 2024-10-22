package es.gob.afirma.android.gui;

import es.gob.afirma.android.crypto.CachePasswordCallback;

/**
 * Created by carlos on 11/07/2016.
 */
public class CanResult {

    private boolean canObtained;
    private CachePasswordCallback passwordCallback;

    public CanResult() {
        this.canObtained = false;
        this.passwordCallback = null;
    }

    public boolean isCanObtained() {
        return canObtained;
    }

    public CachePasswordCallback getPasswordCallback() {
        return passwordCallback;
    }

    public void setCanObtained(boolean canObtained) {
        this.canObtained = canObtained;
    }

    public void setPasswordCallback(CachePasswordCallback passwordCallback) {
        this.passwordCallback = passwordCallback;
    }
}
