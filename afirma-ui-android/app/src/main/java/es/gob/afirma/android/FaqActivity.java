package es.gob.afirma.android;

import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import es.gob.afirma.R;

public class FaqActivity extends AppCompatActivity {

    private ExpandableListView expLV;
    private FaqAdapter adapter;
    private ArrayList<String> questionsList;
    private Map<String, String> mapChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        MaterialToolbar toolbar = findViewById(R.id.faqToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        expLV = this.findViewById(R.id.faqLV);
        questionsList = new ArrayList<>();
        mapChild = new HashMap<>();

        loadData();

    }

    private void loadData() {

        questionsList.add(getResources().getString(R.string.question_1));
        questionsList.add(getResources().getString(R.string.question_2));
        questionsList.add(getResources().getString(R.string.question_3));
        questionsList.add(getResources().getString(R.string.question_4));

        mapChild.put(questionsList.get(0), getResources().getString(R.string.response_1));
        mapChild.put(questionsList.get(1), getResources().getString(R.string.response_2));
        mapChild.put(questionsList.get(2), getResources().getString(R.string.response_3));
        mapChild.put(questionsList.get(3), getResources().getString(R.string.response_4));

        adapter = new FaqAdapter(questionsList, mapChild,this);
        expLV.setAdapter(adapter);

    }

}
