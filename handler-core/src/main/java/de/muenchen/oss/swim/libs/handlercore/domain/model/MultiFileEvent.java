package de.muenchen.oss.swim.libs.handlercore.domain.model;

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
}
