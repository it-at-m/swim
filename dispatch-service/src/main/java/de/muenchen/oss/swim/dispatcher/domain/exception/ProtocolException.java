package de.muenchen.oss.swim.dispatcher.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class ProtocolException extends RuntimeException {
    public ProtocolException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
