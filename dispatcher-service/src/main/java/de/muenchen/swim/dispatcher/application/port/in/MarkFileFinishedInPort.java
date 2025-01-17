package de.muenchen.swim.dispatcher.application.port.in;

import de.muenchen.swim.dispatcher.domain.exception.PresignedUrlException;
import de.muenchen.swim.dispatcher.domain.exception.UseCaseException;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public interface MarkFileFinishedInPort {
    /**
     * Mark a file as finished processing.
     *
     * @param useCase The name of the use case the file was found for.
     * @param presignedUrl The presigned url of the file.
     */
    void markFileFinished(@NotBlank String useCase, @NotBlank String presignedUrl) throws PresignedUrlException, UseCaseException;
}
