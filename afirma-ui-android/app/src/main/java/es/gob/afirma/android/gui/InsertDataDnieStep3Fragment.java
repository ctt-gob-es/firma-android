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
import es.gob.afirma.android.LoadKeyStoreFragmentActivity;
import es.gob.afirma.android.Logger;
import es.gob.afirma.android.StepsInsertDataDnieActivity;
import es.gob.afirma.android.errors.AppErrorCode;
import es.gob.afirma.android.errors.ErrorMapper;
import es.gob.afirma.core.ErrorCode;

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

        // Si se han recibido datos, se comprueba si alguno es un error y, en caso afirmativo,
        // se muestra
        if (bundle != null && bundle.containsKey(getString(R.string.extra_smartcard_error)))  {

            int errorMessage;
            ErrorCode errorCode;
            String errorCodeText = bundle.getString(getString(R.string.extra_smartcard_error));
            if (AppErrorCode.ThirdParty.ERROR_INITIALIZING_CARD.getCode().equals(errorCodeText)) {
                errorCode = AppErrorCode.ThirdParty.ERROR_INITIALIZING_CARD;
                errorMessage = R.string.error_reading_dnie;
            } else if (AppErrorCode.ThirdParty.ERROR_LOADING_CERTIFICATES.getCode().equals(errorCodeText)) {
                errorCode = AppErrorCode.ThirdParty.ERROR_LOADING_CERTIFICATES;
                errorMessage = R.string.error_loading_certs;
            } else {
                errorCode = AppErrorCode.ThirdParty.ERROR_INITIALIZING_CARD;
                errorMessage = R.string.error_reading_dnie;
            }

            if (errorCodeText != null) {
                Logger.i("es.gob.afirma", "Mostramos el dialogo de error para el codigo " + errorCodeText);
                CustomDialog cd = new CustomDialog(getActivity(), R.drawable.warn_icon, getString(errorMessage),
                        ErrorMapper.getErrorMsgFormatted(this.getContext(), errorCode),
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
        }

        return contentLayout;
    }

}
