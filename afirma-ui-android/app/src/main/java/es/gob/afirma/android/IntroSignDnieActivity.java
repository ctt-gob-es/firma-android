package es.gob.afirma.android;

import static es.gob.afirma.android.LoadKeyStoreFragmentActivity.ERROR_LOADING_NFC_KEYSTORE;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.CustomDialog;

public class IntroSignDnieActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_sign_dnie);

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
                finish();
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
        Intent i = new Intent(this, HomeActivity.class);
        this.startActivity(i);
    }
}
