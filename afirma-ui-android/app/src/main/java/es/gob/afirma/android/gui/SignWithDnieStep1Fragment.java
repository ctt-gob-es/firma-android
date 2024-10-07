package es.gob.afirma.android.gui;

import android.os.Build;
import android.os.Bundle;
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
import es.gob.afirma.android.StepsSignDnieActivity;

public class SignWithDnieStep1Fragment extends Fragment {

    public static String canValue = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View contentLayout;

        super.onCreate(savedInstanceState);
        contentLayout = inflater.inflate(R.layout.fragment_signdnie_step1, container, false);

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

        StepsSignDnieActivity.actualStep = 1;

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

        SignWithDnieStep2Fragment signWithDnieStep2Fragment = new SignWithDnieStep2Fragment();
        Bundle bundle = new Bundle();
        bundle.putString(NFCDetectorActivity.INTENT_EXTRA_CAN_VALUE, canText);
        signWithDnieStep2Fragment.setArguments(bundle);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.step_content, signWithDnieStep2Fragment)
                .commit();
    }

    private boolean isValidCan(TextInputEditText canText) {
        if (canText.getText() != null && !canText.getText().toString().isEmpty()) {
            return true;
        }
        return false;
    }

}
