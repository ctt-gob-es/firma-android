package es.gob.afirma.android;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.CustomDialog;
import es.gob.afirma.android.gui.HelpDialog;
import es.gob.afirma.android.gui.SettingDialog;

public class HomeActivity extends AppCompatActivity {

    protected final static String SIGNED_FILE_RESULT = "signedFileResult"; //$NON-NLS-1$

    protected final static String START_IMPORT_CERT = "startImportCert"; //$NON-NLS-1$

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.topHomeToolbar);
        toolbar.setLogo(R.drawable.logo_autofirma);

        MainFragment mainFragment;
        Intent intent = getIntent();
        String signResult = intent.getStringExtra(SIGNED_FILE_RESULT);
        boolean startImportCert = intent.getBooleanExtra(START_IMPORT_CERT, false);
        if(signResult != null) {
            mainFragment = new MainFragment(signResult);
        } else if (startImportCert) {
            mainFragment = new MainFragment(startImportCert);
        } else {
            mainFragment = new MainFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.nav_enter, R.anim.nav_exit)
                .replace(R.id.home_content, mainFragment)
                .commit();
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

}
