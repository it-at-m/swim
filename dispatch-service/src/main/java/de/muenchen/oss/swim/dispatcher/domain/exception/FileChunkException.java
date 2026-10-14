package de.muenchen.oss.swim.dispatcher.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class FileChunkException extends Exception {
    public FileChunkException(final String message) {
        super(message);
    }
}
