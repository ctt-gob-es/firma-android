package es.gob.afirma.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import es.gob.afirma.android.Logger;

public class FileUtil {

    private final static String ES_GOB_AFIRMA = "es.gob.afirma";

    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final int PDF_MIN_FILE_SIZE = 70;
    private static final String PDF_FILE_HEADER = "%PDF-";

    public static File from(Context context, Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        String fileName = getFileName(context, uri);
        String[] splitName = splitFileName(fileName);
        File tempFile = File.createTempFile(splitName[0] + "_temp", splitName[1]);
        tempFile = rename(tempFile, fileName);
        tempFile.deleteOnExit();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tempFile);
        } catch (FileNotFoundException e) {
            Logger.e(ES_GOB_AFIRMA,"Archivo no encontrado ", e);
        }
        if (inputStream != null) {
            copy(inputStream, out);
            inputStream.close();
        }

        if (out != null) {
            out.close();
        }
        return tempFile;
    }

    private static String[] splitFileName(String fileName) {
        String name = fileName;
        String extension = "";
        int i = fileName.lastIndexOf(".");
        if (i != -1) {
            name = fileName.substring(0, i);
            extension = fileName.substring(i);
        }

        return new String[]{name, extension};
    }

    @SuppressLint("Range")
    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                Logger.e(ES_GOB_AFIRMA,"Archivo no encontrado ", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf(File.separator);
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private static File rename(File file, String newName) {
        File newFile = new File(file.getParent(), newName);
        if (!newFile.equals(file)) {
            if (newFile.exists() && newFile.delete()) {
                Logger.d(ES_GOB_AFIRMA,"Se elimina el archivo antiguo " + newName);
            }
            if (file.renameTo(newFile)) {
                Logger.d(ES_GOB_AFIRMA, "Se renombra el archivo a " + newName);
            }
        }
        return newFile;
    }

    private static long copy(InputStream input, OutputStream output) throws IOException {
        long count = 0;
        int n;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static byte[] readDataFromFile(File dataFile) throws IOException {
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

    public static String readPolicyFile(Context ctx, String language) {
        BufferedReader reader = null;
        String html = "";
        try {
            reader = new BufferedReader(
                    new InputStreamReader(ctx.getAssets().open("politica_" + language + ".html")));

            String mLine;
            while ((mLine = reader.readLine()) != null) {
                html += mLine;
            }
        } catch (IOException e) {
            Log.w("es.gob.afirma", "No se ha podido cargar la politica de privacidad de la aplicacion correctamente", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.w("es.gob.afirma", "No se ha podido cargar la politica de privacidad  de la aplicacion correctamente", e);
                }
            }
        }
        return html;
    }

    public static String readLegalFile(Context ctx,String language) {
        BufferedReader reader = null;
        String html = "";
        try {
            reader = new BufferedReader(
                    new InputStreamReader(ctx.getAssets().open("aviso_" + language + ".html")));

            String mLine;
            while ((mLine = reader.readLine()) != null) {
                html += mLine;
            }
        } catch (IOException e) {
            Log.w("es.gob.afirma", "No se ha podido cargar el aviso legal de la aplicacion correctamente", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.w("es.gob.afirma", "No se ha podido cargar el aviso legal de la aplicacion correctamente", e);
                }
            }
        }
        return html;
    }

    public static boolean isPdfFile(final byte[] data) {
        if (data == null || data.length < PDF_MIN_FILE_SIZE) {
            Log.w("es.gob.afirma", "Datos a firmar: " + new String(data));
            return false;
        }
        final byte[] buffer = new byte[PDF_FILE_HEADER.length()];
        try {
            new ByteArrayInputStream(data).read(buffer);
        }
        catch (final Exception e) {
            Log.w("es.gob.afirma", "El contenido parece corrupto o truncado: ", e);
            return false;
        }
        // Comprobamos que cuente con una cabecera PDF
        if (!PDF_FILE_HEADER.equals(new String(buffer))) {
            return false;
        }
        return true;
    }
}
