package com.iambilotta.gdpr.starter.erasure;

/**
 * Thrown by {@link ErasureService} when one or more {@link ErasureListener}s failed after a
 * subject's erasure had already been honoured by the handlers. The erasure itself stands (the
 * handlers committed before any listener ran): this exception surfaces a broken downstream reaction
 * (e.g. a projection rebuild that failed), it never means the subject was un-erased. The first
 * listener failure is the {@link Throwable#getCause() cause}; later failures are suppressed
 * exceptions.
 */
public class ErasureListenerException extends RuntimeException {

    private final String subjectId;

    public ErasureListenerException(String subjectId, Throwable cause) {
        super("erasure of subject '" + subjectId + "' completed, but a post-erasure listener failed; "
                + "the subject is erased, the downstream reaction is not (rebuild/invalidate it)", cause);
        this.subjectId = subjectId;
    }

    public String subjectId() {
        return subjectId;
    }
}
