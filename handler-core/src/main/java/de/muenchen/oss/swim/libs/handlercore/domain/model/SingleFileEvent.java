package de.muenchen.oss.swim.libs.handlercore.domain.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SingleFileEvent(
        @NotBlank String useCase,
        @JsonUnwrapped @NotNull @Valid PresignedFile presignedFile) implements FileEvent {

    public static final String TYPE_NAME = "single";
}
