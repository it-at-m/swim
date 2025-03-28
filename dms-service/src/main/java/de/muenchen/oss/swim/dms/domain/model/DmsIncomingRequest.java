package de.muenchen.oss.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;

public record DmsIncomingRequest(@NotBlank String name, String subject, DmsContentObjectRequest contentObject) {
}
