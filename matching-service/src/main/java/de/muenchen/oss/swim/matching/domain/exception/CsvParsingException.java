package de.muenchen.oss.swim.matching.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class CsvParsingException extends Exception {
    public CsvParsingException(final String message) {
        super(message);
    }

    public CsvParsingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
