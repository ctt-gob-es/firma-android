package es.gob.afirma.android;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import es.gob.afirma.R;

public class LanguageActivity extends AppCompatActivity {

    private ListView languagesLV;
    private FaqAdapter adapter;
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
                String language = (String) adapter.getItem(position);
                Toast.makeText(getApplicationContext(), language, Toast.LENGTH_SHORT);
            }
        });

    }

}
