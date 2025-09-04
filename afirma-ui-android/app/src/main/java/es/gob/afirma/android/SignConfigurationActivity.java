package es.gob.afirma.android;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;
import es.gob.afirma.android.gui.CustomDialog;
import es.gob.afirma.android.gui.SelectCacheMinutesDialog;

public class SignConfigurationActivity extends AppCompatActivity {

    public static Integer timeoutMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_configuration);

        MaterialToolbar toolbar = findViewById(R.id.signConfigToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if (!NfcHelper.isNfcServiceAvailable(this)) {
            this.findViewById(R.id.allowDnieWithNFCSwitch).setEnabled(false);
            LinearLayout allowNFCLayout = findViewById(R.id.allowNFCLayout);
            View allowNFCSeparator = findViewById(R.id.allowNFCSeparator);
            allowNFCLayout.setVisibility(View.GONE);
            allowNFCSeparator.setVisibility(View.GONE);
        } else {
            ((Switch) this.findViewById(R.id.allowDnieWithNFCSwitch)).setChecked(
                    NfcHelper.isNfcPreferredConnection(this)
            );
        }

        final Switch switchNFC = this.findViewById(R.id.allowDnieWithNFCSwitch);

        final Switch switchPadesVisibleSignature = this.findViewById(R.id.padesVisibleSignatureSwitch);
        switchPadesVisibleSignature.setChecked(AppConfig.isPadesVisibleSignature(this));

        final Switch switchObfuscateCertInfo = this.findViewById(R.id.obfuscateUserCertInfoSwitch);
        switchObfuscateCertInfo.setChecked(AppConfig.isPadesObfuscateCertInfo(this));

        this.timeoutMinutes = AppConfig.getStickySignatureTimeout(this);

        Button minutesTimeoutButton = findViewById(R.id.timeoutButton);
        minutesTimeoutButton.setText(this.timeoutMinutes + " Min");
        Context ctx = this;
        minutesTimeoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectCacheMinutesDialog minutesDialog = new SelectCacheMinutesDialog(minutesTimeoutButton, ctx);
                minutesDialog.show();
            }
        });

        Button saveChangesBtn = this.findViewById(R.id.saveChangesBtn);
        saveChangesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NfcHelper.configureNfcAsPreferredConnection(switchNFC.isChecked());
                AppConfig.setPadesVisibleSignature(switchPadesVisibleSignature.isChecked());
                AppConfig.setPadesObfuscateCertInfo(switchObfuscateCertInfo.isChecked());
                AppConfig.setStickySignatureTimeout(timeoutMinutes);
                CustomDialog cd = new CustomDialog(SignConfigurationActivity.this, R.drawable.check_icon, getString(R.string.changes_saved), getString(R.string.changes_saved_correctly), getString(R.string.understood));
                cd.show();
            }
        });
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

}
