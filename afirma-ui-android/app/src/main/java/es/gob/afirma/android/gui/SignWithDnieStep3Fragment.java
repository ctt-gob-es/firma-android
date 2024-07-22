package es.gob.afirma.android.gui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import es.gob.afirma.R;
import es.gob.afirma.android.NFCDetectorActivity;

public class SignWithDnieStep3Fragment extends Fragment {

    private final static int REQUEST_CODE_DETECT_NFC_CARD = 2001;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View contentLayout;

        super.onCreate(savedInstanceState);
        contentLayout = inflater.inflate(R.layout.fragment_signdnie_step3, container, false);

        Button readDnieBtn = contentLayout.findViewById(R.id.readDnieBtn);
        readDnieBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final Intent intentNFC = new Intent(getActivity(), NFCDetectorActivity.class);
                startActivityForResult(intentNFC, REQUEST_CODE_DETECT_NFC_CARD);
            }
        });

        return contentLayout;
    }
}
