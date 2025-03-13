package de.muenchen.oss.swim.dms.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Target for an DMS action.
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DmsTarget extends DmsRequestContext {
    /**
     * COO of an object.
     */
    private final String coo;

    public DmsTarget(final String coo, final String username, final String joboe, final String jobposition) {
        super(username, joboe, jobposition);
        this.coo = coo;
    }
}
