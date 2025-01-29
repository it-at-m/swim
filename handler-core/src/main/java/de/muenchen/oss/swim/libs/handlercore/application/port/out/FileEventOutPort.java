package de.muenchen.oss.swim.libs.handlercore.application.port.out;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public interface FileEventOutPort {
    /**
     * Notify dispatcher that file processing was finished successfully.
     *
     * @param useCase The use case of the file.
     * @param presignedUrl The presigned url of the file.
     * @param metadataPresignedUrl The presigned url of the metadata file.
     */
    void fileFinished(@NotBlank String useCase, @NotBlank String presignedUrl, String metadataPresignedUrl);
}
