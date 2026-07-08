package de.muenchen.oss.swim.dispatcher.domain.model.streaming;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

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

    public static MultiFileEvent fromFileEvent(final FileEvent fileEvent) {
        if (fileEvent instanceof MultiFileEvent multi) {
            return multi;
        } else if (fileEvent instanceof SingleFileEvent(String sUc, PresignedFile sPf)) {
            return new MultiFileEvent(sUc, List.of(sPf));
        } else {
            throw new IllegalArgumentException("Message payload is no valid event but '%s'".formatted(fileEvent.getClass()));
        }
    }
}
