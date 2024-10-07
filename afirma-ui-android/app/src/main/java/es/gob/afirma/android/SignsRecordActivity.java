package es.gob.afirma.android;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import es.gob.afirma.R;

public class SignsRecordActivity extends AppCompatActivity {

    private static final String ES_GOB_AFIRMA = "es.gob.afirma";

    private ListView signRecordLV;
    private SignsRecordAdapter adapter;
    private ArrayList<SignRecord> recordsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signs_record);

        MaterialToolbar toolbar = findViewById(R.id.signRecordToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        signRecordLV = this.findViewById(R.id.signsRecordLV);
        loadDataFromRecordsFile();
        loadDataToAdapter();

        if (recordsList.isEmpty()) {
            TextView noRecordsTv = findViewById(R.id.noRecordsTitle);
            noRecordsTv.setVisibility(View.VISIBLE);
        }

    }

    private void loadDataToAdapter() {
        adapter = new SignsRecordAdapter(recordsList, this);
        signRecordLV.setAdapter(adapter);
    }

    private void loadDataFromRecordsFile() {
        File directory = getFilesDir();
        String signsRecordFileName = "signsRecord.txt";
        File signRecordFile = new File(directory, signsRecordFileName);
        if (signRecordFile.exists()) {
            try (FileReader fr = new FileReader(signRecordFile)) {
                BufferedReader br = new BufferedReader(fr);
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(";");
                    recordsList.add(new SignRecord(parts[0], parts[1], parts[2], parts[3]));
                }
            } catch (Exception e) {
                Logger.e(ES_GOB_AFIRMA, "Error al leer datos del archivo de registro de firmas: " + e, e); //$NON-NLS-1$
            }
        }
    }

}
