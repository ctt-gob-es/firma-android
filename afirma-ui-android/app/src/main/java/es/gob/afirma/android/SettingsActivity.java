package es.gob.afirma.android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.BuildConfig;
import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;
import es.gob.afirma.android.util.Utils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setPortraitSmartphone(this);
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
        String langAbbrevation = LocaleHelper.langAbbrevationMap.get(lang);
        selectLangTv.setText(langAbbrevation.toUpperCase());

        // Dominios de confianza
        TextView trustedDomainsTv = this.findViewById(R.id.trustedDomainsTv);
        trustedDomainsTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), TrustedDomainsActivity.class);
                startActivity(intent);
            }
        });

        // Registro de firmas
        TextView signRecordTv = this.findViewById(R.id.signRecordTv);
        signRecordTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), SignsRecordActivity.class);
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

        // Activar uso de DNIe por NFC
        TextView activateDnieWithNFCTv = this.findViewById(R.id.activateDnieWithNFCTv);
        activateDnieWithNFCTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ActivateDnieWithNFCHelpActivity.class);
                startActivity(intent);
            }
        });

        // Como hacer firma visible
        TextView visibleSignTv = this.findViewById(R.id.howToDoVisibleSignTv);
        visibleSignTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), VisibleSignHelpActivity.class);
                startActivity(intent);
            }
        });

        // Como configurar dominios de confianza
        TextView howConfTrustedDomains = this.findViewById(R.id.howToConfigureTrustedDomainsTv);
        howConfTrustedDomains.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), TrustedDomainsHelpActivity.class);
                startActivity(intent);
            }
        });

        // Instalar un certificado
        TextView installCertsTv = this.findViewById(R.id.howToInsallCertsTv);
        installCertsTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ImportCertsHelpActivity.class);
                startActivityForResult(intent, 2);
            }
        });

        // Configuracion de la firma
        TextView signConfigTv = this.findViewById(R.id.signConfigTv);
        signConfigTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), SignConfigurationActivity.class);
                startActivity(intent);
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
                Intent intent = new Intent(getBaseContext(), LegalActivity.class);
                startActivity(intent);
            }
        });


        // Politica de privacidad
        TextView privacityTv = this.findViewById(R.id.privacityTv);
        privacityTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), PrivacyPolicyActivity.class);
                startActivity(intent);
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
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(getBaseContext(), HomeActivity.class);
        this.startActivity(i);
    }

}
