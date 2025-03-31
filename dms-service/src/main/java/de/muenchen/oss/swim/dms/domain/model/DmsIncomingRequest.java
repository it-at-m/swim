package de.muenchen.oss.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Properties for creating a new Incoming with a ContentObject.
 *
 * @param name The name of the new Incoming.
 * @param subject The subject of the new Incoming.
 * @param contentObject The properties for the new ContentObject created inside the Incoming.
 */
public record DmsIncomingRequest(@NotBlank String name, String subject, DmsContentObjectRequest contentObject) {
}
