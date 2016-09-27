/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.android;


import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.security.KeyChain;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;
import es.gob.afirma.android.gui.AppProperties;
import es.gob.afirma.android.gui.ConfigNfcDialog;
import es.gob.afirma.android.gui.MessageDialog;
import es.gob.afirma.android.gui.SettingDialog;
import es.gob.afirma.android.gui.VerifyCaAppsTask;
import es.gob.afirma.android.gui.VerifyCaAppsTask.CaAppsVerifiedListener;

/** Actividad que se muestra cuando se arranca la aplicaci&oacute;n pulsando su icono.
 * @author Alberto Mart&iacute;nez */
public final class MainActivity extends FragmentActivity implements CaAppsVerifiedListener, DialogInterface.OnClickListener {

    /** Operaci&oacute;n que se estaba realizando antes de solicitar permisos,
     * para continuarla una vez se han concedido. */
    private enum OP_BEFORE_PERM_REQUEST {
        LOCALSIGN,
        CERTIMPORT
    }

    private OP_BEFORE_PERM_REQUEST currentOperation = OP_BEFORE_PERM_REQUEST.LOCALSIGN;

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$

	private final static String EXTRA_RESOURCE_TITLE = "es.gob.afirma.android.title"; //$NON-NLS-1$
	private final static String EXTRA_RESOURCE_EXT = "es.gob.afirma.android.exts"; //$NON-NLS-1$

	private final static String CERTIFICATE_EXTS = ".p12,.pfx"; //$NON-NLS-1$

	private final static int SELECT_CERT_REQUEST_CODE = 1;

	private List<AppProperties> apps;

	private Tracker mTracker;

    /** Indica si tenemos o no permiso de escritura en almacenamiento. */
    private boolean writePerm = false;

    private static final int REQUEST_WRITE_STORAGE = 112;

    /** Indica si se ha hecho la comprobacion de si el dispositivo tiene NFC. Se usa para evitar
	 * detectarlo en cada carga de la actividad. */
	private boolean nfcAvailableChecked = false;

	/** Indica si se ha detectado que el dispositivo tiene NFC. */
	private boolean nfcAvailable = false;

	@Override
	public void onCreate(final Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		GoogleAnalyticsApplication application = (GoogleAnalyticsApplication) getApplication();
		mTracker = application.getDefaultTracker();

		// Por defecto no se muestra el boton de solicitud de certificados
		this.apps = null;

		// Comprobamos si esta instalada la aplicacion de algun proveedor de servicios de certificacion
		// para mostrar el boton de peticion de certificados en caso afirmativo
		verifyCaApps();

		if (!nfcAvailableChecked) {
			nfcAvailable = NfcHelper.isNfcServiceAvailable(this);
			nfcAvailableChecked = true;
		}

		if (nfcAvailable && AppConfig.isFirstExecution(this)) {
			if (!NfcHelper.isNfcPreferredConnection(this)) {
				new ConfigNfcDialog().show(getSupportFragmentManager(), "enableNfcDialog");
			}
			else {
				AppConfig.setFirstExecution(this, false);
			}
		}

        writePerm = (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
        );
	}

