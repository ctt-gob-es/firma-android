package es.gob.afirma.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.ImportCertStep1Fragment;
import es.gob.afirma.android.gui.ImportCertStep2Fragment;

public class CertImportInstructionsActivity extends AppCompatActivity {

    private static final int NUM_PAGES = 2;

    private TextView titleText;

    private TextView introText;

    private ViewPager2 viewPager;

    private FragmentStateAdapter pagerAdapter;

    private Button continueButton;

    private Button addCertButton;

    private int activeStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_import_cert);

        // Recuperamos el paso actual en caso de que gire la pantalla
        if (savedInstanceState != null) {
            activeStep = savedInstanceState.getInt("activeStep", 0);
        }

        Toolbar toolbar = findViewById(R.id.importCertToolbar);
        toolbar.setNavigationContentDescription(getString(R.string.go_back));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (viewPager != null) {
                    viewPager.setCurrentItem(1);
                } else {
                    activateStep2();
                }
            }
        });

        addCertButton = findViewById(R.id.importCertButton);

        addCertButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                activeStep = 0;
                Intent replyIntent = new Intent(getBaseContext(), HomeActivity.class);
                replyIntent.putExtra("startImportCert", true);
                setResult(RESULT_OK, replyIntent);
                startActivity(replyIntent);
                finish();
            }
        });

        titleText = findViewById(R.id.titleText);

        introText = findViewById(R.id.introText);

        viewPager = findViewById(R.id.containerVp);

        if (viewPager != null) {
            pagerAdapter = new ScreenSlidePagerAdapter(this);
            viewPager.setAdapter(pagerAdapter);
        }

        if (activeStep == 1) {
            activateStep2();
        } else {
            activateStep1();
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Guardamos el paso actual antes de que la Activity se destruya
        super.onSaveInstanceState(outState);
        outState.putInt("activeStep", activeStep);
    }

    @Override
    public void onBackPressed() {
        if (viewPager != null) {
            if (viewPager.getCurrentItem() == 0) {
                super.onBackPressed();
            } else {
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
            }
        } else {
            switch (activeStep) {
                case 0:
                    super.onBackPressed();
                    break;
                case 1:
                    activateStep1();
                    break;
            }
        }
    }

    private void activateStep1() {
        titleText.setText(R.string.locate_file_title);
        introText.setText(R.string.locate_file_message);
        continueButton.setVisibility(View.VISIBLE);
        addCertButton.setVisibility(View.GONE);
        ImageView page1Indicator = findViewById(R.id.page1Indicator);
        page1Indicator.setImageResource(R.drawable.selectedpageindicator);
        ImageView page2Indicator = findViewById(R.id.page2Indicator);
        page2Indicator.setImageResource(R.drawable.unselectedpageindicator);
        activeStep = 0;
    }

    private void activateStep2() {
        titleText.setText(R.string.send_to_device_title);
        introText.setText(R.string.send_to_device_message);
        continueButton.setVisibility(View.GONE);
        addCertButton.setVisibility(View.VISIBLE);
        ImageView page1Indicator = findViewById(R.id.page1Indicator);
        page1Indicator.setImageResource(R.drawable.unselectedpageindicator);
        ImageView page2Indicator = findViewById(R.id.page2Indicator);
        page2Indicator.setImageResource(R.drawable.selectedpageindicator);
        activeStep = 1;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {

        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                activateStep1();
                return new ImportCertStep1Fragment();
            } else {
                activateStep2();
                return new ImportCertStep2Fragment();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) {
                activateStep1();
            } else {
                activateStep2();
            }
            return position;
        }

    }
}