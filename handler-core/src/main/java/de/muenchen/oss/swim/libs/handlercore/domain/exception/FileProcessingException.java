package de.muenchen.oss.swim.libs.handlercore.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class FileProcessingException extends RuntimeException {
    public FileProcessingException(final Throwable cause) {
        super(cause);
    }
}
