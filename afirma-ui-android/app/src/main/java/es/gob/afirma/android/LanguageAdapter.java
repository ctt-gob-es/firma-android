package es.gob.afirma.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;

public class LanguageAdapter extends BaseAdapter {

    private ArrayList<String> languagesList;

    private Context context;

    public static Map<String, String> localeMap;

    public LanguageAdapter(ArrayList<String> languagesList, Context context) {
        this.languagesList = languagesList;
        this.context = context;
        localeMap = new HashMap<>();
        localeMap.put("es", context.getString(R.string.espanol));
        localeMap.put("en", context.getString(R.string.english));
        localeMap.put("fr", context.getString(R.string.french));
        localeMap.put("ca", context.getString(R.string.catala));
        localeMap.put("ga", context.getString(R.string.galego));
        localeMap.put("eu", context.getString(R.string.euskera));
        localeMap.put("va", context.getString(R.string.valenciano));
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
        String langConf = AppConfig.getLocaleConfig(context);
        if (localeMap.get(langConf).equals(language)) {
            convertView.findViewById(R.id.languageIndicatorImg).setVisibility(View.VISIBLE);
        }
        return convertView;
    }
}
