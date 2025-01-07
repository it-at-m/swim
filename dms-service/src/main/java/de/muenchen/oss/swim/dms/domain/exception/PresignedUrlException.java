package de.muenchen.oss.swim.dms.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class PresignedUrlException extends RuntimeException {
    public PresignedUrlException(final String message) {
        super(message);
    }

    public PresignedUrlException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
