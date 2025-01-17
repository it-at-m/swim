package de.muenchen.oss.swim.dispatcher.dispatcher.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class FileSystemAccessException extends RuntimeException {
    public FileSystemAccessException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
