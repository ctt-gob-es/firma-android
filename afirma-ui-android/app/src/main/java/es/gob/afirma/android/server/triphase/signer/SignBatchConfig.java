/*
 * Este fichero es parte de Server triphase signer de ClienteFirma,
 * ha sido adaptado y modificado para el funcionamiento de la firma
 * batch en android.
 *
 * Este software sigue los mismos criterios de licencia previos a su
 * modificación: https://github.com/ctt-gob-es/clienteafirma/
 *
 * Modificado por: Eduardo García <eduardo.l.g.g@gmail.com>
 * Fecha modificación: 10 sep 2020
 */
package es.gob.afirma.android.server.triphase.signer;

import java.util.ArrayList;
import java.util.List;

import es.gob.afirma.android.signers.constants.SignAlgorithm;
import es.gob.afirma.android.signers.batch.signer.SingleSign;

/**
 * Configuraci&oacute;n para la firma de un lote.
 */
public class SignBatchConfig {

    /** Tiempo de espera que, por defecto, se aplicar&aacute;a a las distintas
     * operaciones de firma concurrente de datos. */
    private static final long DEFAULT_TIMEOUT = 30;

    private String id;

    private boolean stopOnError;

    private SignAlgorithm algorithm;

    private long concurrentTimeout;

    private final List<SingleSign> signs;

    /**
     * Construye un lote vac&iacute;o.
     */
    public SignBatchConfig() {
        this.id = null;
        this.stopOnError = true;
        this.concurrentTimeout = DEFAULT_TIMEOUT;
        this.signs = new ArrayList<>();
    }

    /**
     * Obtiene el ID del lote.
     * @return Identificador del lote o {@code null} si no est&aacute; definido.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Indica si se debe detener la ejecuci&oacute;n del lote al detectar un error.
     * @return {@code true}, valor por defecto, si se debe detener la ejecuci&oacute;n,
     * {@code false} en caso contrario.
     */
    public boolean isStopOnError() {
        return this.stopOnError;
    }

    /**
     * Devuelve el algoritmo de firma.
     * @return Algoritmo de firma.
     */
    public SignAlgorithm getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Devuelve el tiempo de espera m&aacute;ximo para la ejecuci&oacute;n del lote.
     * Por defecto, devuelve el valor 0 (se espera indefinidamente).
     * @return Tiempo de espera en milisegundos o 0 si se espera indefinidamente.
     */
    public long getConcurrentTimeout() {
        return this.concurrentTimeout;
    }

    /**
     * Establece el identificador del lote.
     * @param id Identificador del lote.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Establece si debe detenerse la ejecuci&oacute;n del lote en caso de error.
     * @param stopOnError {@code true} si se debe detener la ejecuci&oacute;n,
     * {@code false} en caso contrario.
     */
    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    /**
     * Establece el algoritmo de firma para los documentos del lote.
     * @param algorithm Algoritmo de firma.
     */
    public void setAlgorithm(SignAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Establece el tiempo m&aacute;ximo de espera para la firma del lote.
     * @param concurrentTimeout Tiempo m&aacute;ximo de espera o 0 si se
     * espera indefinidamente.
     */
    public void setConcurrentTimeout(long concurrentTimeout) {
        this.concurrentTimeout = concurrentTimeout;
    }

    /**
     * Agrega un nuevo documento al lote.
     * @param sign Informaci&oacute;n necesaria para la firma del documento.
     */
    public void addSingleSign(SingleSign sign) {
        this.signs.add(sign);
    }

    /**
     * Obtiene el listado de configuraciones de los documentos a firmar.
     * @return Listado de configuraciones de los documentos a firmar.
     */
    public List<SingleSign> getSingleSigns() {
        return this.signs;
    }
}
