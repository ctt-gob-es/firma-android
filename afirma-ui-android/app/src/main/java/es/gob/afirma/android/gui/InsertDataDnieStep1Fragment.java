package es.gob.afirma.android.gui;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import es.gob.afirma.R;
import es.gob.afirma.android.NFCDetectorActivity;
import es.gob.afirma.android.StepsInsertDataDnieActivity;

import static es.gob.afirma.android.NFCDetectorActivity.INTENT_EXTRA_CAN_VALUE;

public class InsertDataDnieStep1Fragment extends Fragment {

    public static String canValue = null;

    private static final int CAN_LENGTH = 6;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View contentLayout = inflater.inflate(R.layout.fragment_signdnie_step1, container, false);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            String previousCan = bundle.getString(INTENT_EXTRA_CAN_VALUE);
            if (previousCan != null) {
                TextInputEditText canText = contentLayout.findViewById(R.id.canEtx);
                canText.setText(previousCan);
            }
        }

        Button continueToStep2Btn = contentLayout.findViewById(R.id.continueToStep2Btn);
        continueToStep2Btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                TextInputEditText canText = getActivity().findViewById(R.id.canEtx);

                if (isValidCan(canText)) {
                    canValue = canText.getText().toString();
                    loadStep2(canText.getText().toString());
                } else {
                    TextInputLayout canInputLayout = getActivity().findViewById(R.id.canEtxLayout);
                    canInputLayout.setError(getString(R.string.enter_valid_can));
                    canInputLayout.setErrorEnabled(true);
                }
            }
        });

        StepsInsertDataDnieActivity.actualStep = 1;

        return contentLayout;
    }

    private void loadStep2(String canText) {
        TextView stepTv = getActivity().findViewById(R.id.stepTv);
        stepTv.setText(getString(R.string.actual_step, "2"));

        TextView titleTv = getActivity().findViewById(R.id.titleTv);
        titleTv.setText(getString(R.string.enter_pin_dni));

        ProgressBar progressBar = getActivity().findViewById(R.id.signDnieStepsPb);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBar.setProgress(2,true);
        }

        InsertDataDnieStep2Fragment insertDataDnieStep2Fragment = new InsertDataDnieStep2Fragment();
        Bundle bundle = new Bundle();
        bundle.putString(NFCDetectorActivity.INTENT_EXTRA_CAN_VALUE, canText);
        insertDataDnieStep2Fragment.setArguments(bundle);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.step_content, insertDataDnieStep2Fragment)
                .commit();
    }

    private boolean isValidCan(TextInputEditText canText) {
        Editable text = canText.getText();
        if (text != null && !text.toString().isEmpty() && text.length() == CAN_LENGTH) {
            return true;
        }
        return false;
    }

}
