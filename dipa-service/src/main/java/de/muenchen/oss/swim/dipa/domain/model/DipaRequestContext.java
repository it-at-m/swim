package de.muenchen.oss.swim.dipa.domain.model;

import jakarta.validation.constraints.NotBlank;

public record DipaRequestContext(
        @NotBlank String username) {
}
