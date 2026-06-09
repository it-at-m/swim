package de.muenchen.oss.swim.libs.handlercore.domain.model;

import jakarta.validation.constraints.NotBlank;

public record SingleFileEvent(
        @NotBlank String useCase,
        @NotBlank String presignedUrl,
        String metadataPresignedUrl) implements FileEvent {
    public static final String TYPE_NAME = "single";
}
