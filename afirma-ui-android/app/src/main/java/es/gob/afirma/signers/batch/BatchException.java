package es.gob.afirma.signers.batch;

public class BatchException extends Exception {
    private static final long serialVersionUID = 1L;

	public BatchException(final String msg, final Throwable e) {
		super(msg, e);
	}
}
