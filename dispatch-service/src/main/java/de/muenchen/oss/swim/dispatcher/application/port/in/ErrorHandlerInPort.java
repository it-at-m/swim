package de.muenchen.oss.swim.dispatcher.application.port.in;

import de.muenchen.oss.swim.dispatcher.domain.model.ErrorDetails;
import de.muenchen.oss.swim.dispatcher.domain.model.FileEvent;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ErrorHandlerInPort {
    /**
     * Handle error which was thrown while processing dispatched message.
     * Could either be in external service or while marking file as finished.
     *
     * @param event The event for the file the error occurred for.
     * @param cause The Exception which occurred.
     */
    void handleError(@NotNull FileEvent event, @NotNull ErrorDetails cause);
}
