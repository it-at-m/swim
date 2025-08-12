package de.muenchen.oss.swim.dispatcher.domain.model;

import jakarta.validation.constraints.NotBlank;

public record FileEvent(
        @NotBlank String useCase,
        @NotBlank String presignedUrl,
        String metadataPresignedUrl) {
}
