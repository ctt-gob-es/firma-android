package es.gob.afirma.android;

import android.app.Activity;
import android.app.Application;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import es.gob.afirma.R;
import es.gob.afirma.android.util.EdgeToEdgeScrimHelper;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            // Para Android 10+ (API 29): se llama despues de onCreate
            @Override public void onActivityPostCreated(Activity activity, Bundle savedInstanceState) {
                tryInstallWhenReady(activity);
            }

            // Fallback para <29
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    tryInstallWhenReady(activity);
                }
            }

            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    private void tryInstallWhenReady(Activity activity) {

        // Se saltan temas de dialogo/flotantes
        if (isDialogOrFloating(activity)) return;

        // Esperar a que el content tenga un hijo
        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null) return;

        if (content.getChildCount() > 0) {
            install(activity);
            return;
        }

        // Escuchar el primer layout donde ya exista el hijo
        ViewTreeObserver vto = content.getViewTreeObserver();
        ViewTreeObserver.OnGlobalLayoutListener[] holder = new ViewTreeObserver.OnGlobalLayoutListener[1];
        holder[0] = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!content.getViewTreeObserver().isAlive()) return;
                if (content.getChildCount() > 0) {
                    content.getViewTreeObserver().removeOnGlobalLayoutListener(holder[0]);
                    install(activity);
                }
            }
        };
        vto.addOnGlobalLayoutListener(holder[0]);
    }

    private void install(Activity activity) {
        // Evita doble instalacion si ya se anadieron scrims (por rotaciones, etc.)
        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        if (content.findViewWithTag("edge_to_edge_status_scrim") != null &&
                content.findViewWithTag("edge_to_edge_nav_scrim") != null) {
            return;
        }

        EdgeToEdgeScrimHelper.install(
                activity,
                ContextCompat.getColor(this, R.color.gray), // status bar
                ContextCompat.getColor(this, R.color.gray), // nav bar
                false,
                false
        );
    }

    private boolean isDialogOrFloating(Activity activity) {
        int[] attrs = new int[] { android.R.attr.windowIsTranslucent, android.R.attr.windowIsFloating };
        TypedArray ta = activity.getTheme().obtainStyledAttributes(attrs);
        boolean translucent = ta.getBoolean(0, false);
        boolean floating = ta.getBoolean(1, false);
        ta.recycle();
        return translucent || floating;
    }

}
