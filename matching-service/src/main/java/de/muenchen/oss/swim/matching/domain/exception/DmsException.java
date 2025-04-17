package de.muenchen.oss.swim.matching.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class DmsException extends RuntimeException {
    public DmsException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DmsException(final String message) {
        super(message);
    }
}
