package es.gob.afirma.android;

import static es.gob.afirma.android.LoadKeyStoreFragmentActivity.ERROR_LOADING_NFC_KEYSTORE;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.CustomDialog;
import es.gob.afirma.core.AOCancelledOperationException;

public class IntroSignDnieActivity extends LoadKeyStoreFragmentActivity {

    public static int actualStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_sign_dnie);

        actualStep = 0;

        MaterialToolbar toolbar = this.findViewById(R.id.introSignDnieToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Button startButton = this.findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(getBaseContext(), StepsSignDnieActivity.class);
                v.getContext().startActivity(intent);
            }
        });

        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(ERROR_LOADING_NFC_KEYSTORE)) {
            CustomDialog cd = new CustomDialog(this, R.mipmap.error_icon,
                    getString(R.string.error), getIntent().getStringExtra(ERROR_LOADING_NFC_KEYSTORE), getString(R.string.understood));
            cd.show();
        }

    }

    @Override
    public void onBackPressed() {
        if (actualStep == 0) {
            ksmListener.onLoadingKeyStoreError("Operacion cancelada",  new PendingIntent.CanceledException("Operacion cancelada"));
        } else {
            super.onBackPressed();
        }
    }
}
