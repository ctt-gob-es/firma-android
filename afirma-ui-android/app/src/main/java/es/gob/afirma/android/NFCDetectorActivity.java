package es.gob.afirma.android;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;

import es.gob.afirma.android.crypto.DnieConnectionManager;
import es.gob.afirma.android.gui.CanResult;
import es.gob.afirma.R;
import es.gob.jmulticard.android.callbacks.CachePasswordCallback;

/** Indica al usuario que acerque el DNIe por NFC para obtener los certificados.
 * @author Sergio Mart&iacute;nez */
public class NFCDetectorActivity extends FragmentActivity {

    public static final String INTENT_EXTRA_CAN_VALUE = "canValue"; //$NON-NLS-1$

    static final String INTENT_EXTRA_PASSWORD_CALLBACK = "pc"; //$NON-NLS-1$

    private NfcAdapter mNfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] mTechLists;

    private CanResult canResult;
    public Tag discoveredTag = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_nfc);

        // Si buscamos la tarjeta, borramos los datos que ya tuviesemos
        DnieConnectionManager.getInstance().reset();

        this.discoveredTag = null;
        this.canResult = new CanResult();

        if (getIntent() != null && getIntent().hasExtra(INTENT_EXTRA_CAN_VALUE)) {
            this.canResult.setPasswordCallback(
                    new CachePasswordCallback(getIntent().getCharArrayExtra(INTENT_EXTRA_CAN_VALUE)));
        }

        Button cancelSearch = findViewById(R.id.cancelSearchBtn);
        cancelSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NFCDetectorActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        IntentFilter discovery = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

        // Filtros del Intent para escribir un Tag
        intentFiltersArray = new IntentFilter[]{discovery, ndefDetected, techDetected};

        mTechLists = new String[][] { new String[] {
                NfcV.class.getName(),
                NfcF.class.getName(),
                NfcA.class.getName(),
                NfcB.class.getName()
        } };

        final Intent singleTopIntent = new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(
                this, 0, singleTopIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
        startActivity(singleTopIntent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        discoveredTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (this.canResult.getPasswordCallback() != null && discoveredTag != null) {
            prepareCardConnection();
        }
    }

    private void prepareCardConnection() {
        DnieConnectionManager.getInstance().setDiscoveredTag(this.discoveredTag);

        Intent dataIntent = new Intent();
        dataIntent.putExtra(INTENT_EXTRA_PASSWORD_CALLBACK, this.canResult.getPasswordCallback());
        setResult(RESULT_OK, dataIntent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, mTechLists);
    }

    @Override
    public void onPause() {
        mNfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }
}
