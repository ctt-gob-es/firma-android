package es.gob.afirma.android;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.SettingDialog;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        DrawerLayout.DrawerListener {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setLogo(R.drawable.logo_autofirma);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.menu,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        drawerLayout.addDrawerListener(this);

        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.nav_enter, R.anim.nav_exit)
                .replace(R.id.home_content, new MainFragment())
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.settings:
                new SettingDialog().show(getSupportFragmentManager(), "SettingDialog"); //$NON-NLS-1$
                break;
            case R.id.privacy_policy:
                Intent browserPrivacyPolicyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)));
                startActivity(browserPrivacyPolicyIntent);
                break;
            case R.id.accesibility_policy:
                Intent browserAccesibilityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.accessibility_statement_url)));
                startActivity(browserAccesibilityIntent);
                break;
            default:
                throw new IllegalArgumentException("Opcion del menu no implementada.");
        }

        setTitle(getString(R.string.app_name));

        drawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onDrawerSlide(@NonNull View view, float v) {
        //Cambio en la posición del drawer.
    }

    @Override
    public void onDrawerOpened(@NonNull View view) {
        //Drawer abierto.
    }

    @Override
    public void onDrawerClosed(@NonNull View view) {
        //Drawer cerrado.
    }

    @Override
    public void onDrawerStateChanged(int i) {
        //Cambio de estado del drawer.
    }

}
