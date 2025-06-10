package es.gob.afirma.android.gui;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import es.gob.afirma.R;
import es.gob.afirma.android.NFCDetectorActivity;
import es.gob.afirma.android.StepsInsertDataDnieActivity;
import es.gob.afirma.android.errors.AppErrorCode;
import es.gob.afirma.android.errors.ErrorMapper;

public class InsertDataDnieStep3Fragment extends Fragment{

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View contentLayout = inflater.inflate(R.layout.fragment_signdnie_step3, container, false);

        Bundle bundle = getArguments();

        Button readDnieBtn = contentLayout.findViewById(R.id.readDnieBtn);
        readDnieBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                final Intent resultNFC = new Intent();
                if (bundle != null) {
                    resultNFC.putExtra(getString(R.string.extra_can), bundle.getString(getString(R.string.extra_can)));
                    resultNFC.putExtra(getString(R.string.extra_pin), bundle.getString(getString(R.string.extra_pin)));
                }

                getActivity().setResult(RESULT_OK, resultNFC);
                getActivity().finish();
            }
        });

        StepsInsertDataDnieActivity.actualStep = 3;

        if (bundle != null && bundle.getBoolean(NFCDetectorActivity.INTENT_EXTRA_ERROR_READING_CARD))  {
            CustomDialog cd = new CustomDialog(getActivity(), R.drawable.warn_icon, getString(R.string.error_reading_dnie),
                    ErrorMapper.getErrorMsgFormatted(this.getContext(), AppErrorCode.ThirdParty.ERROR_INITIALIZING_CARD),
                    getString(R.string.try_again), true, getString(R.string.cancel_underline));
            CustomDialog finalCd = cd;
            cd.setAcceptButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finalCd.hide();
                }
            });
            cd.setCancelButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finalCd.hide();
                    getActivity().setResult(RESULT_CANCELED);
                    getActivity().finish();
                }
            });
            cd.show();
        }

        return contentLayout;
    }

}
