package es.gob.afirma.android.errors;

import android.content.Context;

import es.gob.afirma.R;
import es.gob.afirma.core.ErrorCode;

public class ErrorMapper {

    public static String getErrorMessageByCode(Context context, String code) {
        int resId = context.getResources().getIdentifier( "error_" + code, "string", context.getPackageName());
        return resId != 0 ? context.getString(resId) : context.getString(R.string.not_completed_request);
    }

    public static String getErrorMsgFormatted(Context context, ErrorCode errorCode) {
        return "AA" + errorCode.getCode() + " - " + getErrorMessageByCode(context, errorCode.getCode());
    }

    public static String getErrorMsgFormatted(Context context, String errorCodeText) {
        return "AA" + errorCodeText + " - " + getErrorMessageByCode(context, errorCodeText);
    }

}
