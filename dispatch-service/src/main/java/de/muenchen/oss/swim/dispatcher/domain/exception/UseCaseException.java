package de.muenchen.oss.swim.dispatcher.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class UseCaseException extends Exception {
    public UseCaseException(final String message) {
        super(message);
    }

    public UseCaseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
