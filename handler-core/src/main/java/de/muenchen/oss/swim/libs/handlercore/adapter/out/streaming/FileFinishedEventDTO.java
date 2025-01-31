package de.muenchen.oss.swim.libs.handlercore.adapter.out.streaming;

import jakarta.validation.constraints.NotBlank;

public record FileFinishedEventDTO(
        @NotBlank String useCase,
        @NotBlank String presignedUrl,
        String metadataPresignedUrl) {
}
