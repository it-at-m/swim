package de.muenchen.oss.swim.dispatcher.application.port.in;

import de.muenchen.oss.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.model.FileEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface MarkFileFinishedInPort {
    /**
     * Mark a file as finished processing.
     *
     * @param event The event for the file to marks as finished.
     */
    void markFileFinished(@NotNull @Valid FileEvent event) throws PresignedUrlException, UseCaseException;
}
