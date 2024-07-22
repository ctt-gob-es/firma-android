package es.gob.afirma.android.gui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import es.gob.afirma.R;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.MainFragment;

public class SignWithDnieStep1Fragment extends Fragment {
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
                TextView stepTv = getActivity().findViewById(R.id.stepTv);
                stepTv.setText(getString(R.string.actual_step, "2"));

                TextView titleTv = getActivity().findViewById(R.id.titleTv);
                titleTv.setText(getString(R.string.enter_pin_dni));

                ProgressBar progressBar = getActivity().findViewById(R.id.signDnieStepsPb);
                progressBar.setProgress(2,true);

                SignWithDnieStep2Fragment signWithDnieStep2Fragment = new SignWithDnieStep2Fragment();
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.step_content, signWithDnieStep2Fragment)
                        .commit();
            }
        });

        return contentLayout;
    }
}
