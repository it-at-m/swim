package de.muenchen.oss.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;

public record DmsContentObjectRequest(@NotBlank String name, String subject) {
}
