package de.muenchen.oss.swim.invoice.application.port.out;

import java.io.InputStream;

public interface InvoiceServiceOutPort {
    /**
     * Create Invoice from file.
     *
     * @param filename Filename of the invoice.
     * @param inputStream Content of the file.
     */
    void createInvoice(String filename, InputStream inputStream);
}
