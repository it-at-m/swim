package de.muenchen.oss.swim.dispatcher.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class FileSizeException extends Exception {
    public FileSizeException(final String message) {
        super(message);
    }
}
