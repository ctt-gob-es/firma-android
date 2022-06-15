package es.gob.afirma.android;

import java.io.IOException;
import java.util.Arrays;

import es.gob.afirma.android.crypto.CipherDataManager;
import es.gob.afirma.android.exceptions.DecryptionException;
import es.gob.afirma.android.exceptions.InvalidEncryptedDataLengthException;
import es.gob.afirma.core.misc.protocol.UrlParameters;

public class WebSignUtil {


   public static byte[] getDataFromRetrieveServlet(final UrlParameters params) throws DecryptionException,
                                                                                        InvalidEncryptedDataLengthException,
                                                                                        IOException {
        // Preparamos la URL
        final StringBuilder dataUrl = new StringBuilder(
                params.getRetrieveServletUrl().toString()).
                append("?") //$NON-NLS-1$
                .append("op=get&v=1_0&id=") //$NON-NLS-1$
                .append(params.getFileId());

        Logger.i("Intentamos recuperar los datos del servidor con la URL" , dataUrl.toString()); //$NON-NLS-1$

        // Leemos los datos
        final byte[] recoveredData = IntermediateServerUtil.retrieveData(params.getRetrieveServletUrl().toString(), params.getFileId());

        // Si los datos recibidos representan un error, detenemos la ejecucion
        if (recoveredData.length > 8 && new String(Arrays.copyOf(recoveredData, 8)).toLowerCase().startsWith("err-")) { //$NON-NLS-1$
            Logger.e("Se recupera un error desde el servidor intermedio" , new String(recoveredData)); //$NON-NLS-1$
            throw new InvalidEncryptedDataLengthException("Se recupera un error desde el servidor intermedio: " + new String(recoveredData)); //$NON-NLS-1$
        }

        // Si no ha ocurrido un error, debemos haber recibido los datos cifrados
        final byte[] data;
        try {
            data = CipherDataManager.decipherData(recoveredData, params.getDesKey());
        }
        catch (final Exception e) {
            Logger.e("Error en el descifrado de los datos" , e.toString()); //$NON-NLS-1$
            throw new DecryptionException("Error en el descifrado de los datos: " + e, e); //$NON-NLS-1$
        }
        return data;
    }
}
