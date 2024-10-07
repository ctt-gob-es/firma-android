package es.gob.afirma.android.gui;

import static es.gob.afirma.android.NFCDetectorActivity.INTENT_EXTRA_CAN_VALUE;
import static es.gob.afirma.android.gui.SignWithDnieStep2Fragment.INTENT_EXTRA_PIN_VALUE;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import es.gob.afirma.R;
import es.gob.afirma.android.LoadKeyStoreFragmentActivity;
import es.gob.afirma.android.StepsSignDnieActivity;

public class SignWithDnieStep3Fragment extends Fragment{

    String canValue;
    String pinValue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View contentLayout;

        super.onCreate(savedInstanceState);
        contentLayout = inflater.inflate(R.layout.fragment_signdnie_step3, container, false);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            canValue = bundle.getString(INTENT_EXTRA_CAN_VALUE);
            pinValue = bundle.getString(INTENT_EXTRA_PIN_VALUE);
        }

        Button readDnieBtn = contentLayout.findViewById(R.id.readDnieBtn);
        readDnieBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final Intent resultNFC = new Intent(getContext(), LoadKeyStoreFragmentActivity.class);
                resultNFC.putExtra(INTENT_EXTRA_CAN_VALUE, canValue);
                resultNFC.putExtra(INTENT_EXTRA_PIN_VALUE, pinValue);
                startActivity(resultNFC);
            }
        });

        StepsSignDnieActivity.actualStep = 3;

        return contentLayout;
    }

}
