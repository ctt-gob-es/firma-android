package es.gob.afirma.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

import es.gob.afirma.R;

public class LanguageAdapter extends BaseAdapter {

    private ArrayList<String> languagesList;
    private Context context;

    public LanguageAdapter(ArrayList<String> languagesList, Context context) {
        this.languagesList = languagesList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return languagesList.size();
    }

    @Override
    public Object getItem(int position) {
        return languagesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String language = (String) getItem(position);
        convertView = LayoutInflater.from(context).inflate(R.layout.language_group, null);
        TextView tvGroup = convertView.findViewById(R.id.languageGroup);
        tvGroup.setText(language);
        return convertView;
    }
}
