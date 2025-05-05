package de.muenchen.oss.swim.dipa.domain.model;

import lombok.Getter;

@Getter
public enum UseCaseType {
    /**
     * Create an Incoming inside a HrSubfile.
     */
    HR_SUBFILE_INCOMING(DipaResourceType.HR_SUBFILE, DipaResourceType.INCOMING);

    /**
     * The type of the target to create the new resource under.
     */
    private final DipaResourceType target;
    /**
     * The type of resource to create.
     */
    private final DipaResourceType type;

    /**
     * Type of the use case.
     * Specifies where and what resource is created.
     *
     * @param target The type of the target to create the new resource under.
     * @param type The type of resource to create.
     */
    UseCaseType(final DipaResourceType target, final DipaResourceType type) {
        this.target = target;
        this.type = type;
    }
}
