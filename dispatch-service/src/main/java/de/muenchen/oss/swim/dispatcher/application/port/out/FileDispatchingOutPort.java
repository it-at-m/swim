package de.muenchen.oss.swim.dispatcher.application.port.out;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import jakarta.validation.Valid;
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
     * @param presignedFile The presigned information for a file.
     */
    void dispatchFile(@NotBlank String bindingName, @NotBlank String useCase, @NotNull @Valid PresignedFile presignedFile);
}
