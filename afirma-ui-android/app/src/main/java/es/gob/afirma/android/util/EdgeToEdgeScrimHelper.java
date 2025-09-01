package es.gob.afirma.android.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class EdgeToEdgeScrimHelper {

    private EdgeToEdgeScrimHelper() {}

    public static void install(Activity activity,
                               @ColorInt int statusScrimColor,
                               @ColorInt int navScrimColor,
                               boolean lightStatusBarIcons,
                               boolean lightNavBarIcons) {

        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(lightStatusBarIcons);
        controller.setAppearanceLightNavigationBars(lightNavBarIcons);

        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        View root = content.getChildCount() > 0 ? content.getChildAt(0) : content;

        final String TAG_STATUS = "edge_to_edge_status_scrim";
        final String TAG_NAV = "edge_to_edge_nav_scrim";

        View statusScrim = content.findViewWithTag(TAG_STATUS);
        View navScrim = content.findViewWithTag(TAG_NAV);

        if (statusScrim == null) {
            statusScrim = new View(activity);
            statusScrim.setTag(TAG_STATUS);
            statusScrim.setBackgroundColor(statusScrimColor);
            content.addView(statusScrim, new FrameLayout.LayoutParams(0, 0));
        } else {
            statusScrim.setBackgroundColor(statusScrimColor);
        }

        if (navScrim == null) {
            navScrim = new View(activity);
            navScrim.setTag(TAG_NAV);
            navScrim.setBackgroundColor(navScrimColor);
            content.addView(navScrim, new FrameLayout.LayoutParams(0, 0));
        } else {
            navScrim.setBackgroundColor(navScrimColor);
        }

        View finalStatusScrim = statusScrim;
        View finalNavScrim = navScrim;

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            v.setPadding(
                    Math.max(statusInsets.left, navInsets.left),
                    statusInsets.top,
                    navInsets.right,
                    navInsets.bottom
            );

            FrameLayout.LayoutParams lpS = (FrameLayout.LayoutParams) finalStatusScrim.getLayoutParams();
            lpS.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lpS.height = statusInsets.top;
            lpS.gravity = android.view.Gravity.TOP;
            finalStatusScrim.setLayoutParams(lpS);

            FrameLayout.LayoutParams lpN = (FrameLayout.LayoutParams) finalNavScrim.getLayoutParams();
            if (navInsets.bottom > 0) {
                lpN.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lpN.height = navInsets.bottom;
                lpN.gravity = android.view.Gravity.BOTTOM;
            } else if (navInsets.right > 0) {
                lpN.width = navInsets.right;
                lpN.height = ViewGroup.LayoutParams.MATCH_PARENT;
                lpN.gravity = android.view.Gravity.END;
            } else if (navInsets.left > 0) {
                lpN.width = navInsets.left;
                lpN.height = ViewGroup.LayoutParams.MATCH_PARENT;
                lpN.gravity = android.view.Gravity.START;
            } else {
                lpN.width = 0;
                lpN.height = 0;
            }
            finalNavScrim.setLayoutParams(lpN);

            return insets;
        });
    }
}
