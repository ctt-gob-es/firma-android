package es.gob.afirma.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.afirma_splash_screen_file);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreenActivity.this, IntroScreenActivity.class);
                if (AppConfig.isSkipIntroScreen(getApplicationContext())) {
                    if (AppConfig.isSkipConditionsScreen(getApplicationContext())) {
                        intent = new Intent(SplashScreenActivity.this, HomeActivity.class);
                    } else {
                        intent = new Intent(SplashScreenActivity.this, ConditionsActivity.class);
                    }
                }
                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}
