package es.gob.afirma.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import es.gob.afirma.R;

import static es.gob.afirma.android.SignFragmentActivity.SIGN_TYPE_LOCAL;
import static es.gob.afirma.android.SignFragmentActivity.SIGN_TYPE_WEB;
import static es.gob.afirma.android.batch.SignBatchFragmentActivity.SIGN_TYPE_BATCH;
import static es.gob.afirma.android.crypto.SignTask.OP_COSIGN;
import static es.gob.afirma.android.crypto.SignTask.OP_COUNTERSIGN;
import static es.gob.afirma.android.crypto.SignTask.OP_SIGN;

public class SignsRecordAdapter extends BaseAdapter {

    private ArrayList<SignRecord> recordsList;

    private Context context;

    public SignsRecordAdapter(ArrayList<SignRecord> recordsList, Context context) {
        this.recordsList = recordsList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return recordsList.size();
    }

    @Override
    public Object getItem(int position) {
        return recordsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SignRecord signRecord = (SignRecord) getItem(position);
        convertView = LayoutInflater.from(context).inflate(R.layout.sign_record_group, null);
        TextView dateTv = convertView.findViewById(R.id.dateTv);
        dateTv.setText(signRecord.getSignDate());
        TextView recordDescTv = convertView.findViewById(R.id.descTv);
        String signType = signRecord.getSignType();
        String signOp = null;
        switch (signRecord.getSignOperation()) {
            case OP_SIGN:
                signOp = context.getString(R.string.sign);
                break;
            case OP_COSIGN:
                signOp = context.getString(R.string.cosign);
                break;
            case OP_COUNTERSIGN:
                signOp = context.getString(R.string.countersign);
                break;
        }
        switch (signType) {
            case SIGN_TYPE_LOCAL:
                recordDescTv.setText(context.getString(R.string.local_sign_record, signOp, signRecord.getAppName()));
                break;
            case SIGN_TYPE_WEB:
                recordDescTv.setText(context.getString(R.string.web_sign_record, signOp, signRecord.getAppName()));
                break;
            case SIGN_TYPE_BATCH:
                recordDescTv.setText(context.getString(R.string.batch_sign_record, signRecord.getAppName()));
                break;
        }
        return convertView;
    }
}
