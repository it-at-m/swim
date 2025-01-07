package de.muenchen.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;

public record File(
        @NotBlank String bucket,
        @NotBlank String path) {
}
