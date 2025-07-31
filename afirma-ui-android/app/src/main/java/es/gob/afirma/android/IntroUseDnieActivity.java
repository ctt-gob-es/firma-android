package es.gob.afirma.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.appbar.MaterialToolbar;

import es.gob.afirma.R;
import es.gob.afirma.android.errors.AppErrorCode;
import es.gob.afirma.android.errors.ErrorMapper;
import es.gob.afirma.android.gui.CustomDialog;
import es.gob.afirma.android.gui.CompatibleDniDialog;
import es.gob.afirma.android.util.Utils;

public class IntroUseDnieActivity extends FragmentActivity {

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setPortraitSmartphone(this);
        setContentView(R.layout.activity_intro_sign_dnie);

        MaterialToolbar toolbar = this.findViewById(R.id.introSignDnieToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        context = this;

        Button compatibleDniBtn = this.findViewById(R.id.isCompatibleButton);
        compatibleDniBtn.setPaintFlags(compatibleDniBtn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        compatibleDniBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                CompatibleDniDialog dialog = new CompatibleDniDialog(context);
                dialog.show();
            }
        });

        Button startButton = this.findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(getBaseContext(), StepsInsertDataDnieActivity.class);
                startActivityForResult(intent, StepsInsertDataDnieActivity.REQUEST_NFC_PARAMS);
            }
        });

        checkErrors(getIntent());
    }

    private void checkErrors(Intent intent) {

        boolean unSupportedError = intent.getBooleanExtra(LoadKeyStoreFragmentActivity.ERROR_UNSUPPORTED_NFC, false);
        if (unSupportedError) {
            CustomDialog cd = new CustomDialog(this, R.drawable.warn_icon, getString(R.string.error_ocurred), ErrorMapper.getErrorMsgFormatted(this, AppErrorCode.ThirdParty.UNKNOWN_OR_NOT_SUPPORTED_CARD),
                    getString(R.string.ok));
            CustomDialog finalCd = cd;
            cd.setAcceptButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finalCd.hide();
                }
            });
            cd.show();
            return;
        }

        boolean canError = intent.getBooleanExtra(LoadKeyStoreFragmentActivity.ERROR_CAN_VALIDATION_NFC, false);
        if (canError) {
            CustomDialog cd = new CustomDialog(this, R.drawable.warn_icon, getString(R.string.incorrect_can), ErrorMapper.getErrorMsgFormatted(this, AppErrorCode.ThirdParty.CAN_VALIDATION),
                    getString(R.string.ok));
            CustomDialog finalCd = cd;
            cd.setAcceptButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finalCd.hide();
                }
            });
            cd.show();
            return;
        }

        boolean pinValidationError = intent.getBooleanExtra(LoadKeyStoreFragmentActivity.ERROR_PIN_VALIDATION_NFC, false);
        if (pinValidationError) {
            CustomDialog cd = new CustomDialog(this, R.drawable.warn_icon, getString(R.string.incorrect_pin), ErrorMapper.getErrorMsgFormatted(this, AppErrorCode.ThirdParty.INCORRECT_PIN),
                    getString(R.string.ok));
            CustomDialog finalCd = cd;
            cd.setAcceptButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finalCd.hide();
                }
            });
            cd.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        // Volvemos del proceso de insercion de CAN y PIN del DNIe y se lo devolvemos a la
        // clase de carga de almacenes
		if (requestCode == StepsInsertDataDnieActivity.REQUEST_NFC_PARAMS && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {

        setResult(RESULT_CANCELED);
        finish();

        super.onBackPressed();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}
