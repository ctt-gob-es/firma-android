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

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Properties;

import es.gob.afirma.R;
import es.gob.afirma.android.crypto.SignResult;
import es.gob.afirma.android.errors.AppErrorCode;
import es.gob.afirma.android.errors.ErrorMapper;
import es.gob.afirma.android.gui.AppConfig;
import es.gob.afirma.android.util.FileUtil;
import es.gob.afirma.core.AOControlledException;
import es.gob.afirma.core.ErrorCode;
import es.gob.afirma.core.signers.AOSignConstants;
import es.gob.afirma.core.signers.AOSignerFactory;
import es.gob.afirma.signers.cades.CAdESExtraParams;
import es.gob.afirma.signers.pades.common.PdfExtraParams;

/** Esta actividad permite firmar un fichero local. La firma se guarda en un fichero <i>.csig</i>.
 * Esta clase tiene mucho c&oacute;digo duplicado de la clase <code>LocalSignResultActivity</code>.
 * Hay crear una nueva clase con los m&eacute;todos duplicados.
 * @author Astrid Idoate Gil. */
public final class LocalSignActivity extends SignFragmentActivity {

	private final static String EXTRA_RESOURCE_TITLE = "es.gob.afirma.android.title"; //$NON-NLS-1$
	private final static String EXTRA_RESOURCE_EXCLUDE_DIRS = "es.gob.afirma.android.excludedDirs"; //$NON-NLS-1$

	/** C&oacute;digo de solicitud de carga de fichero. */
	private final static int REQUEST_CODE_SELECT_FILE = 103;
	/** C&oacute;digo de solicitud de guardado de fichero. */
	private final static int REQUEST_CODE_SAVE_FILE = 104;
	/** C&oacute;digo de solicitud de firma visible. */
	private final static int REQUEST_VISIBLE_SIGN_PARAMS = 105;
	/** Error cargando PDF para firma visible. */
	public final static int ERROR_REQUEST_VISIBLE_SIGN = 106;

	static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA256"; //$NON-NLS-1$

	private static final String PDF_FILE_SUFFIX = ".pdf"; //$NON-NLS-1$

	private final static String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$
	private final static String SIGNING_ERROR = "errorSigning"; //$NON-NLS-1$

	private final static String SHOW_SIGNING_RESULT = "showSigningResult"; //$NON-NLS-1$

	private final static String ERROR_TITLE_PARAM = "errorTitle"; //$NON-NLS-1$

	private final static String ERROR_MESSAGE_PARAM = "errorMessage"; //$NON-NLS-1$

	String fileName; //Nombre del fichero seleccionado

	private SignResult signedData;

	private String signatureFilename = null;

	private String signedDataContentType = "*/*";

	private String format = null;

	byte[] fileContent;

