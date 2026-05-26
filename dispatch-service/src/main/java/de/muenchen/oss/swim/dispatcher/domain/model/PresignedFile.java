package de.muenchen.oss.swim.dispatcher.domain.model;

import jakarta.validation.constraints.NotBlank;

public record PresignedFile(@NotBlank String presignedUrl, @NotBlank String metadataPresignedUrl) {
}
