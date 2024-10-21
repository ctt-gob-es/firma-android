package es.gob.afirma.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.R;

public class IntroUseDnieActivity extends FragmentActivity {

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
                Intent intent = new Intent(getBaseContext(), StepsInsertDataDnieActivity.class);
                startActivityForResult(intent, StepsInsertDataDnieActivity.REQUEST_NFC_PARAMS);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        // Volvemos del proceso de insercion de CAN y PIN del DNIe y se lo devolvemos a la
        // clase de carga de almacenes
		if (requestCode == StepsInsertDataDnieActivity.REQUEST_NFC_PARAMS && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {

        setResult(RESULT_CANCELED);
        finish();

        super.onBackPressed();
    }
}
