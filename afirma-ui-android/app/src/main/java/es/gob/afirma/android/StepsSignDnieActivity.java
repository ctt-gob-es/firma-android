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
import es.gob.afirma.android.gui.SignWithDnieStep1Fragment;

public class StepsSignDnieActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps_sign_dnie);

        MaterialToolbar toolbar = this.findViewById(R.id.stepsSignDnieToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        TextView stepTv = this.findViewById(R.id.stepTv);
        stepTv.setText(getString(R.string.actual_step, "1"));

        SignWithDnieStep1Fragment step1Fragment;
        step1Fragment = new SignWithDnieStep1Fragment();

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

}
