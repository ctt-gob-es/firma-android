package es.gob.afirma.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.BuildConfig;
import es.gob.afirma.R;

public class VersionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_version);

        MaterialToolbar toolbar = this.findViewById(R.id.versionToolbar);
        toolbar.setTitle(getString(R.string.appversion, BuildConfig.VERSION_NAME));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        TextView question1Tv = this.findViewById(R.id.p1Tv);
        question1Tv.setText(getString(R.string.version_desc_p1, BuildConfig.VERSION_NAME));

        TextView link1Tv = this.findViewById(R.id.l1Tv);
        link1Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(getString(R.string.version_desc_link1)));
                startActivity(i);
            }
        });

        TextView link2Tv = this.findViewById(R.id.l2Tv);
        link2Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(getString(R.string.version_desc_link2)));
                startActivity(i);
            }
        });

        TextView link3Tv = this.findViewById(R.id.l3Tv);
        link3Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(getString(R.string.version_desc_link3)));
                startActivity(i);
            }
        });

    }

}
