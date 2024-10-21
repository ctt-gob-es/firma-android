package es.gob.afirma.android;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.InsertDataDnieStep1Fragment;
import es.gob.afirma.android.gui.InsertDataDnieStep2Fragment;

public class StepsInsertDataDnieActivity extends AppCompatActivity {

    public static final int REQUEST_NFC_PARAMS = 2011;

    public static int actualStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps_sign_dnie);

        actualStep = 0;

        MaterialToolbar toolbar = this.findViewById(R.id.stepsSignDnieToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        loadStep1(InsertDataDnieStep1Fragment.canValue);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.close:
                Intent i = new Intent(this, HomeActivity.class);
                this.startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void loadStep1(String canText) {

        TextView titleTv = findViewById(R.id.titleTv);
        titleTv.setText(getString(R.string.enter_can_dni));

        TextView stepTv = this.findViewById(R.id.stepTv);
        stepTv.setText(getString(R.string.actual_step, "1"));

        InsertDataDnieStep1Fragment step1Fragment;
        step1Fragment = new InsertDataDnieStep1Fragment();
        Bundle bundle = new Bundle();
        bundle.putString(NFCDetectorActivity.INTENT_EXTRA_CAN_VALUE, canText);
        step1Fragment.setArguments(bundle);

        ProgressBar progressBar = this.findViewById(R.id.signDnieStepsPb);
        progressBar.setMax(3);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBar.setProgress(1,true);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.step_content, step1Fragment)
                .commit();
    }

    private void loadStep2(String canText) {
        TextView stepTv = this.findViewById(R.id.stepTv);
        stepTv.setText(getString(R.string.actual_step, "2"));

        TextView titleTv = this.findViewById(R.id.titleTv);
        titleTv.setText(getString(R.string.enter_pin_dni));

        ProgressBar progressBar = this.findViewById(R.id.signDnieStepsPb);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBar.setProgress(2,true);
        }

        InsertDataDnieStep2Fragment insertDataDnieStep2Fragment = new InsertDataDnieStep2Fragment();
        Bundle bundle = new Bundle();
        bundle.putString(NFCDetectorActivity.INTENT_EXTRA_CAN_VALUE, canText);
        insertDataDnieStep2Fragment.setArguments(bundle);
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.step_content, insertDataDnieStep2Fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        // Se intenta volver a la pantalla de introduccion
        if (actualStep == 2) {
            actualStep--;
            loadStep1(InsertDataDnieStep1Fragment.canValue);
            return;
        // Se intenta volver a la pantalla de PIN
        } else if (actualStep == 3) {
            actualStep--;
            loadStep2(InsertDataDnieStep1Fragment.canValue);
            return;
        }
        super.onBackPressed();
    }
}
