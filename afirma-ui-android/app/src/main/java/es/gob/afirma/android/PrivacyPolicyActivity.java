package es.gob.afirma.android;

import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.R;
import es.gob.afirma.android.util.FileUtil;
import es.gob.afirma.android.util.Utils;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);

        MaterialToolbar toolbar = this.findViewById(R.id.privatyPolicyToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        WebView wvPolicy =  this.findViewById(R.id.contentprivatyPolicyWv);
        String privacyHtml = FileUtil.readPolicyFile(this, LocaleHelper.getPersistedData(this));
        String encodedHtml = Base64.encodeToString(privacyHtml.getBytes(),
                Base64.NO_PADDING);
        wvPolicy.loadData(encodedHtml, "text/html", "base64");

    }

}
