package es.gob.afirma.android.errors;

import android.content.Context;

import es.gob.afirma.core.ErrorCode;

public class ErrorMapper {

    public static String getErrorMessageByCode(Context context, String code) {
        int resId = context.getResources().getIdentifier( "error_" + code, "string", context.getPackageName());
        return resId != 0 ? context.getString(resId) : "Error desconocido";
    }

    public static String getErrorMsgFormatted(Context ctx, ErrorCode errorCode) {
        return "AA" + errorCode.getCode() + " - " + getErrorMessageByCode(ctx, errorCode.getCode());
    }

}
