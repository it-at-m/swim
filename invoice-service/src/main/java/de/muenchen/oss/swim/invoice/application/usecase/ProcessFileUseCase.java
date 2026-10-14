package de.muenchen.oss.swim.invoice.application.usecase;

import de.muenchen.oss.swim.invoice.application.port.out.InvoiceServiceOutPort;
import de.muenchen.oss.swim.invoice.configuration.InvoiceMeter;
import de.muenchen.oss.swim.libs.handlercore.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileReference;
import de.muenchen.oss.swim.libs.handlercore.domain.model.MultiFileEvent;
import de.muenchen.oss.swim.libs.handlercore.domain.model.PresignedFile;
import de.muenchen.oss.swim.libs.handlercore.domain.model.SingleFileEvent;
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
    public void processEvent(final SingleFileEvent event)
            throws PresignedUrlException {
        final PresignedFile presignedFile = event.file();
        final FileReference file = FileReference.fromPresignedUrl(presignedFile.presignedUrl());
        log.info("Processing file {} for use case {}", file, event.useCase());
        // load file
        try (InputStream fileStream = fileSystemOutPort.getPresignedUrlFile(presignedFile.presignedUrl())) {
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

    @Override
    public void processEvent(MultiFileEvent event) {
        throw new IllegalArgumentException("Handling of multi event is not supported");
    }
}
