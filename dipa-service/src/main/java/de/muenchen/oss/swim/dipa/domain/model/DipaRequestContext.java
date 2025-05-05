package de.muenchen.oss.swim.dipa.domain.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Context for a DiPa request.
 *
 * @param username User under which the request is executed.
 */
public record DipaRequestContext(
        @NotBlank String username) {
}
