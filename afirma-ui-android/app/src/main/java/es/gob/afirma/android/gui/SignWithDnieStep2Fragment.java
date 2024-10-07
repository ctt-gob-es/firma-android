package es.gob.afirma.android.gui;

import static es.gob.afirma.android.NFCDetectorActivity.INTENT_EXTRA_CAN_VALUE;

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
import es.gob.afirma.android.StepsSignDnieActivity;

public class SignWithDnieStep2Fragment extends Fragment {

    public static final String INTENT_EXTRA_PIN_VALUE = "pinValue"; //$NON-NLS-1$

    String canValue;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View contentLayout;

        super.onCreate(savedInstanceState);
        contentLayout = inflater.inflate(R.layout.fragment_signdnie_step2, container, false);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            canValue = bundle.getString(INTENT_EXTRA_CAN_VALUE);
        }

        Button continueToStep3Btn = contentLayout.findViewById(R.id.continueToStep3Btn);
        continueToStep3Btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                TextInputEditText pinText = getActivity().findViewById(R.id.pinEtx);

                if (isValidPin(pinText)) {
                    TextView stepTv = getActivity().findViewById(R.id.stepTv);
                    stepTv.setText(getString(R.string.actual_step, "3"));

                    TextView titleTv = getActivity().findViewById(R.id.titleTv);
                    titleTv.setText(getString(R.string.read_dnie_with_smartphone));

                    ProgressBar progressBar = getActivity().findViewById(R.id.signDnieStepsPb);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        progressBar.setProgress(3,true);
                    }

                    SignWithDnieStep3Fragment signWithDnieStep3Fragment = new SignWithDnieStep3Fragment();
                    Bundle bundle = new Bundle();

                    bundle.putString(INTENT_EXTRA_CAN_VALUE, canValue);
                    bundle.putString(INTENT_EXTRA_PIN_VALUE, pinText.getText().toString());
                    signWithDnieStep3Fragment.setArguments(bundle);

                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.step_content, signWithDnieStep3Fragment)
                            .commit();
                } else {
                    TextInputLayout pinInputLayout = getActivity().findViewById(R.id.pinEtxLayout);
                    pinInputLayout.setError(getString(R.string.enter_valid_pin));
                    pinInputLayout.setErrorEnabled(true);
                }
            }
        });

        StepsSignDnieActivity.actualStep = 2;

        return contentLayout;
    }

    private boolean isValidPin(TextInputEditText pinText) {
        if (pinText.getText() != null && !pinText.getText().toString().isEmpty()) {
            return true;
        }
        return false;
    }

}
