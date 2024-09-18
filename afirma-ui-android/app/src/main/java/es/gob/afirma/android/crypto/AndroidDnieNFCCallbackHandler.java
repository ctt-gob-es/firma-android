package es.gob.afirma.android.crypto;

import android.app.Activity;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import es.gob.afirma.android.Logger;
import es.gob.jmulticard.android.callbacks.CachePasswordCallback;
import es.gob.jmulticard.android.callbacks.DialogDoneChecker;
import es.gob.jmulticard.callback.CustomAuthorizeCallback;
import es.gob.jmulticard.callback.CustomTextInputCallback;

/** CallbackHandler que gestiona los Callbacks de petici&oacute;n de informaci&oacute;n al usuario.
 * @author Sergio Mart&iacute;nez Rico. */
public class AndroidDnieNFCCallbackHandler implements CallbackHandler {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma";

	private final Activity activity;
	private final DialogDoneChecker dialogDone;
	private CachePasswordCallback canPasswordCallback;
	private CachePasswordCallback pinPasswordCallback;

	/**
	 * CallbackHandler que gestiona los Callbacks de petici&oacute;n de informaci&oacute;n al
	 * usuario. Este CallbackHandler est&aacute; adaptado al funcionamiento del Portafirmas
	 * m&oacute;vil. El CAN nunca se deber&iacute;a pedir a trav&eacute;s del di&aacute;logo de este
	 * CallbackHandler, siempre se debe proporcionar de forma externa, mientras que el PIN si se
	 * pedir&aacute; a trav&eacute;s del di&aacute;logo interno.
	 *
	 * @param ac               Handler de la actividad desde la que se llama.
	 * @param ddc              Instancia de la clase utilizada para utilizar wait() y notify() al esperar el PIN.
	 * @param passwordCallback Instancia que contiene el CAN pedido antes a la lectura NFC.
	 * @param pinPasswordCallback Instancia que contiene el PIN pedido antes a la lectura NFC.
	 */
	public AndroidDnieNFCCallbackHandler(final Activity ac, final DialogDoneChecker ddc, final CachePasswordCallback passwordCallback, CachePasswordCallback pinPasswordCallback) {
		this.activity = ac;
		this.dialogDone = ddc;
		this.canPasswordCallback = passwordCallback;
		this.pinPasswordCallback = pinPasswordCallback;
	}

	@Override
	public void handle(final Callback[] callbacks) throws UnsupportedCallbackException {
		if (callbacks != null) {
			for (final Callback cb : callbacks) {

				if (cb instanceof PasswordCallback) {
					String input = new String(this.pinPasswordCallback.getPassword());
					((PasswordCallback) cb).setPassword(input.toCharArray());
					return;
				}
				String input;
				if (cb instanceof CustomTextInputCallback) {
					input = new String(this.canPasswordCallback.getPassword());
					((CustomTextInputCallback) cb).setText(input);
					return;
				}

				if (cb instanceof CustomAuthorizeCallback) {
					return;
				}

				Logger.e(ES_GOB_AFIRMA, "Se ha solicitado un tipo de entrada desconocido: " + cb.getClass().getName());
			}
		}
		else {
			Logger.e(ES_GOB_AFIRMA, "Se ha recibido un array de Callbacks nulo"); //$NON-NLS-1$
			throw new UnsupportedCallbackException(null);
		}
	}
}
