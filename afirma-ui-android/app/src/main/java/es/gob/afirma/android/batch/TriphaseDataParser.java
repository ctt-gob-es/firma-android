package es.gob.afirma.android.batch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import es.gob.afirma.core.signers.TriphaseData;

public class TriphaseDataParser {

    /**
     * Carga datos de firma trif&aacute;sica.
     * @param jsonObject Objeto JSON con los datos a transformar.
     * @return Informaci&oacute;n de firma trif&aacute;sica.
     */
    public static TriphaseData parseFromJSON(final JSONObject jsonObject) throws JSONException {
        JSONArray signsArray = null;
        if (jsonObject.has("signs")) { //$NON-NLS-1$
            signsArray = jsonObject.getJSONArray("signs"); //$NON-NLS-1$
        }

        String format = null;
        if (jsonObject.has("format")) { //$NON-NLS-1$
            format = jsonObject.getString("format"); //$NON-NLS-1$
        }

        final List<TriphaseData.TriSign> triSigns = new ArrayList<>();

        if (signsArray != null) {
            for (int i = 0 ; i < signsArray.length() ; i++) {
                final JSONObject sign = signsArray.getJSONObject(i);
                final JSONArray signInfos = sign.getJSONArray("signinfo"); //$NON-NLS-1$

                for (int j = 0; j < signInfos.length(); j++) {
                    final JSONObject signInfo = signInfos.getJSONObject(j);
                    final String id = signInfo.getString("id"); //$NON-NLS-1$
                    final String signId = signInfo.optString("signid", null); //$NON-NLS-1$
                    final JSONObject params = signInfo.getJSONObject("params"); //$NON-NLS-1$

                    triSigns.add(new TriphaseData.TriSign(parseParamsJSON(params), id, signId));
                }
            }
        } else {
            final JSONArray signInfoArray = jsonObject.getJSONArray("signinfo"); //$NON-NLS-1$
            for (int i = 0 ; i < signInfoArray.length() ; i++) {
                final JSONObject signInfo = signInfoArray.getJSONObject(i);
                final String id = signInfo.getString("id"); //$NON-NLS-1$
                final String signId = signInfo.optString("signid", null); //$NON-NLS-1$
                final JSONObject params = signInfo.getJSONObject("params"); //$NON-NLS-1$
                triSigns.add(new TriphaseData.TriSign(parseParamsJSON(params), id, signId));
            }
        }
        return new TriphaseData(triSigns, format);
    }

    /**
     * Mapea los par&aacute;metros de las firmas.
     * @param params par&aacute;metros a parsear.
     * @return par&aacute;metros mapeados.
     */
    private static Map<String, String> parseParamsJSON(final JSONObject params) throws JSONException {

        final Map<String, String> paramsResult = new ConcurrentHashMap<>();

        Iterator<String> it = params.keys();
        while (it.hasNext()) {
            final String key = it.next();
            final String value = params.getString(key);
            paramsResult.put(key, value);
        }

        return paramsResult;
    }

    /**
     * Genera un JSON con la descripci&oacute;n del mensaje trif&aacute;sico.
     * @param td objeto con los datos a generar.
     * @return JSON con la descripci&oacute;n.
     * @throws JSONException Cuando ocurre un error al formar el JSON.
     */
    public static JSONObject triphaseDataToJson(final TriphaseData td) throws JSONException {

        final JSONArray signInfos = new JSONArray();

        for (TriphaseData.TriSign signConfig : td.getTriSigns()) {

            final JSONObject signInfo = new JSONObject();

            // Agregamos el identificador de la firma concreta (en las contrafirmas una firma puede tener varias firmas)
            if (signConfig.getId() != null) {
                signInfo.put("id", signConfig.getId()); //$NON-NLS-1$
            }
            // Agregamos el identificador de la firma concreta (en las contrafirmas una firma puede tener varias firmas)
            if (signConfig.getSignatureId() != null) {
                signInfo.put("signid", signConfig.getSignatureId()); //$NON-NLS-1$
            }

            // Agregamos los parametros de la firma trifasica
            final JSONObject params = new JSONObject();
            for (String p : signConfig.getDict().keySet()) {
                params.put(p, signConfig.getProperty(p));
            }
            signInfo.put("params", params); //$NON-NLS-1$

            signInfos.put(signInfo);
        }

        final JSONObject tdObject = new JSONObject();
        if (td.getFormat() != null) {
            tdObject.put("format", td.getFormat()); //$NON-NLS-1$
        }
        tdObject.put("signinfo", signInfos); //$NON-NLS-1$

        return tdObject;
    }
}
