package de.muenchen.oss.swim.dipa.domain.model.dipa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request for creating an Incoming with a ContentObject.
 *
 * @param subject Subject of the new ContentObject.
 * @param contentObject The ContentObject to create inside the Incoming.
 */
public record IncomingRequest(
        String subject,
        @NotNull @Valid ContentObjectRequest contentObject) {
}
