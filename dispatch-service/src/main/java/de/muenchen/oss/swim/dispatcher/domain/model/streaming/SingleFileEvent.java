package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Event for processing a single file.
 * <p>
 * ATTENTION: This class needs to match the one in the dispatch-service.
 *
 * @param useCase The use case of the event.
 * @param file The file to process.
 */
public record SingleFileEvent(
        @NotBlank String useCase,
        @JsonUnwrapped @NotNull @Valid PresignedFile file) implements FileEvent {

    public static final String TYPE_NAME = "single";

    public SingleFileEvent {
        if (useCase == null || useCase.isBlank()) {
            throw new IllegalArgumentException("useCase must not be null or blank");
        }
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
    }

    @Override
    public List<PresignedFile> files() {
        return List.of(file);
    }
}
