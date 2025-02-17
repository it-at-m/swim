package de.muenchen.oss.swim.libs.handlercore.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class MetadataException extends Exception {
    public MetadataException(final String message) {
        super(message);
    }

    public MetadataException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
