package es.gob.afirma.android;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.R;

public class PermissionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        if (!NfcHelper.isNfcServiceAvailable(this)) {
            this.findViewById(R.id.permissionsSwitch).setEnabled(false);
        } else {
            ((Switch) this.findViewById(R.id.permissionsSwitch)).setChecked(
                    NfcHelper.isNfcPreferredConnection(this)
            );
        }

        final Switch switchNFC = this.findViewById(R.id.permissionsSwitch);
        switchNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NfcHelper.configureNfcAsPreferredConnection(switchNFC.isChecked());
            }
        });

        MaterialToolbar toolbar = findViewById(R.id.permissionsToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

}
