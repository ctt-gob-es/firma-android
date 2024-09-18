package es.gob.afirma.android;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.CustomDialog;

public class TrustedDomainsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_domains);

        MaterialToolbar toolbar = findViewById(R.id.trustedDomainsToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ((Switch) this.findViewById(R.id.checkSSLSwitch)).setChecked(
                CheckConnectionsHelper.isValidateSSLConnections(this)
        );

        ((EditText) this.findViewById(R.id.editTextTrustedDomains)).setText(
                CheckConnectionsHelper.getTrustedDomains(this)
        );

        final Switch switchValidateSSLConnections = TrustedDomainsActivity.this.findViewById(R.id.checkSSLSwitch);
        switchValidateSSLConnections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckConnectionsHelper.configureValidateSSLConnections(switchValidateSSLConnections.isChecked());
            }
        });

        Button saveChangesBtn = this.findViewById(R.id.saveChangesBtn);
        saveChangesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editTextTrustedDomains = TrustedDomainsActivity.this.findViewById(R.id.editTextTrustedDomains);
                String domains = editTextTrustedDomains.getText().toString().trim();
                try {
                    if (!domains.isEmpty()) {
                        checkCorrectDomainFormat(domains);
                    }
                    CheckConnectionsHelper.setTrustedDomains(domains);
                    CustomDialog cd = new CustomDialog(TrustedDomainsActivity.this, R.mipmap.check_icon, getString(R.string.changes_saved), getString(R.string.changes_saved_correctly), getString(R.string.understood));
                    cd.show();
                }
                catch (DomainFormatException e) {
                    Log.w("es.gob.afirma", "Se han encontrado entradas no validas en el listado de dominios", e);
                    CustomDialog cd = new CustomDialog(TrustedDomainsActivity.this, R.mipmap.error_icon, getString(R.string.error), getString(R.string.error_format_trusted_domains, e.getMessage()), getString(R.string.understood));
                    cd.show();
                }
            }
        });

    }


    /**
     * Comprueba que el formato de los dominios indicados sea el correcto.
     * @param domainsText Texto con todos los dominios.
     * @throws DomainFormatException Cuando se encuentra un dominio no v&aacute;lido.
     */
    private static void checkCorrectDomainFormat(final String domainsText) throws DomainFormatException {

        final String [] domainsArray = domainsText.split("\n");

        final String regex = "^[a-z0-9*][a-z0-9-.:]{1,61}[a-z0-9*]$";

        final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

        for (final String domain : domainsArray) {
            String domainCleaned = domain.trim();
            if (!domainCleaned.isEmpty()) {
                final Matcher matcher = pattern.matcher(domainCleaned);
                if (!matcher.matches()) {
                    throw new DomainFormatException(domainCleaned);
                }
            }
        }
    }

    /**
     * Se&ntilde;ala un error en un patr&oacute; de dominio.
     */
    private static class DomainFormatException extends Exception {

        /**
         * Construye la excepci&oacute;n con el patr&oacute; de dominio
         * inv&aacute;lido.
         * @param domain Patr&oacute;n de dominio inv&aacute;lido.
         */
        public DomainFormatException(String domain) {
            super(domain);
        }
    }

}
