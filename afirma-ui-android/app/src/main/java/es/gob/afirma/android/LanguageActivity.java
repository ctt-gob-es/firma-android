package es.gob.afirma.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

import es.gob.afirma.R;

public class LanguageActivity extends AppCompatActivity {

    private ListView languagesLV;
    private ArrayList<String> languagesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);

        MaterialToolbar toolbar = findViewById(R.id.languageToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        languagesLV = this.findViewById(R.id.languageLV);
        languagesList = new ArrayList<>();

        loadData();
    }

    private void loadData() {

        languagesList.add(getResources().getString(R.string.espanol));
        languagesList.add(getResources().getString(R.string.english));
        languagesList.add(getResources().getString(R.string.french));
        languagesList.add(getResources().getString(R.string.catala));
        languagesList.add(getResources().getString(R.string.galego));
        languagesList.add(getResources().getString(R.string.euskera));
        languagesList.add(getResources().getString(R.string.valenciano));

        LanguageAdapter adapter = new LanguageAdapter(languagesList, this);
        languagesLV.setAdapter(adapter);
        languagesLV.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        languagesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        changeLang("en");
                        break;
                    case 2:
                        changeLang("fr");
                        break;
                    case 3:
                        changeLang("ca");
                        break;
                    case 4:
                        changeLang("ga");
                        break;
                    case 5:
                        changeLang("eu");
                        break;
                    case 6:
                        changeLang("va");
                        break;
                    default:
                        changeLang("es");
                }
            }
        });

    }

    public void changeLang(String lang) {
        LocaleHelper.setLocale(this, lang);
        this.recreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(this, SettingsActivity.class);
        this.startActivity(i);
    }
}
