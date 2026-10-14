package de.muenchen.oss.swim.dispatcher.application.port.in;

import de.muenchen.oss.swim.dispatcher.domain.model.ErrorDetails;
import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ErrorHandlerInPort {
    /**
     * Handle error which was thrown while processing dispatched message.
     * Could either be in external service or while marking file as finished.
     *
     * @param useCaseName Name of the useCase for which the Exception occurred.
     * @param presignedFile Presigned information of the file for which the Exception occurred.
     * @param cause The Exception which occurred.
     */
    void handleError(String useCaseName, PresignedFile presignedFile, @NotNull ErrorDetails cause);
}
