package de.muenchen.oss.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Properties for creating a new Procedure.
 *
 * @param name The name of the new Procedure.
 */
public record DmsProcedureRequest(@NotBlank String name) {
}
