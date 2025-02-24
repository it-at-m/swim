package de.muenchen.oss.swim.invoice.application.usecase;

import de.muenchen.oss.swim.invoice.application.port.out.InvoiceServiceOutPort;
import de.muenchen.oss.swim.invoice.configuration.InvoiceMeter;
import de.muenchen.oss.swim.libs.handlercore.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase implements ProcessFileInPort {
    private final FileSystemOutPort fileSystemOutPort;
    private final InvoiceServiceOutPort invoiceServiceOutPort;
    private final FileEventOutPort fileEventOutPort;
    private final InvoiceMeter invoiceMeter;

    @Override
    public void processFile(final FileEvent event, final File file)
            throws PresignedUrlException {
        log.info("Processing file {} for use case {}", file, event.useCase());
        // load file
        try (InputStream fileStream = fileSystemOutPort.getPresignedUrlFile(event.presignedUrl())) {
            // create invoice
            this.invoiceServiceOutPort.createInvoice(file.getFileName(), fileStream);
        } catch (final IOException e) {
            throw new PresignedUrlException("Error while handling file InputStream", e);
        }
        // mark file as finished
        fileEventOutPort.fileFinished(event);
        log.info("File {} in use case {} finished", file, event.useCase());
        // update metric
        invoiceMeter.incrementProcessed(event.useCase());
    }
}
