package es.gob.afirma.android;

import android.widget.RelativeLayout;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import es.gob.afirma.R;

public class IntroScreenActivity extends AppCompatActivity {

    RelativeLayout bottomSheetRL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_screen_file);
        bottomSheetRL = this.findViewById(R.id.customDialog);

        Button startButton = this.findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(IntroScreenActivity.this, ConditionsActivity.class);
                startActivity(intent);
            }
        });


    }
}
