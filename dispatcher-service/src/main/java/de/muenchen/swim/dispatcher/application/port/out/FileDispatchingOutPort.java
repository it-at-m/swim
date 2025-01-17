package de.muenchen.swim.dispatcher.application.port.out;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface FileDispatchingOutPort {
    /**
     * Dispatch a file for further processing.
     *
     * @param bindingName The name to send the notification to.
     * @param useCase The name of the use case the file was found for.
     * @param presignedUrl The presigned url of the file.
     * @param metadataPresignedUrl The presigned url of the metadata file. Only if required by use case.
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    void dispatchFile(@NotBlank String bindingName, @NotBlank String useCase, @NotNull String presignedUrl, String metadataPresignedUrl);
}
