package de.muenchen.oss.swim.libs.handlercore.application.port.in;

import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ProcessFileInPort {
    /**
     * Start processing the given file.
     *
     * @param event The event for the file to process.
     * @param file The attributes of the file.
     * @throws PresignedUrlException Is thrown when presign url can't be parsed or isn't valid.
     * @throws UnknownUseCaseException Is thrown when use case name isn't known.
     */
    void processFile(@Valid FileEvent event, @Valid File file)
            throws PresignedUrlException, UnknownUseCaseException, MetadataException;
}
