package es.gob.afirma.android;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import es.gob.afirma.R;

public class FaqActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView titleTv;
    private View navButton;
    private RecyclerView faqRecycler;
    private FaqRecyclerAdapter adapter;
    private List<FaqItem> faqItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        toolbar = findViewById(R.id.faqToolbar);
        titleTv = findViewById(R.id.faqToolbarTitle);
        faqRecycler = findViewById(R.id.faqRecycler);
        navButton = toolbar.getChildCount() > 0 ? toolbar.getChildAt(0) : null;

        initSampleData();
        setupRecycler();
        setupToolbar();
        setupFocusCycle();
    }

    private void initSampleData() {
        faqItems = new ArrayList<>();

        List<CharSequence> ans1 = new ArrayList<>();
        ans1.add(HtmlCompat.fromHtml(
                getString(R.string.response_1),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString());
        faqItems.add(new FaqItem(getString(R.string.question_1), ans1));

        List<CharSequence> ans2 = new ArrayList<>();
        ans2.add(getString(R.string.response_2));
        faqItems.add(new FaqItem(getString(R.string.question_2), ans2));

        List<CharSequence> ans3 = new ArrayList<>();
        ans3.add(getString(R.string.response_3));
        faqItems.add(new FaqItem(getString(R.string.question_3), ans3));
    }

    private void setupRecycler() {
        adapter = new FaqRecyclerAdapter(this, faqItems, navButton);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        faqRecycler.setLayoutManager(layoutManager);
        faqRecycler.setAdapter(adapter);
        faqRecycler.setFocusable(true);
        faqRecycler.setFocusableInTouchMode(true);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setNavigationContentDescription(getString(R.string.go_back));

        if (navButton != null) {
            navButton.setFocusable(true);
            navButton.setFocusableInTouchMode(true);

            navButton.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

                if (keyCode == KeyEvent.KEYCODE_TAB || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    titleTv.requestFocus();
                    titleTv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    titleTv.announceForAccessibility(titleTv.getText().toString());
                    return true;
                }
                return false;
            });
        }
    }

    private void setupFocusCycle() {
        titleTv.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

            if (keyCode == KeyEvent.KEYCODE_TAB || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                faqRecycler.post(() -> {
                    faqRecycler.requestFocus();
                    faqRecycler.scrollToPosition(0);
                    View first = faqRecycler.getLayoutManager().findViewByPosition(0);
                    if (first != null) first.requestFocus();
                });
                return true;
            }
            return false;
        });

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus) return;

        if (navButton != null) {
            navButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        // Forzamos foco y lectura del título
        titleTv.postDelayed(() -> {
            titleTv.requestFocus();
            titleTv.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            titleTv.announceForAccessibility(titleTv.getText());

            titleTv.setNextFocusDownId(R.id.faqRecycler);

            if (navButton != null) {
                navButton.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }

        }, 250);
    }


}
