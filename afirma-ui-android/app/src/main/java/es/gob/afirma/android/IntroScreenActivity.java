package es.gob.afirma.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;

public class IntroScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_screen_file);

        Button startButton = this.findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AppConfig.setSkipIntroScreen(true);
                Intent intent = new Intent(IntroScreenActivity.this, ConditionsActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onBackPressed() {
        // No hace nada
    }
}
