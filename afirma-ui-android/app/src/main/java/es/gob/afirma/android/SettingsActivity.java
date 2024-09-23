package es.gob.afirma.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import junit.runner.Version;

import es.gob.afirma.BuildConfig;
import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;
import es.gob.afirma.android.gui.CertImportInstructionsActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Idioma
        TextView languageTv = this.findViewById(R.id.languageTv);
        languageTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), LanguageActivity.class);
                startActivity(intent);
            }
        });

        // Abreviatura de idioma seleccionado
        TextView selectLangTv = this.findViewById(R.id.languageSelectedTv);
        String lang = AppConfig.getLocaleConfig(this);
        selectLangTv.setText(lang.toUpperCase());

        // Permisos
        TextView permissionsTv = this.findViewById(R.id.permissionsTv);
        permissionsTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), PermissionsActivity.class);
                startActivity(intent);
            }
        });

        // Dominios de confianza
        TextView trustedDomainsTv = this.findViewById(R.id.trustedDomainsTv);
        trustedDomainsTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), TrustedDomainsActivity.class);
                startActivity(intent);
            }
        });

        // FAQ
        TextView faqTv = this.findViewById(R.id.faqTv);
        faqTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), FaqActivity.class);
                startActivity(intent);
            }
        });

        // Instalar un certificado
        TextView installCertsTv = this.findViewById(R.id.howToInsallCertsTv);
        installCertsTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), CertImportInstructionsActivity.class);
                startActivityForResult(intent, 2);
            }
        });

        // Declaracion de accesibilidad
        TextView accesibilityTv = this.findViewById(R.id.accesibilityTv);
        accesibilityTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(getString(R.string.accessibility_statement_url)));
                startActivity(i);
            }
        });

        // Aviso legal
        TextView legalTv = this.findViewById(R.id.legalTv);
        legalTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(getString(R.string.accessibility_statement_url)));
                startActivity(i);
            }
        });

        // Politica de privacidad
        TextView privacityTv = this.findViewById(R.id.privacityTv);
        privacityTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(getString(R.string.privacy_policy_url)));
                startActivity(i);
            }
        });

        // Version
        TextView versionTv = this.findViewById(R.id.versionTv);
        versionTv.setText(getString(R.string.appversion, BuildConfig.VERSION_NAME));
        versionTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), VersionActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(this, HomeActivity.class);
        this.startActivity(i);
    }

}
