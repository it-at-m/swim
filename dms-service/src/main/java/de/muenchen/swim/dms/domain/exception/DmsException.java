package de.muenchen.swim.dms.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class DmsException extends RuntimeException {
    public DmsException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
