package es.gob.afirma.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.HelpDialog;

public class HomeActivity extends AppCompatActivity {

    protected final static String SIGNING_ERROR = "errorSigning"; //$NON-NLS-1$

    private final static String SHOW_SIGNING_RESULT = "showSigningResult"; //$NON-NLS-1$

    protected final static String START_IMPORT_CERT = "startImportCert"; //$NON-NLS-1$

    private final static String ERROR_TITLE_PARAM = "errorTitle"; //$NON-NLS-1$

    private final static String ERROR_MESSAGE_PARAM = "errorMessage"; //$NON-NLS-1$

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.topHomeToolbar);
        toolbar.setLogo(R.drawable.logo_autofirma);

        MainFragment mainFragment;
        Intent intent = getIntent();
        boolean showSigningResult = intent.getBooleanExtra(SHOW_SIGNING_RESULT, false);
        boolean signingError = intent.getBooleanExtra(SIGNING_ERROR, false);
        boolean startImportCert = intent.getBooleanExtra(START_IMPORT_CERT, false);
        if(showSigningResult) {
            String errorTitle = intent.getStringExtra(ERROR_TITLE_PARAM);
            String errorMessage = intent.getStringExtra(ERROR_MESSAGE_PARAM);
            mainFragment = new MainFragment(true, signingError, errorTitle, errorMessage);
        } else if (startImportCert) {
            mainFragment = new MainFragment(true);
        } else {
            mainFragment = new MainFragment();
        }

        intent.removeExtra(SHOW_SIGNING_RESULT);
        intent.removeExtra(SIGNING_ERROR);
        intent.removeExtra(START_IMPORT_CERT);

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.nav_enter, R.anim.nav_exit)
                .replace(R.id.home_content, mainFragment)
                .commit();

        if (getIntent().getBooleanExtra("CLOSE_ACTIVITY", false)) {
            finishAffinity();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent i = new Intent(this, SettingsActivity.class);
                this.startActivity(i);
                return true;
            case R.id.info:
                HelpDialog hd = new HelpDialog(this);
                hd.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        // No hace nada
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

}
