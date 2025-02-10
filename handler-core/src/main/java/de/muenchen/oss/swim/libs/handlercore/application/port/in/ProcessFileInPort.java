package de.muenchen.oss.swim.libs.handlercore.application.port.in;

import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ProcessFileInPort {
    /**
     * Start processing the given file.
     *
     * @param useCase The use case of the file.
     * @param file The attributes of the file.
     * @param presignedUrl The presigned url to the file.
     * @param metadataPresignedUrl The presigned url to the metadata file.
     * @throws PresignedUrlException Is thrown when presign url can't be parsed or isn't valid.
     * @throws UnknownUseCaseException Is thrown when use case name isn't known.
     */
    void processFile(@NotBlank String useCase, @Valid File file, @NotBlank String presignedUrl, String metadataPresignedUrl)
            throws PresignedUrlException, UnknownUseCaseException, MetadataException;
}
