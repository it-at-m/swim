package de.muenchen.oss.swim.dispatcher.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class StreamingException extends RuntimeException {
    public StreamingException(final String message) {
        super(message);
    }

    public StreamingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
