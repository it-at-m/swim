package de.muenchen.oss.swim.dipa.domain.model.dipa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IncomingRequest(
        @NotBlank String subject,
        @NotNull @Valid ContentObjectRequest contentObject) {
}
