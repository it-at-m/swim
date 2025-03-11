package de.muenchen.oss.swim.dms.domain.model;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Context for an DMS request.
 */
@RequiredArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class DmsRequestContext {
    /**
     * User under which a DMS request is executed.
     */
    @NotBlank
    private final String username;
    /**
     * Used to resolve user role under which the DMS request is executed, default role if not defined.
     */
    private final String joboe;
    /**
     * Used to resolve user role under which the DMS request is executed, default role if not defined.
     */
    private final String jobposition;
}
