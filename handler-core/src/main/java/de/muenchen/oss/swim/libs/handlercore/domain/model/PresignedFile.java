package de.muenchen.oss.swim.libs.handlercore.domain.model;

import jakarta.validation.constraints.NotBlank;

public record PresignedFile(@NotBlank String presignedUrl, String metadataPresignedUrl) {
}
