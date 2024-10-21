package es.gob.afirma.android.gui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import es.gob.afirma.R;
import es.gob.afirma.android.StepsInsertDataDnieActivity;

import static android.app.Activity.RESULT_OK;

public class InsertDataDnieStep3Fragment extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View contentLayout = inflater.inflate(R.layout.fragment_signdnie_step3, container, false);

        Button readDnieBtn = contentLayout.findViewById(R.id.readDnieBtn);
        readDnieBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                final Intent resultNFC = new Intent();
                Bundle bundle = InsertDataDnieStep3Fragment.this.getArguments();
                if (bundle != null) {
                    resultNFC.putExtra(getString(R.string.extra_can), bundle.getString(getString(R.string.extra_can)));
                    resultNFC.putExtra(getString(R.string.extra_pin), bundle.getString(getString(R.string.extra_pin)));
                }

                getActivity().setResult(RESULT_OK, resultNFC);
                getActivity().finish();
            }
        });

        StepsInsertDataDnieActivity.actualStep = 3;

        return contentLayout;
    }

}
