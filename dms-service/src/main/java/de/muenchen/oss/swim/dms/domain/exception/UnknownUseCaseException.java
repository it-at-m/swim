package de.muenchen.oss.swim.dms.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class UnknownUseCaseException extends Exception {
    public UnknownUseCaseException(final String message) {
        super(message);
    }
}
