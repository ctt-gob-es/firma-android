package es.gob.afirma.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import es.gob.afirma.R;

public class FaqAdapter extends BaseExpandableListAdapter {

    private ArrayList<String> questionsList;
    private Map<String, String> mapChild;
    private Context context;

    public FaqAdapter(ArrayList<String> questionsList, Map<String, String> mapChild, Context context) {
        this.questionsList = questionsList;
        this.mapChild = mapChild;
        this.context = context;
    }

    @Override
    public int getGroupCount() {
        return questionsList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return questionsList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mapChild.get(questionsList.get(groupPosition));
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String question = (String) getGroup(groupPosition);
        convertView = LayoutInflater.from(context).inflate(R.layout.faq_group, null);
        TextView tvGroup = convertView.findViewById(R.id.faqGroup);
        tvGroup.setText(question);

        ImageView imgExpandCollapse = convertView.findViewById(R.id.groupIndicatorImg);

        // check if GroupView is expanded and set imageview for expand/collapse-action
        if(isExpanded){
            imgExpandCollapse.setImageResource(R.mipmap.chevron_up);
        }
        else{
            imgExpandCollapse.setImageResource(R.mipmap.chevron_down);
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String item = (String) getChild(groupPosition, childPosition);
        convertView = LayoutInflater.from(context).inflate(R.layout.faq_child, null);
        TextView tvChild =  convertView.findViewById(R.id.faqChild);
        tvChild.setText(item);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