	/** @param v Vista sobre la que se hace clic. */
	public void onClick(final View v) {

        Log.d("es.gob.afirma", "Identificador de elemento pulsado: " + v.getId());

		// Dialogo de solicitud de certificados/
		if (v.getId() == R.id.requestCertButton) {

			if (this.apps == null || this.apps.size() < 1) {
				final AlertDialog ad = new AlertDialog.Builder(MainActivity.this).create();
				ad.setTitle(getString(R.string.appsDialogTitle));
				ad.setMessage(getString(R.string.ca_not_found));
				ad.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.close), this);
				ad.show();
			}
			else {
				final AlertDialog ad = new AlertDialog.Builder(MainActivity.this).create();
				ad.setTitle(getString(R.string.appsDialogTitle));
				ad.setMessage(getString(R.string.appsDialogMessage));

				final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				final View view = inflater.inflate(R.layout.dialog_list, null);
				final ListView listView = (ListView) view.findViewById(R.id.listViewListadoApp);

				final AppAdapter listaAppAdapter = new AppAdapter(MainActivity.this, ad, R.layout.array_adapter_apps, this.apps);
				listView.setAdapter(listaAppAdapter);
				ad.setView(view);
				ad.show();
			}
		}

		//Boton firmar fichero local
		else if(v.getId() == R.id.buttonSign){
			if (!writePerm) {
                Log.i("es.gob.afirma", "No se tiene permiso de escritura en memoria");
                currentOperation = OP_BEFORE_PERM_REQUEST.LOCALSIGN;
				requestStoragePerm();
			}
            else {
                startLocalSign();
            }
		}

		// Instalacion de certificados
		else if(v.getId() == R.id.importCertButton) {
            if (!writePerm) {
                Log.i("es.gob.afirma", "No se tiene permiso de escritura en memoria");
                currentOperation = OP_BEFORE_PERM_REQUEST.CERTIMPORT;
                requestStoragePerm();
            }
            else {
                startCertImport();
            }
		}
	}

	private void requestStoragePerm() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                REQUEST_WRITE_STORAGE
        );
	}

    private void startCertImport() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setClass(this, FileChooserActivity.class);
        intent.putExtra(EXTRA_RESOURCE_TITLE, getString(R.string.title_activity_cert_chooser));
        intent.putExtra(EXTRA_RESOURCE_EXT, CERTIFICATE_EXTS);
        startActivityForResult(intent, SELECT_CERT_REQUEST_CODE);
    }

    private void startLocalSign() {
        startActivity(new Intent(getApplicationContext(), LocalSignResultActivity.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("es.gob.afirma", "Concedido permiso de escritura en memoria");
                    switch (currentOperation) {
                        case LOCALSIGN:
                            startLocalSign();
                            break;
                        case CERTIMPORT:
                            startCertImport();
                            break;
                        default:
                            Log.w("es.gob.afirma", "Operacion desconocida ha provocado la solicitud de permisos");
                    }

                }
                else {
                    Toast.makeText(
                        this,
                        getString(R.string.nostorageperm),
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        }
    }

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

		if (requestCode == SELECT_CERT_REQUEST_CODE && resultCode == RESULT_OK) {

			final String filename = data.getStringExtra(FileChooserActivity.RESULT_DATA_STRING_FILENAME);

			int n;
			final byte[] buffer = new byte[1024];
			final ByteArrayOutputStream baos;
			try {
				baos = new ByteArrayOutputStream();
				final InputStream is = new FileInputStream(filename);
				while ((n = is.read(buffer)) > 0) {
					baos.write(buffer, 0, n);
				}
				is.close();
			}
			catch (final IOException e) {
				showErrorMessage(getString(R.string.error_loading_selected_file, filename));
				Log.e(ES_GOB_AFIRMA, "Error al cargar el fichero: " + e.toString()); //$NON-NLS-1$
				e.printStackTrace();
				return;
			}

			final Intent intent = KeyChain.createInstallIntent();
			intent.putExtra(KeyChain.EXTRA_PKCS12, baos.toByteArray());
			startActivity(intent);
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Muestra un mensaje de advertencia al usuario.
	 * @param message Mensaje que se desea mostrar.
	 */
	private void showErrorMessage(final String message) {
		MessageDialog md = MessageDialog.newInstance(message);
		md.setListener(null);
		md.setDialogBuilder(this);
		md.show(getSupportFragmentManager(), "ErrorDialog"); //$NON-NLS-1$;
	}

	private void verifyCaApps() {
		new VerifyCaAppsTask(this, this).execute();
	}

	@Override
	public void caAppsVerified(final List<AppProperties> ap) {
		this.apps = ap;
	}

	@Override
	protected void onResume() {
		super.onResume();
		GoogleAnalytics.getInstance(this).reportActivityStart(this);
		mTracker.setScreenName("MainActivity");
		mTracker.send(new HitBuilders.ScreenViewBuilder().build());
	}

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		dialog.dismiss();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (nfcAvailable) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.menu_settings, menu);
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.settings:
//				Dialog d = new AlertDialog.Builder(this)
//						.setTitle("Prueba")
//						.setMessage("Hola Mundo!!")
//						.create();
//				d.show();
				new SettingDialog().show(getSupportFragmentManager(), "SettingDialog"); //$NON-NLS-1$
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/**Clase para crear cada fila de la tabla.
	 * Cada fila consta de: logo, nombre de la aplicaci&oacute;n, nombre del paquete
	 * @author Astrid Idoate Gil*/
	final class AppAdapter extends ArrayAdapter<AppProperties> {

		private final List<AppProperties> items;
		private final AlertDialog ad;
		private TextView mTextViewAppName;
		private TextView mTextViewAppDescription;
		private ImageView mImageViewIconApp;

		AppAdapter(final Context context, final AlertDialog ad, final int textViewResourceId,  final List<AppProperties> items) {
			super(context, textViewResourceId, items != null ? items : new ArrayList<AppProperties>());
			this.ad = ad;
			this.items = items == null ? new ArrayList<AppProperties>(0) : items;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {

			final LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View v = vi.inflate(R.layout.array_adapter_apps, null);

			final AppProperties appProperties = this.items.get(position);

			this.mTextViewAppName = (TextView) v.findViewById(R.id.tvName);
			this.mTextViewAppDescription = (TextView) v.findViewById(R.id.tvDescription);
			this.mImageViewIconApp = (ImageView) v.findViewById(R.id.icon);

			this.mTextViewAppName.setText(appProperties.getNameApp());
			this.mTextViewAppDescription.setText(appProperties.getDescription());
			this.mImageViewIconApp.setImageDrawable(appProperties.getIcon());

			final LinearLayout linearLayoutList  = (LinearLayout) v.findViewById(R.id.listaItem);
			linearLayoutList.setOnClickListener(new DialogItemClientListener(this.ad, appProperties));
			return v;
		}

		private class DialogItemClientListener implements OnClickListener {

			private final AlertDialog alertDialog;
			private final AppProperties appProperties;

			DialogItemClientListener(final AlertDialog ad, final AppProperties appProperties) {
				this.alertDialog = ad;
				this.appProperties = appProperties;
			}

			@Override
			public void onClick(final View view) {
				if (this.appProperties.isInstalled()) {
					final ComponentName cn = new ComponentName(this.appProperties.getPackageName(), this.appProperties.getMainActivity());
					final Intent intent = new Intent();
					intent.setComponent(cn);
					startActivity(intent);
				}
				else {
					final Intent intent = new Intent();
					intent.setData(Uri.parse(this.appProperties.getMarketUrl()));
					startActivity(intent);
				}
				this.alertDialog.dismiss();
			}
		}
	}
}