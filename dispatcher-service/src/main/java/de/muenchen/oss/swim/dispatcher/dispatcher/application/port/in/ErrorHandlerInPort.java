package de.muenchen.oss.swim.dispatcher.dispatcher.application.port.in;

import de.muenchen.oss.swim.dispatcher.dispatcher.domain.model.ErrorDetails;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ErrorHandlerInPort {
    /**
     * Handle error which was thrown while processing dispatched message.
     * Could either be in external service or while marking file as finished.
     *
     * @param useCaseName Name of the useCase for which the Exception occurred.
     * @param presignedUrl PresignedUrl of the file for which the Exception occurred.
     * @param metadataPresignedUrl PresignedUrl of the metadata file for which the Exception occurred.
     * @param cause The Exception which occurred.
     */
    void handleError(String useCaseName, String presignedUrl, String metadataPresignedUrl, @NotNull ErrorDetails cause);
}
