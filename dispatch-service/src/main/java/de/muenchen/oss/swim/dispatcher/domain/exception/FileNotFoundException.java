package de.muenchen.oss.swim.dispatcher.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class FileNotFoundException extends Exception {
    public FileNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
