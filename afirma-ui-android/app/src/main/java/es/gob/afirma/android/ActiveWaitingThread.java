package es.gob.afirma.android;

import java.io.IOException;

/**
 * Hilo para la solicitud continua de espera a trav&eacute;s del servidor
 * intermedio. Una vez en ejecuci&oacute;n, enviar&aacute; cada cierto
 * tiempo una solicitud de espera al servidor intermedio hasta que el hilo
 * se interrumpa.
 */
public class ActiveWaitingThread extends Thread {

    private static final String ES_GOB_AFIRMA = "es.gob.afirma"; //$NON-NLS-1$
    private static final String WAIT_CONSTANT = "#WAIT"; //$NON-NLS-1$
    private static final long SLEEP_PERIOD = 10000;

    private final String storageServiceUrl;
    private final String transactionId;

    private boolean cancelled = false;

    /**
     * Construye un hilo que cada cierto tiempo sube al servidor intermedio
     * un mensaje de espera.
     * @param storageServiceUrl URL del servicio de guardado del servidor intermedio.
     * @param transactionId Identificador de transacci&oacute;n.
     */
    public ActiveWaitingThread(final String storageServiceUrl, final String transactionId) {
        this.storageServiceUrl = storageServiceUrl;
        this.transactionId = transactionId;
    }

    @Override
    public void run() {

        while (!this.cancelled) {
            try {
                Thread.sleep(SLEEP_PERIOD);
            } catch (final InterruptedException e) {
                Logger.w(ES_GOB_AFIRMA, "No se ha podido esperar para el envio de la senal de espera activa para el JavaScript. "
                        + "No se envia la peticion", e); //$NON-NLS-1$
            }
            synchronized (IntermediateServerUtil.getUniqueSemaphoreInstance()) {
                if (!this.cancelled) {
                    try {
                        IntermediateServerUtil.sendData(WAIT_CONSTANT, this.storageServiceUrl, this.transactionId);
                    } catch (final IOException e) {
                        Logger.w(ES_GOB_AFIRMA, "No se ha podido enviar la peticion de espera" , e);
                    }
                }
            }
        }
    }

    @Override
    public void interrupt() {
        this.cancelled = true;
        super.interrupt();
    }
}
