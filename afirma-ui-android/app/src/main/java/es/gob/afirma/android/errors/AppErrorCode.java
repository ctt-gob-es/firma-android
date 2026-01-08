package es.gob.afirma.android.errors;

import es.gob.afirma.core.ErrorCode;

public class AppErrorCode {

    public static class Hardware {
        public static final ErrorCode RESET_NFC								    = new ErrorCode("102000", "Error al resetear la tarjeta NFC"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode SMARTCARD_CONNECTION_LOST           		= new ErrorCode("102008", "Se ha perdido la conexion con la tarjeta"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static class Internal {
        public static final ErrorCode GENERAL_ERROR								= new ErrorCode("200000", "Error general de software"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode CANT_SAVE_SIGN_RECORD						= new ErrorCode("200001", "La firma se ha realizado correctamente pero no se ha podido guardar en el historico de firmas"); //$NON-NLS-1$ //$NON-NLS-2$

        public static final ErrorCode CYPHERING_SIGN							= new ErrorCode("200100", "Error al cifrar la firma para enviarla al servidor intermedio"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode FILE_READ_OUT_OF_MEMORY					= new ErrorCode("200105", "Error de memoria al cargar el fichero"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode USER_SELECT_NOT_PDF						= new ErrorCode("200106", "El usuario selecciono un documento no PDF sobre el que realizar una firma PAdES"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode PADES_NO_PDF_SIGN							= new ErrorCode("200107", "No es posible hacer firma PAdES sobre datos no PDF"); //$NON-NLS-1$ //$NON-NLS-2$

        public static final ErrorCode SAVING_DATA_DISK							= new ErrorCode("200301", "No se ha podido guardar el fichero en disco"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode NO_DEVICE_STORE							= new ErrorCode("200302", "No se ha encontrado donde guardar la firma generada"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode NO_STORAGE_PERMISSIONS					= new ErrorCode("200304", "No dispone de permisos de escritura"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode SAVING_DATA_OP							= new ErrorCode("200305", "Error general durante la operacion de guardado de datos"); //$NON-NLS-1$ //$NON-NLS-2$

        public static final ErrorCode DOMAIN_FORMAT_INCORRECT				    = new ErrorCode("200700", "El dominio tiene un formato incorrecto"); //$NON-NLS-1$ //$NON-NLS-2$

    }

    public static class ThirdParty {

        public static final ErrorCode UNKNOWN_OR_NOT_SUPPORTED_CARD		= new ErrorCode("300100", "La tarjeta identificada en el lector es desconocida o no esta soportada"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode CANT_CONNECT_CARD			        = new ErrorCode("300102", "No se ha podido conectar con la tarjeta"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode ERROR_INITIALIZING_CARD			= new ErrorCode("300103", "La conexion con la tarjeta no esta inicializada"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode INCORRECT_PIN				        = new ErrorCode("300105", "PIN incorrecto"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode BLOCKED_CARD				        = new ErrorCode("300106", "Tarjeta bloqueada"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode CAN_VALIDATION				    = new ErrorCode("300108", "Error durante la validacion del CAN"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode ERROR_LOADING_CERTIFICATES		= new ErrorCode("300111", "Error cargando los certificados del almacen"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static class Communication {

        public static final ErrorCode UNKNOWN_ERROR								= new ErrorCode("400000", "Error desconocido al enviar el error obtenido al servidor"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode COMMUNICATION_WITH_SERVICE				= new ErrorCode("400001", "Error durante la descarga de la configuracion para la seleccion de certificado"); //$NON-NLS-1$ //$NON-NLS-2$

        public static final ErrorCode DOWNLOAD_SIGN								= new ErrorCode("401100", "Error en la descarga de la firma"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode DOWNLOAD_CONFIG_CERT						= new ErrorCode("401101", "Error durante la descarga de la configuracion para la seleccion de certificado"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode UPLOAD_DATA								= new ErrorCode("401200", "Error en el envio de datos"); //$NON-NLS-1$ //$NON-NLS-2$

    }

    public static class Request {

        public static final ErrorCode NOT_SUPPORTED_OPERATION				= new ErrorCode("600002", "La operacion no esta soportada"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode REQUEST_PARAM_NOT_VALID				= new ErrorCode("600006", "Los parametros de la peticion no eran validos"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode ENCODING_CERT				            = new ErrorCode("600007", "Error en la codificacion del certificado"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode INVOCATION_WITHOUT_URL				= new ErrorCode("600008", "Se ha invocado sin URL a la actividad de firma por protocolo"); //$NON-NLS-1$ //$NON-NLS-2$

        public static final ErrorCode CANT_DECODE_DATA						= new ErrorCode("600108", "Los datos de la operacion no se han podido decodificar para la operacion de firma"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode PARAM_NOT_COMPATIBLE_POLICY			= new ErrorCode("600109", "Los parametros configurados son incompatibles con la politica de firma"); //$NON-NLS-1$ //$NON-NLS-2$

        public static final ErrorCode NO_DATA_NO_ID_BATCH			        = new ErrorCode("600401", "No se ha recibido los datos en la peticion ni el id del fichero a descargar para la operacion de firma de lotes"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode URL_DOWNLOAD_NOT_FOUND_BATCH			= new ErrorCode("600401", "Es necesario descargar la informacion del servidor intermedio, ha llegado el id de fichero pero no ha llegado la url del servidor de descarga para la operacion de firma de lotes"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode JSON_NOT_FORMED_CORRECTLY			    = new ErrorCode("600407", "El data de la operacion de firma batch no es un JSON valido"); //$NON-NLS-1$ //$NON-NLS-2$
        public static final ErrorCode ALGORITHM_NOT_FOUND_BATCH			    = new ErrorCode("600408", "No se ha recibido el algoritmo de firma en la firma batch"); //$NON-NLS-1$ //$NON-NLS-2$

    }
}
