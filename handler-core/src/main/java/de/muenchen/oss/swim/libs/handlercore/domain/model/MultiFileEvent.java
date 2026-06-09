package de.muenchen.oss.swim.libs.handlercore.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MultiFileEvent(
        @NotBlank String useCase,
        @NotEmpty List<@Valid PresignedFile> files) implements FileEvent {
    public static final String TYPE_NAME = "multi";
}
