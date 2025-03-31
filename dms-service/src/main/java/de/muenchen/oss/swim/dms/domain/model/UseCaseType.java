package de.muenchen.oss.swim.dms.domain.model;

import lombok.Getter;

@Getter
public enum UseCaseType {
    /**
     * Create a Content Object inside an Inbox.
     */
    INBOX_CONTENT_OBJECT(DmsResourceType.INBOX, DmsResourceType.CONTENT_OBJECT),
    /**
     * Create an Incoming inside an Inbox.
     */
    INBOX_INCOMING(DmsResourceType.INBOX, DmsResourceType.INCOMING),
    /**
     * Create an Incoming either inside given Procedure {@link DmsTarget#getCoo()} or OU work queue of
     * {@link DmsTarget#getUsername()}.
     */
    PROCEDURE_INCOMING(DmsResourceType.PROCEDURE, DmsResourceType.INCOMING),
    /**
     * Resolve target resource type from metadata file.
     */
    METADATA_FILE(null, null);

    /**
     * The type of the target to create the new resource under.
     */
    private final DmsResourceType target;
    /**
     * The type of resource to create.
     */
    private final DmsResourceType type;

    /**
     * Type of the use case.
     * Specifies where and what resource is created.
     *
     * @param target The type of the target to create the new resource under.
     * @param type The type of resource to create.
     */
    UseCaseType(final DmsResourceType target, final DmsResourceType type) {
        this.target = target;
        this.type = type;
    }
}
