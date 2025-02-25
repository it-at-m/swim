package de.muenchen.oss.swim.invoice.domain.exception;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class InvoiceException extends RuntimeException {
    public InvoiceException(final String message) {
        super(message);
    }

    public InvoiceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
