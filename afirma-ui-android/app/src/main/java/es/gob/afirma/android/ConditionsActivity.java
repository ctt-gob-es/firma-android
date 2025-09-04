package es.gob.afirma.android;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.Locale;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;
import es.gob.afirma.android.util.FileUtil;
import es.gob.afirma.android.util.Utils;

public class ConditionsActivity extends AppCompatActivity {

    Context ctx;
    int posSelected = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conditions);
        MaterialToolbar toolbar = findViewById(R.id.conditionsToolbar);
        this.setSupportActionBar(toolbar);
        Spinner spinner =  this.findViewById(R.id.spinner_languages);
        List<String> spinnerValues = List.of(getString(R.string.espanol), getString(R.string.english), getString(R.string.catala), getString(R.string.galego), getString(R.string.euskera), getString(R.string.valenciano));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.textview_spinner_selected, spinnerValues);
        adapter.setDropDownViewResource(R.layout.textview_spinner_drop);
        spinner.setAdapter(adapter);

        this.ctx = this;

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                posSelected = position;
                switch (position) {
                    case 0:
                        changeLang("es");
                        break;
                    case 1:
                        changeLang("en");
                        break;
                    case 2:
                        changeLang("ca");
                        break;
                    case 3:
                        changeLang("gl");
                        break;
                    case 4:
                        changeLang("eu");
                        break;
                    case 5:
                        changeLang("ca-ES-valencia");
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinner.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, R.layout.textview_spinner_selected_white, spinnerValues);
                    spinner.setAdapter(adapter);
                    adapter.setDropDownViewResource(R.layout.textview_spinner_drop);
                    spinner.setBackgroundResource(R.drawable.spinner_white_border);
                    spinner.setSelection(posSelected);
                } else {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, R.layout.textview_spinner_selected, spinnerValues);
                    spinner.setAdapter(adapter);
                    adapter.setDropDownViewResource(R.layout.textview_spinner_drop);
                    spinner.setBackgroundResource(R.drawable.spinner_black_border);
                    spinner.setSelection(posSelected);
                }
            }
        });

        changeLang("es");

        Button acceptConditionsBtn = this.findViewById(R.id.acceptConditionsBtn);
        acceptConditionsBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AppConfig.setSkipConditionsScreen(true);
                Intent intent = new Intent(ConditionsActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });
        acceptConditionsBtn.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    acceptConditionsBtn.setBackgroundResource(R.drawable.buttonblackbackground);
                } else {
                    acceptConditionsBtn.setBackgroundResource(R.drawable.buttonredbackground);
                }
            }
        });

        CheckBox readAndAcceptPrivacyChk = this.findViewById(R.id.readAndAcceptPrivacyChk);
        CheckBox readAndAcceptLegalChk = this.findViewById(R.id.readAndAcceptLegalChk);

        if (savedInstanceState != null) {
            //Recuperamos el estado de los checkbox en caso de que se haya girado la pantalla
            boolean readAndAcceptPrivacyChkState = savedInstanceState.getBoolean("readAndAcceptPrivacyChk", false); // Valor por defecto es false
            boolean readAndAcceptLegalChkState = savedInstanceState.getBoolean("readAndAcceptLegalChk", false);
            readAndAcceptPrivacyChk.setChecked(readAndAcceptPrivacyChkState);
            readAndAcceptLegalChk.setChecked(readAndAcceptLegalChkState);
        }

        readAndAcceptPrivacyChk.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkButtonState(readAndAcceptPrivacyChk, readAndAcceptLegalChk, acceptConditionsBtn);
            }
        });

        readAndAcceptLegalChk.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                checkButtonState(readAndAcceptPrivacyChk, readAndAcceptLegalChk, acceptConditionsBtn);
            }
        });

        checkButtonState(readAndAcceptPrivacyChk, readAndAcceptLegalChk, acceptConditionsBtn);

    }

    private void checkButtonState(final CheckBox readAndAcceptPrivacyChk, final CheckBox readAndAcceptLegalChk, final Button acceptConditionsBtn) {
        if (readAndAcceptPrivacyChk.isChecked() && readAndAcceptLegalChk.isChecked()) {
            acceptConditionsBtn.setEnabled(true);
            acceptConditionsBtn.setTextColor(Color.WHITE);
            acceptConditionsBtn.setBackgroundResource(R.drawable.buttonredbackground);
        } else {
            acceptConditionsBtn.setEnabled(false);
            acceptConditionsBtn.setTextColor(Color.parseColor("#767676"));
            acceptConditionsBtn.setBackgroundResource(R.drawable.buttongraybackground);
        }
    }

    private void changeLang(String lang) {
        LocaleHelper.setLocale(this, lang);
        refreshComponents(lang);
        String policyHtml = FileUtil.readPolicyFile(this, lang);
        String legalHtml = FileUtil.readLegalFile(this, lang);
        String htmlText = policyHtml + legalHtml;
        String encodedHtml = Base64.encodeToString(htmlText.getBytes(),
                Base64.NO_PADDING);
        WebView wvChild =  this.findViewById(R.id.contentConditionsWv);
        wvChild.loadData(encodedHtml, "text/html", "base64");
    }

    private void refreshComponents(String lang) {
        Resources res = getLocalizedResources(this, lang);
        getSupportActionBar().setTitle(res.getString(R.string.acceptance_conditions));
        TextView langTv = this.findViewById(R.id.langTv);
        langTv.setText(res.getString(R.string.language));
        CheckBox readAndAcceptPrivacyChk = this.findViewById(R.id.readAndAcceptPrivacyChk);
        readAndAcceptPrivacyChk.setText(res.getString(R.string.read_and_accept_privacy));
        CheckBox readAndAcceptLegalChk = this.findViewById(R.id.readAndAcceptLegalChk);
        readAndAcceptLegalChk.setText(res.getString(R.string.read_and_accept_legal));
        Button acceptBtn = this.findViewById(R.id.acceptConditionsBtn);
        acceptBtn.setText(res.getString(R.string.ok));
    }

    private Resources getLocalizedResources(Context context, String lang) {
        Locale locale = Locale.forLanguageTag(lang);
        Configuration conf = context.getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(locale);
        Context localizedContext = context.createConfigurationContext(conf);
        return localizedContext.getResources();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onBackPressed() {
        // No hace nada
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        CheckBox readAndAcceptPrivacyChk = this.findViewById(R.id.readAndAcceptPrivacyChk);
        outState.putBoolean("readAndAcceptPrivacyChk", readAndAcceptPrivacyChk.isChecked());
        CheckBox readAndAcceptLegalChk = this.findViewById(R.id.readAndAcceptLegalChk);
        outState.putBoolean("readAndAcceptLegalChk", readAndAcceptLegalChk.isChecked());
    }

}
