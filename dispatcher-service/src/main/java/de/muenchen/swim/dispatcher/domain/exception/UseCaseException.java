package de.muenchen.swim.dispatcher.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class UseCaseException extends Exception {
    public UseCaseException(final String message) {
        super(message);
    }
}