package de.muenchen.oss.swim.dms.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class MetadataException extends RuntimeException {
    public MetadataException(final String message) {
        super(message);
    }

    public MetadataException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
