package de.muenchen.swim.dms.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class UnknownUseCaseException extends RuntimeException {
    public UnknownUseCaseException(final String message) {
        super(message);
    }
}
