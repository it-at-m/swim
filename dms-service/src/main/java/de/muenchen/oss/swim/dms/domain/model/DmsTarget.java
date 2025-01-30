package de.muenchen.oss.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Target for an DMS action.
 *
 * @param coo COO of an object.
 * @param userName User under which a DMS action is executed.
 * @param joboe Used to resolve user role under which the DMS action is executed, default role if not defined.
 * @param jobposition Used to resolve user role under which the DMS action is executed, default role if not defined.
 */
public record DmsTarget(
        String coo,
        @NotBlank String userName,
        String joboe,
        String jobposition) {
}
