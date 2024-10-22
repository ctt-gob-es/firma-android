/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 11/01/11
 * You may contact the copyright holder at: soporte.afirma5@mpt.es
 */

package es.gob.afirma.android.crypto;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import androidx.fragment.app.FragmentActivity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.util.Properties;

import es.gob.afirma.android.Logger;
import es.gob.afirma.android.gui.PinDialog;
import es.gob.jmulticard.android.nfc.AndroidNfcConnection;
import es.gob.jmulticard.connection.ApduConnection;

/** Facrtor&iacute;a de gestores de contrase&ntilde;as y claves para Android. */
public final class KeyStoreManagerFactory {

	private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$
	private static final String AET_PKCS11_STORE = "PKCS11KeyStore"; //$NON-NLS-1$

	private KeyStoreManagerFactory() {
		// Se prohibe crear instancias
	}

	/** Obtiene el gestor de contrase&ntilde;as y claves m&aacute;s apropiado seg&uacute;n el entorno
	 * operativo y el hardware encontrado.
	 * @param activity Actividad padre
	 * @param ksml Clase a la que hay que notificar la finalizaci&oacute;n de la
	 *             carga e inicializaci&oacute;n del gestor de claves y certificados
	 * @param usbDevice Dispositivo USB en el caso de almacenes de claves externos
	 * @param usbManager Gestor de dispositivos USB en el caso de almacenes de claves externos */
	public static KeyStore initKeyStoreManager(final FragmentActivity activity,
											   final KeyStoreManagerListener ksml,
											   final UsbDevice usbDevice,
											   final UsbManager usbManager) {

		// Buscamos primero una tarjeta CERES
		if (usbDevice != null && usbManager != null) {
			try {

				final Class<?> androidCCIDConnectionClass = Class.forName("es.inteco.labs.android.usb.AndroidCCIDConnection"); //$NON-NLS-1$
				final Object androidCCIDConnectionObject = androidCCIDConnectionClass.getConstructor(
						UsbManager.class,
						UsbDevice.class
				).newInstance(usbManager, usbDevice);

				final Class<?> dnieProviderClass = Class.forName("es.gob.jmulticard.jse.provider.ceres.CeresProvider"); //$NON-NLS-1$
				final Provider p = (Provider) dnieProviderClass.getConstructor(
						Class.forName("es.gob.jmulticard.apdu.connection.ApduConnection") //$NON-NLS-1$
				).newInstance(androidCCIDConnectionObject);

				Security.addProvider(p);

				// Obtenemos el almacen unicamente para ver si falla
				return KeyStore.getInstance("CERES", p); //$NON-NLS-1$
			}
			catch (final ClassNotFoundException | NoSuchMethodException e) {
				Logger.w(ES_GOB_AFIRMA, "No se encuentran las bibliotecas de acceso a la tarjeta CERES", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (final KeyStoreException e) {
				Logger.w(ES_GOB_AFIRMA, "Se ha encontrado un CCID USB, pero no una tarjeta CERES en el", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (final Exception e) {
				Logger.w(ES_GOB_AFIRMA, "No se ha podido instanciar el controlador de la tarjeta CERES", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		// Si no, buscamos un DNIe en el CCID USB
		if (usbDevice != null && usbManager != null) {
			try {

				final Class<?> androidCCIDConnectionClass = Class.forName("es.inteco.labs.android.usb.AndroidCCIDConnection"); //$NON-NLS-1$
				final Object androidCCIDConnectionObject = androidCCIDConnectionClass.getConstructor(
					UsbManager.class,
					UsbDevice.class
				).newInstance(usbManager, usbDevice);

				final Class<?> dnieProviderClass = Class.forName("es.gob.jmulticard.jse.provider.DnieProvider"); //$NON-NLS-1$
				final Provider p = (Provider) dnieProviderClass.getConstructor(
						Class.forName("es.gob.jmulticard.apdu.connection.ApduConnection") //$NON-NLS-1$
				).newInstance(androidCCIDConnectionObject);

				Security.addProvider(p);

				// Obtenemos el almacen unicamente para ver si falla
				return KeyStore.getInstance("DNI", p); //$NON-NLS-1$
			}
			catch (final ClassNotFoundException | NoSuchMethodException e) {
				Logger.w(ES_GOB_AFIRMA, "No se encuentran las bibliotecas de acceso al DNIe: " + e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (final KeyStoreException e) {
				Logger.w(ES_GOB_AFIRMA, "Se ha encontrado un CCID USB, pero no un DNIe en el: " + e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (final Exception e) {
				Logger.w(ES_GOB_AFIRMA, "No se ha podido instanciar el controlador del DNIe por chip: " + e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		// Si no encontramos el almacen anterior, intentamos la instanciacion de un PKCS#11 MSC AET
		try {
			// Todo el proceso se hace por reflexion, porque el entorno de ejecucion es completamente opcional
			// Si falla en este primer bloque devolvemos el almacen de Android, de forma transparente para el usuario
			final Properties providerConfiguration = new Properties();
			final Class<?> contextManagerClass = Class.forName("com.aet.android.javaprovider.context.ContextManager"); //$NON-NLS-1$
			final Field contextManagerTypeFiled = contextManagerClass.getDeclaredField("CONTEXT_MANAGER_TYPE"); //$NON-NLS-1$
			final Class<?> contextManagerTypeClass = Class.forName("com.aet.android.javaprovider.context.ContextManagerType"); //$NON-NLS-1$
			final Field microsdField = contextManagerTypeClass.getDeclaredField("MICROSD"); //$NON-NLS-1$
			final Object microsdObject = microsdField.get(null);
			final Method getTypeMethod = contextManagerTypeClass.getDeclaredMethod("getType"); //$NON-NLS-1$
			final Object typeValue = getTypeMethod.invoke(microsdObject);

			providerConfiguration.put(
					contextManagerTypeFiled.get(null),
					typeValue
					);

			final Class<?> aetProviderClass = Class.forName("com.aet.android.javaprovider.AETProvider"); //$NON-NLS-1$
			final Field aetProviderTypeField = aetProviderClass.getDeclaredField("PROVIDER_TYPE"); //$NON-NLS-1$
			final Object aetProviderTypeObject = aetProviderTypeField.get(null);
			final Class<?> aetProviderTypeClass = Class.forName("com.aet.android.javaprovider.AETProviderType"); //$NON-NLS-1$
			final Field javaField = aetProviderTypeClass.getDeclaredField("JAVA"); //$NON-NLS-1$
			final Object javaObject = javaField.get(null);
			final Method aetProviderGetTypeMethod = aetProviderTypeClass.getDeclaredMethod("getType"); //$NON-NLS-1$
			final Object javaTypeValue = aetProviderGetTypeMethod.invoke(javaObject);

			providerConfiguration.put(
					aetProviderTypeObject,
					javaTypeValue
					);

			final Method aetProviderGetInstanceMethod = aetProviderClass.getDeclaredMethod("getInstance", Properties.class, Context.class); //$NON-NLS-1$
			final Provider provider = (Provider) aetProviderGetInstanceMethod.invoke(
				null,
				providerConfiguration,
				activity.getApplicationContext()
			);

			Security.addProvider(provider);
			Logger.i(ES_GOB_AFIRMA, "Anadido el proveedor AET: " + provider.getName());  //$NON-NLS-1$

			// Obtenemos el almacen unicamente para ver si falla
			KeyStore.getInstance(AET_PKCS11_STORE, provider);

			Logger.i(ES_GOB_AFIRMA, "Se ha instanciado correctamente el proveedor AET");  //$NON-NLS-1$

			// A partir de este punto, si falla, terminamos con error y no devolvemos el almacen de Android, mostrando
			// un dialogo de error al usuario

			// Si llegamos hasta aqui sin errores preguntamos el PIN al usuario
			// KeyStore: "PKCS11KeyStore", Proveedor: "AETProvider"
			final PinDialog pinDialog = PinDialog.newInstance("AETProvider", "PKCS11KeyStore", ksml); //$NON-NLS-1$ //$NON-NLS-2$
			pinDialog.show(activity.getSupportFragmentManager(), "PinDialog"); //$NON-NLS-1$

			return KeyStore.getInstance(AET_PKCS11_STORE, provider);
		}
		catch (final Exception e) {
			Logger.w(ES_GOB_AFIRMA, "No se ha detectado una MSC: " + e); //$NON-NLS-1$
		}

		// Si no encontramos el almacen anterior, accedemos al almacen del sistema
		Logger.i(ES_GOB_AFIRMA, "Estableciendo almacen del sistema"); //$NON-NLS-1$
		ksml.onLoadingKeyStoreSuccess(new Android4KeyStoreManager(activity));
		return null;
	}

	/** Obtiene el gestor de contrase&ntilde;as y claves m&aacute;s apropiado seg&uacute;n el entorno
	 * operativo y el hardware encontrado.
	 * @param ksml Clase a la que hay que notificar la finalizaci&oacute;n de la
	 *             carga e inicializaci&oacute;n del gestor de claves y certificados
	 */
	public static KeyStore initNfcKeyStoreManager(
								final KeyStoreManagerListener ksml)
									throws UnsupportedNfcCardException,
			InitializingNfcCardException {

		KeyStore ks = null;


		// En caso de no existir un lector conectado por USB, comprobamos que se haya detectado una tarjeta por NFC
		DnieConnectionManager dnieManager = DnieConnectionManager.getInstance();
		if (dnieManager.getDiscoveredTag() != null) {
			try {
				final ApduConnection androidNfcConnectionObject =
						new AndroidNfcConnection(dnieManager.getDiscoveredTag());
				dnieManager.setNfcConnection(androidNfcConnectionObject);
				final Provider p = new es.gob.jmulticard.jse.provider.DnieProvider(androidNfcConnectionObject);

				Security.addProvider(p);

				// Obtenemos el almacen unicamente para ver si falla
				ks = KeyStore.getInstance("DNI", p); //$NON-NLS-1$
			} catch (final KeyStoreException e) {
				Logger.e(ES_GOB_AFIRMA, "Se ha encontrado una tarjeta por NFC, pero no es un DNIe: " + e); //$NON-NLS-1$ //$NON-NLS-2$
				throw new UnsupportedNfcCardException("Se ha encontrado una tarjeta por NFC distinta al DNIe", e);
			} catch (final Exception e) {
				Logger.e(ES_GOB_AFIRMA, "No se ha podido instanciar el controlador del DNIe por NFC: " + e, e); //$NON-NLS-1$ //$NON-NLS-2$
				throw new InitializingNfcCardException("Error inicializando la tarjeta", e);
			}
		}

		return ks;
	}
}