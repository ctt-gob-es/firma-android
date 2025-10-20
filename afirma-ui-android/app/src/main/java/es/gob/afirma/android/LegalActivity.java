package es.gob.afirma.android;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.R;
import es.gob.afirma.android.util.FileUtil;

public class LegalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);

        MaterialToolbar toolbar = this.findViewById(R.id.legalToolbar);
        toolbar.setNavigationContentDescription(getString(R.string.go_back));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        WebView wvLegal = this.findViewById(R.id.contentLegalWv);
        String legalHtml = FileUtil.readLegalFile(this, LocaleHelper.getPersistedData(this));
        String baseUrl = "file:///android_asset/";
        wvLegal.loadDataWithBaseURL(baseUrl, legalHtml, "text/html", "utf-8", null);

    }

}
