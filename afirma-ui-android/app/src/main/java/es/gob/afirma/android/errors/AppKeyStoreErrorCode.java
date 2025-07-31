package es.gob.afirma.android.errors;

import es.gob.afirma.core.ErrorCode;

public class AppKeyStoreErrorCode {

    public static class Internal {

        public static final ErrorCode CYPHERING_CERT							= new ErrorCode("200101", "Error al cifrar el certificado para enviarlo al servidor intermedio"); //$NON-NLS-1$ //$NON-NLS-2$

        public static final ErrorCode CYPHERING_CERT_TO_SEND					= new ErrorCode("200401", "Error al cifrar el certificado para enviarlo al sevidor intermedio"); //$NON-NLS-1$ //$NON-NLS-2$

        public static final ErrorCode LOAD_CERT_TO_IMPORT						= new ErrorCode("201100", "Error en la carga de certificado para importar"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode ALIAS_NOT_VALID							= new ErrorCode("201102", "Error al cargar el certificado, posiblemente relacionado por usar un alias de certificado no valido"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode USER_NOT_SELECT_CERT						= new ErrorCode("201104", "El usuario no selecciono un certificado"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode UNEXPECTED_RECOVERING_KEY					= new ErrorCode("201105", "Error inesperado al recuperar la clave del certificado de firma"); //$NON-NLS-1$ //$NON-NLS-2$

    }
}
