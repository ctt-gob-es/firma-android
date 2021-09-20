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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.security.KeyChain;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;
import es.gob.afirma.android.gui.ConfigNfcDialog;
import es.gob.afirma.android.gui.MessageDialog;
import es.gob.afirma.android.gui.SettingDialog;

/** Actividad que se muestra cuando se arranca la aplicaci&oacute;n pulsando su icono.
 * @author Alberto Mart&iacute;nez */
public final class MainActivity extends FragmentActivity implements DialogInterface.OnClickListener {

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

		if (!nfcAvailableChecked) {
			nfcAvailable = NfcHelper.isNfcServiceAvailable(this);
			nfcAvailableChecked = true;
		}

		if (nfcAvailable && AppConfig.isFirstExecution(this)) {
			new ConfigNfcDialog().show(getSupportFragmentManager(), "enableNfcDialog");
			AppConfig.setFirstExecution(false);
		}

        writePerm = (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
        );

        TextView privacyPolicy = findViewById(R.id.privacyPolicyTextView);
        privacyPolicy.setPaintFlags(privacyPolicy.getPaintFlags()| Paint.UNDERLINE_TEXT_FLAG);
	}

    public void privacyPolicyLinkClick(final View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)));
        startActivity(browserIntent);
    }

	/** @param v Vista sobre la que se hace clic. */
	public void onClick(final View v) {

        Logger.d("es.gob.afirma", "Identificador de elemento pulsado: " + v.getId());

		//Boton firmar fichero local
		if(v.getId() == R.id.buttonSign){
			if (!writePerm) {
                Logger.i("es.gob.afirma", "No se tiene permiso de escritura en memoria");
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
                Logger.i("es.gob.afirma", "No se tiene permiso de escritura en memoria");
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

		Intent intent;
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setTypeAndNormalize("application/x-pkcs12"); //$NON-NLS-1$
		}
		else {
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setClass(this, FileChooserActivity.class);
			intent.putExtra(EXTRA_RESOURCE_TITLE, getString(R.string.title_activity_cert_chooser));
			intent.putExtra(EXTRA_RESOURCE_EXT, CERTIFICATE_EXTS);
		}
        startActivityForResult(intent, SELECT_CERT_REQUEST_CODE, null);
    }

    private void startLocalSign() {
        startActivity(new Intent(getApplicationContext(), LocalSignResultActivity.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Logger.i("es.gob.afirma", "Concedido permiso de escritura en disco");
                    switch (currentOperation) {
                        case LOCALSIGN:
                            startLocalSign();
                            break;
                        case CERTIMPORT:
                            startCertImport();
                            break;
                        default:
                            Logger.w("es.gob.afirma", "Operacion desconocida ha provocado la solicitud de permisos");
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
			byte[] fileContent;
			String filename = null;
			try {
				if (data.getStringExtra(FileChooserActivity.RESULT_DATA_STRING_FILENAME) != null) {
					filename = data.getStringExtra(FileChooserActivity.RESULT_DATA_STRING_FILENAME);
					fileContent = readDataFromFile(new File(filename));
				}
				else {
					final Uri dataUri = data.getData();
					filename = dataUri.getLastPathSegment();
					fileContent = readDataFromUri(dataUri);
				}
			} catch (final IOException e) {
				showErrorMessage(getString(R.string.error_loading_selected_file, filename));
				Logger.e(ES_GOB_AFIRMA, "Error al cargar el fichero", e); //$NON-NLS-1$
				return;
			}
			final Intent intent = KeyChain.createInstallIntent();
			intent.putExtra(KeyChain.EXTRA_PKCS12, fileContent);
			startActivity(intent);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private byte[] readDataFromFile(File dataFile) throws IOException {
		int n;
		final byte[] buffer = new byte[1024];
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (final InputStream is = new FileInputStream(dataFile);) {
			while ((n = is.read(buffer)) > 0) {
				baos.write(buffer, 0, n);
			}
		}
		return baos.toByteArray();
	}

	private byte[] readDataFromUri(Uri uri) throws IOException {
		int n;
		final byte[] buffer = new byte[1024];
		final ByteArrayOutputStream baos;
		try (InputStream is = getContentResolver().openInputStream(uri);) {
			baos = new ByteArrayOutputStream();
			while ((n = is.read(buffer)) > 0) {
				baos.write(buffer, 0, n);
			}
		}
		return baos.toByteArray();
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

	@Override
	protected void onResume() {
		super.onResume();
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
				new SettingDialog().show(getSupportFragmentManager(), "SettingDialog"); //$NON-NLS-1$
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}