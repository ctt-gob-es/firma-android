package es.gob.afirma.android;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;

public class ConditionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conditions);

        Button acceptConditionsBtn = this.findViewById(R.id.acceptConditionsBtn);
        acceptConditionsBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AppConfig.setSkipConditionsScreen(true);
                Intent intent = new Intent(ConditionsActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        CheckBox readAndAcceptPrivacyChk = this.findViewById(R.id.readAndAcceptPrivacyChk);
        CheckBox readAndAcceptLegalChk = this.findViewById(R.id.readAndAcceptLegalChk);

        readAndAcceptPrivacyChk.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkButtonState(readAndAcceptPrivacyChk, readAndAcceptLegalChk, acceptConditionsBtn);
            }
        });

        readAndAcceptLegalChk.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkButtonState(readAndAcceptPrivacyChk, readAndAcceptLegalChk, acceptConditionsBtn);
            }
        });

    }

    private void checkButtonState(final CheckBox readAndAcceptPrivacyChk, final CheckBox readAndAcceptLegalChk, final Button acceptConditionsBtn) {
        if (readAndAcceptPrivacyChk.isChecked() && readAndAcceptLegalChk.isChecked()) {
            acceptConditionsBtn.setEnabled(true);
            acceptConditionsBtn.setTextColor(Color.WHITE);
        } else {
            acceptConditionsBtn.setEnabled(false);
            acceptConditionsBtn.setTextColor(Color.parseColor("#767676"));
        }
    }

    @Override
    public void onBackPressed() {
        // No hace nada
    }

}
