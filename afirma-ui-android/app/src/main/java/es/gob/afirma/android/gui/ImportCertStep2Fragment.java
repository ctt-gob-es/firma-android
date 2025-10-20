package es.gob.afirma.android.gui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import es.gob.afirma.R;

public class ImportCertStep2Fragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        int orientation = getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return inflater.inflate(
                    R.layout.import_cert_instructions_step2_land, container, false);
        } else  {
            return inflater.inflate(
                    R.layout.import_cert_instructions_step2, container, false);
        }
    }
}
