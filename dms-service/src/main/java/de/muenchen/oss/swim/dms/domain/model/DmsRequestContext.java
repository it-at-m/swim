package de.muenchen.oss.swim.dms.domain.model;

import lombok.Data;

/**
 * Context for an DMS request.
 */
@Data
public class DmsRequestContext {
    /**
     * User under which a DMS request is executed.
     */
    final String username;
    /**
     * Used to resolve user role under which the DMS request is executed, default role if not defined.
     */
    final String joboe;
    /**
     * Used to resolve user role under which the DMS request is executed, default role if not defined.
     */
    final String jobposition;
}
