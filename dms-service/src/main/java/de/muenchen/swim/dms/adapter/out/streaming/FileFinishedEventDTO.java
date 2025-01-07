package de.muenchen.swim.dms.adapter.out.streaming;

import jakarta.validation.constraints.NotBlank;

public record FileFinishedEventDTO(
        @NotBlank String useCase,
        @NotBlank String presignedUrl,
        String metadataPresignedUrl) {
}
