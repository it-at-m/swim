package de.muenchen.oss.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;

public record DmsTarget(
        String coo,
        @NotBlank String userName,
        String joboe,
        String jobposition) {
}
