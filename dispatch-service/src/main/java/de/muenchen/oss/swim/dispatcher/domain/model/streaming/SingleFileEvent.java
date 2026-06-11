package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SingleFileEvent(
        @NotBlank String useCase,
        @JsonUnwrapped @NotNull @Valid PresignedFile presignedFile) implements FileEvent {

    public static final String TYPE_NAME = "single";

    public SingleFileEvent {
        if (useCase == null || useCase.isBlank()) {
            throw new IllegalArgumentException("useCase must not be null or blank");
        }
        if (presignedFile == null) {
            throw new IllegalArgumentException("presignedUrl must not be null or blank");
        }
    }
}