	File dataFile;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!isSigning()) {

			// Elegimos un fichero del directorio
			Intent intent;
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setTypeAndNormalize("*/*"); //$NON-NLS-1$
			}
			else {
				intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setClass(this, FileChooserActivity.class);
				intent.putExtra(EXTRA_RESOURCE_TITLE, getString(R.string.title_activity_choose_sign_file));
				intent.putExtra(EXTRA_RESOURCE_EXCLUDE_DIRS, FileSystemConstants.COMMON_EXCLUDED_DIRS); //$NON-NLS-1$
			}
			startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

		// El usuario ha seleccionado un fichero
		if (requestCode == REQUEST_CODE_SELECT_FILE) {

			if (resultCode == RESULT_OK) {

				String filePath = null;
				try {
					if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
						final Uri dataUri = data.getData();
						this.fileName = getFileName(dataUri);
						this.fileContent = readDataFromUri(dataUri);
						this.dataFile = FileUtil.from(this, dataUri);
						if (this.dataFile.exists()) {
							filePath = this.dataFile.getAbsolutePath();
						}
					} else {
						this.fileName = data.getStringExtra(FileChooserActivity.RESULT_DATA_STRING_FILENAME);
						this.dataFile = new File(this.fileName);
						filePath = this.dataFile.getAbsolutePath();
						this.fileContent = FileUtil.readDataFromFile(dataFile);
					}
				} catch (final OutOfMemoryError e) {
					showErrorMessage(getString(R.string.error_ocurred), AppErrorCode.Internal.FILE_READ_OUT_OF_MEMORY);
					Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.FILE_READ_OUT_OF_MEMORY.toString(), e); //$NON-NLS-1$
					return;
				} catch (final IOException e) {
					showErrorMessage(getString(R.string.error_ocurred), ErrorCode.Internal.LOADING_LOCAL_FILE_ERROR);
					Logger.e(ES_GOB_AFIRMA, ErrorCode.Internal.LOADING_LOCAL_FILE_ERROR.toString(), e); //$NON-NLS-1$
					return;
				}

                LocalSignActivity.this.format = this.fileName.toLowerCase(Locale.ENGLISH)
						.endsWith(PDF_FILE_SUFFIX) ?
						AOSignConstants.SIGN_FORMAT_PADES :
						AOSignConstants.SIGN_FORMAT_CADES;

				if (AOSignConstants.SIGN_FORMAT_PADES.equals(this.format) && AppConfig.isPadesVisibleSignature(this)) {
					// Previsualizacion de PDF
					Intent intent = new Intent(this, PdfSelectPreviewActivity.class);
					intent.putExtra("filePath", filePath);
					startActivityForResult(intent, REQUEST_VISIBLE_SIGN_PARAMS);
				} else {
					Properties extraParams = new Properties();
					extraParams.setProperty(CAdESExtraParams.MODE, "implicit");
					sign("SIGN", fileContent, format, DEFAULT_SIGNATURE_ALGORITHM, true, extraParams);
				}
			}
			else if (resultCode == RESULT_CANCELED) {
				finish();
				return;
			}
		}
		else if (requestCode == REQUEST_VISIBLE_SIGN_PARAMS) {

				if (resultCode == RESULT_OK) {

					Properties extraParams = new Properties();
					extraParams.setProperty(CAdESExtraParams.MODE, "implicit");

					if(data.hasExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTX)
					&& data.hasExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTY)
					&& data.hasExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTX)
					&& data.hasExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTY)) {
						// Ofuscacion de los datos del certificado de firma
						final boolean obfuscate = AppConfig.isPadesObfuscateCertInfo(this);
						extraParams.setProperty(PdfExtraParams.OBFUSCATE_CERT_DATA, Boolean.toString(obfuscate));

						extraParams.setProperty(PdfExtraParams.SIGNATURE_PAGES, data.getStringExtra(PdfExtraParams.SIGNATURE_PAGES));
						extraParams.setProperty(PdfExtraParams.LAYER2_TEXT, getString(R.string.pdf_visible_sign_template));

						extraParams.setProperty(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTX, data.getStringExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTX));
						extraParams.setProperty(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTY, data.getStringExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTY));
						extraParams.setProperty(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTX, data.getStringExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTX));
						extraParams.setProperty(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTY, data.getStringExtra(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTY));

						if (data.hasExtra(PdfExtraParams.OWNER_PASSWORD_STRING)) {
							extraParams.setProperty(PdfExtraParams.OWNER_PASSWORD_STRING, new String(data.getByteArrayExtra(PdfExtraParams.OWNER_PASSWORD_STRING)));
						}
					}

					sign("SIGN", fileContent, format, DEFAULT_SIGNATURE_ALGORITHM, true, extraParams);
				}
				else if (resultCode == RESULT_CANCELED) {
					finish();
					return;
				} else if (resultCode == ERROR_REQUEST_VISIBLE_SIGN) {
					showErrorMessage(getString(R.string.error_ocurred), ErrorCode.Internal.LOADING_LOCAL_FILE_ERROR);
					Logger.e(ES_GOB_AFIRMA, ErrorCode.Internal.LOADING_LOCAL_FILE_ERROR.toString()); //$NON-NLS-1$
					return;
				}
			}
		// Resultado del guardado de fichero a partir de Android 11
		else if (requestCode == REQUEST_CODE_SAVE_FILE) {
			if (resultCode == RESULT_OK) {

				try {
					OutputStream outputStream = getContentResolver().openOutputStream(data.getData());
					if (outputStream != null) {
						outputStream.write(this.signedData.getSignature());
						outputStream.close();
					}
					else {
						showErrorMessage(getString(R.string.error_ocurred), AppErrorCode.Internal.SAVING_DATA_DISK);
						Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.SAVING_DATA_DISK.toString()); //$NON-NLS-1$
						return;
					}
				} catch (final IOException e) {
					showErrorMessage(getString(R.string.error_ocurred), AppErrorCode.Internal.SAVING_DATA_DISK);
					Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.SAVING_DATA_DISK.toString(), e); //$NON-NLS-1$
					return;
				}

				showSuccessMessage();

			} else {
				finish();
				return;
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private byte[] readDataFromUri(Uri uri) throws IOException {
		int n;
		final byte[] buffer = new byte[1024];
		final ByteArrayOutputStream baos;
		try (InputStream is = getContentResolver().openInputStream(uri)) {
			baos = new ByteArrayOutputStream();
			while ((n = is.read(buffer)) > 0) {
				baos.write(buffer, 0, n);
			}
		}
		return baos.toByteArray();
	}

	public String getFileName(Uri uri) {
		String result = null;
		if (uri.getScheme().equals("content")) {
			Cursor cursor = getContentResolver().query(uri, null, null, null, null);
			try {
				if (cursor != null && cursor.moveToFirst()) {
					int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
					if (idx >= 0) {
						result = cursor.getString(idx);
					}
				}
			} finally {
				cursor.close();
			}
		}
		if (result == null) {
			result = uri.getPath();
			int cut = result.lastIndexOf('/');
			if (cut != -1) {
				result = result.substring(cut + 1);
			}
		}
		return result;
	}

	//Guarda los datos en un directorio del dispositivo y muestra por pantalla al usuario la informacion indicando donse se ha almacenado el fichero
	private void saveData(final SignResult signature){

		// Definimos el nombre del fichero de firma
		String inText = null;
		if (AOSignConstants.SIGN_FORMAT_PADES.equals(this.format)) {
			inText = "_signed"; //$NON-NLS-1$
		}
		this.signatureFilename = AOSignerFactory.getSigner(this.format).getSignedName(new File(this.fileName).getName(), inText);
		this.signedDataContentType = "application/" + this.signatureFilename.substring(this.signatureFilename.lastIndexOf('.') + 1);

		// Registramos los datos sobre la firma realizada
		saveSignRecord(SIGN_TYPE_LOCAL, this.dataFile.getName(), null);

		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType(this.signedDataContentType);
			intent.putExtra(Intent.EXTRA_TITLE, this.signatureFilename);

			startActivityForResult(intent, REQUEST_CODE_SAVE_FILE);
		}
		else {

			// Comprobamos que tenemos permisos de lectura sobre el directorio en el que se encuentra el fichero origen
			final File outDirectory;
			if (new File(this.fileName).getParentFile().canWrite()) {
				Logger.d(ES_GOB_AFIRMA, "La firma se guardara en el directorio del fichero de entrada"); //$NON-NLS-1$
				outDirectory = new File(this.fileName).getParentFile();
			} else if (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).exists() && Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).canWrite()) {
				Logger.d(ES_GOB_AFIRMA, "La firma se guardara en el directorio de descargas"); //$NON-NLS-1$
				outDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			} else {
				Logger.w(ES_GOB_AFIRMA, AppErrorCode.Internal.NO_DEVICE_STORE.toString()); //$NON-NLS-1$
				showErrorMessage(getString(R.string.error_ocurred), AppErrorCode.Internal.NO_DEVICE_STORE);
				return;
			}

			int i = 0;
			String finalSignatureFilename = this.signatureFilename;
			while (new File(outDirectory, finalSignatureFilename).exists()) {
				finalSignatureFilename = buildName(this.signatureFilename, ++i);
			}

			try {
				final FileOutputStream fos = new FileOutputStream(new File(outDirectory, finalSignatureFilename));
				fos.write(signature.getSignature());
				fos.flush();
				fos.close();
			} catch (final Exception e) {
				showErrorMessage(getString(R.string.error_ocurred), AppErrorCode.Internal.SAVING_DATA_DISK);
				Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.SAVING_DATA_DISK.toString(), e); //$NON-NLS-1$
				return;
			}

			showSuccessMessage();

			// Refrescamos el directorio para permitir acceder al fichero
			try {
				MediaScannerConnection.scanFile(
						this,
						new String[]{new File(outDirectory, finalSignatureFilename).toString(),
								outDirectory.toString()},
						null,
						null
				);
			} catch (final Exception e) {
				showErrorMessage(getString(R.string.error_ocurred), AppErrorCode.Internal.SAVING_DATA_DISK);
				Logger.e(ES_GOB_AFIRMA, AppErrorCode.Internal.SAVING_DATA_DISK.toString(), e); //$NON-NLS-1$
			}
		}
	}

	/** Muestra los elementos de pantalla informando de un error ocurrido durante la operaci&oacute;n de
	 * firma.
	 * @param title T&iacute;tulo que describe el error producido.
	 * @param errorThrowable Informacion sobre el error producido. */
	private void showErrorMessage(final String title, final Throwable errorThrowable) {
		Intent intent = new Intent(this, HomeActivity.class);
		intent.putExtra(SHOW_SIGNING_RESULT, true);
		intent.putExtra(SIGNING_ERROR, true);
		intent.putExtra(ERROR_TITLE_PARAM, title);
		String msg;
		if (errorThrowable instanceof AOControlledException) {
			msg = ErrorMapper.getErrorMsgFormatted(this, ((AOControlledException) errorThrowable).getErrorCode()); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			msg = getString(R.string.not_completed_request);
		}
		intent.putExtra(ERROR_MESSAGE_PARAM, msg);
		startActivity(intent);
	}

	/** Muestra los elementos de pantalla informando de un error ocurrido durante la operaci&oacute;n de
	 * firma.
	 * @param title T&iacute;tulo que describe el error producido.
	 * @param error Informacion sobre el error producido. */
	private void showErrorMessage(final String title, final ErrorCode error) {
		Intent intent = new Intent(this, HomeActivity.class);
		intent.putExtra(SHOW_SIGNING_RESULT, true);
		intent.putExtra(SIGNING_ERROR, true);
		intent.putExtra(ERROR_TITLE_PARAM, title);
		String msgWithCode = ErrorMapper.getErrorMsgFormatted(this, error);
		intent.putExtra(ERROR_MESSAGE_PARAM, msgWithCode);
		startActivity(intent);
	}

	/** Muestra los elementos de pantalla informando de que la firma se ha generado correctamente y
	 * donde se ha almacenado. */
	private void showSuccessMessage() {
		Intent intent = new Intent(LocalSignActivity.this, HomeActivity.class);
		intent.putExtra(SHOW_SIGNING_RESULT, true);
		intent.putExtra(SIGNING_ERROR, false);
		startActivity(intent);
	}

	@Override
	public void onSigningSuccess(final SignResult signature) {
		this.signedData = signature;
		saveData(signature);
	}

	/** Construye un nombre apropiado para un fichero de firma en base a un nombre base
	 * y un &iacute;ndice.
	 * @param originalName Nombre base del fichero.
	 * @param index &Iacute;ndice.
	 * @return Nombre apropiado para el fichero de firma. */
	private static String buildName(final String originalName, final int index) {

		String indexSuffix = ""; //$NON-NLS-1$
		if (index > 0) {
			indexSuffix = "(" + index + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		final int dotPos = originalName.lastIndexOf('.');
		if (dotPos == -1) {
			return originalName + indexSuffix;
		}
		return originalName.substring(0, dotPos) + indexSuffix + originalName.substring(dotPos);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onSigningError(KeyStoreOperation op, Throwable t) {
		if (t instanceof PendingIntent.CanceledException) {
			Logger.i(ES_GOB_AFIRMA, ErrorCode.Functional.CANCELLED_OPERATION.toString());
			finish();
		}
		else {
			Logger.e(ES_GOB_AFIRMA, "Error durante la firma: " + t);
			if (KeyStoreOperation.SIGN == op) {
				showErrorMessage(getString(R.string.error_ocurred), t);
			}
			else {
				showErrorMessage(getString(R.string.error_ocurred), t);
			}
		}
	}

}

