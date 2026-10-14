package de.muenchen.oss.swim.libs.handlercore.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class StreamingException extends RuntimeException {
    public StreamingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
