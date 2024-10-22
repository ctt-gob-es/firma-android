package es.gob.afirma.android.gui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
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
import es.gob.afirma.android.HomeActivity;

public class CertImportInstructionsActivity extends AppCompatActivity {

    private static final int NUM_PAGES = 2;

    private TextView titleText;

    private TextView introText;

    private ViewPager2 viewPager;

    private FragmentStateAdapter pagerAdapter;

    private Drawable navIcon;

    private Button continueButton;

    private Button addCertButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_import_cert);

        Toolbar toolbar = findViewById(R.id.importCertToolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        navIcon = toolbar.getNavigationIcon();

        continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                viewPager.setCurrentItem(1);
            }
        });

        addCertButton = findViewById(R.id.importCertButton);

        addCertButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent replyIntent = new Intent(getBaseContext(), HomeActivity.class);
                replyIntent.putExtra("startImportCert", true);
                setResult(RESULT_OK, replyIntent);
                startActivity(replyIntent);
                finish();
            }
        });

        titleText = findViewById(R.id.titleText);
        titleText.setText(getResources().getString(R.string.locate_file_title));

        introText = findViewById(R.id.introText);
        introText.setText(getResources().getString(R.string.locate_file_message));

        viewPager = findViewById(R.id.containerVp);

        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
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