package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Event for processing multiple files.
 * <p>
 * ATTENTION: This class need to match the one in the dispatch-service.
 *
 * @param useCase The use case of the event.
 * @param files The files to process.
 */
public record MultiFileEvent(
        @NotBlank String useCase,
        @NotEmpty List<@NotNull @Valid PresignedFile> files) implements FileEvent {
    public static final String TYPE_NAME = "multi";

    public MultiFileEvent {
        if (useCase == null || useCase.isBlank()) {
            throw new IllegalArgumentException("useCase must not be null or blank");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files must not be null or empty");
        }
    }
}
