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
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.security.KeyChain;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import es.gob.afirma.R;
import es.gob.afirma.android.gui.AppConfig;
import es.gob.afirma.android.gui.ConfigNfcDialog;
import es.gob.afirma.android.gui.MessageDialog;

/** Actividad que se muestra cuando se arranca la aplicaci&oacute;n pulsando su icono.
 * @author Alberto Mart&iacute;nez */
public final class MainFragment extends Fragment implements DialogInterface.OnClickListener {

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
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
			Bundle savedInstanceState) {

		View contentLayout;

		super.onCreate(savedInstanceState);
		contentLayout = inflater.inflate(R.layout.fragment_main, container, false);

		ImageView logo = contentLayout.findViewById(R.id.imageView1);
		ViewCompat.setAccessibilityHeading(logo, true);

		if (!nfcAvailableChecked) {
			nfcAvailable = NfcHelper.isNfcServiceAvailable(getActivity());
			nfcAvailableChecked = true;
		}

		if (nfcAvailable && AppConfig.isFirstExecution(getActivity())) {
			new ConfigNfcDialog().show(getChildFragmentManager(), "enableNfcDialog");
			AppConfig.setFirstExecution(false);
		}

		writePerm = (
				ContextCompat.checkSelfPermission(
						getActivity(),
						Manifest.permission.WRITE_EXTERNAL_STORAGE
				) == PackageManager.PERMISSION_GRANTED
		);

		TextView selectOptionText = contentLayout.findViewById(R.id.selectOptionTextView);
		ViewCompat.setAccessibilityHeading(selectOptionText, true);

		TextView wellcomeTextView = contentLayout.findViewById(R.id.textView1);
		if (wellcomeTextView != null) {
			wellcomeTextView.setMovementMethod(new ScrollingMovementMethod());
		}

		Button signButton = contentLayout.findViewById(R.id.buttonSign);
		signButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (!writePerm) {
					Logger.i(ES_GOB_AFIRMA, "No se tiene permiso de escritura en memoria");
					currentOperation = OP_BEFORE_PERM_REQUEST.LOCALSIGN;
					requestStoragePerm();
				}
				else {
					startLocalSign();
				}
			}
		});
		// Control de foco para mejorar la accesibilidad en la navegacion por teclado
		signButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					signButton.setBackgroundColor(Color.parseColor("#000000"));
				} else {
					signButton.setBackgroundColor(Color.parseColor("#981c1c"));
				}
			}
		});

		Button importButton = contentLayout.findViewById(R.id.importCertButton);
		importButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (!writePerm) {
					Logger.i(ES_GOB_AFIRMA, "No se tiene permiso de escritura en memoria");
					currentOperation = OP_BEFORE_PERM_REQUEST.CERTIMPORT;
					requestStoragePerm();
				}
				else {
					startCertImport();
				}
			}
		});
		// Control de foco para mejorar la accesibilidad en la navegacion por teclado
		importButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					importButton.setBackgroundColor(Color.parseColor("#000000"));
				} else {
					importButton.setBackgroundColor(Color.parseColor("#981c1c"));
				}
			}
		});

		return contentLayout;
	}
	private void requestStoragePerm() {
		ActivityCompat.requestPermissions(
				getActivity(),
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
			intent.setClass(getActivity(), FileChooserActivity.class);
			intent.putExtra(EXTRA_RESOURCE_TITLE, getString(R.string.title_activity_cert_chooser));
			intent.putExtra(EXTRA_RESOURCE_EXT, CERTIFICATE_EXTS);
		}
		startActivityForResult(intent, SELECT_CERT_REQUEST_CODE, null);
	}

	private void startLocalSign() {
		startActivity(new Intent(getActivity().getApplicationContext(), LocalSignResultActivity.class));
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

		if (requestCode == SELECT_CERT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
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
	}

	private byte[] readDataFromFile(File dataFile) throws IOException {
		int n;
		final byte[] buffer = new byte[1024];
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (final InputStream is = new FileInputStream(dataFile)) {
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
		try (InputStream is = getActivity().getContentResolver().openInputStream(uri)) {
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
		md.setDialogBuilder(getActivity());
		md.show(getActivity().getSupportFragmentManager(), "ErrorDialog"); //$NON-NLS-1$;
	}

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		dialog.dismiss();
	}

}