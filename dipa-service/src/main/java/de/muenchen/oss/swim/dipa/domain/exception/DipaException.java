package de.muenchen.oss.swim.dipa.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class DipaException extends RuntimeException {
    public DipaException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DipaException(final String message) {
        super(message);
    }
}
