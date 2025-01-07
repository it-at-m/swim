package de.muenchen.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;

public record DmsTarget(
        @NotBlank String coo,
        @NotBlank String userName,
        String joboe,
        String jobposition) {
}
