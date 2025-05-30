package de.muenchen.oss.swim.dms.application.port.out;

import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsRequestContext;
import de.muenchen.oss.swim.dms.domain.model.DmsResourceType;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;

@Validated
public interface DmsOutPort {
    /**
     * Create ContentObject inside an Inbox.
     *
     * @param dmsTarget The target Inbox.
     * @param contentObjectRequest The values for the new ContentObject.
     * @param inputStream The content of the new ContentObject.
     * @return The coo of the new ContentObject.
     */
    String createContentObjectInInbox(@NotNull @Valid DmsTarget dmsTarget, @NotNull @Valid DmsContentObjectRequest contentObjectRequest,
            @NotNull InputStream inputStream);

    /**
     * Create Incoming inside an Inbox-
     *
     * @param dmsTarget The target Inbox.
     * @param incomingRequest The values for the new Incoming.
     * @param inputStream The content of the new ContentObject.
     * @return The coo of the new Incoming.
     */
    String createIncomingInInbox(@NotNull @Valid DmsTarget dmsTarget, @NotNull @Valid DmsIncomingRequest incomingRequest, @NotNull InputStream inputStream);

    /**
     * Create Incoming.
     * Either inside given Procedure {@link DmsTarget#getCoo()} or OU work queue of
     * {@link DmsTarget#getUsername()}.
     *
     * @param dmsTarget The target. If {@link DmsTarget#getCoo()} is defined: Procedure, if not: OU work
     *            queue.
     * @param incomingRequest The values for the new Incoming.
     * @param inputStream The content of the new ContentObject.
     * @return The coo of the new Incoming.
     */
    String createProcedureIncoming(@NotNull @Valid DmsTarget dmsTarget, @NotNull @Valid DmsIncomingRequest incomingRequest, @NotNull InputStream inputStream);

    /**
     * Get name of Procedure by coo.
     *
     * @param dmsTarget The Procedure to get the name of.
     * @return The name of the Procedure.
     */
    String getProcedureName(@NotNull @Valid DmsTarget dmsTarget);

    /**
     * Get the coo of the first Incoming where the name starts with the given name.
     *
     * @param dmsTarget The Procedure to search in.
     * @param incomingNamePrefix The prefix the Incoming name needs to start with.
     * @return The coo of the procedure. Null if it doesn't exist.
     */
    Optional<String> getIncomingCooByName(@NotNull @Valid DmsTarget dmsTarget, @NotNull String incomingNamePrefix);

    /**
     * Create ContentObject inside Incoming.
     *
     * @param dmsTarget The Incoming to create the ContentObject in.
     * @param contentObjectRequest The values for the new ContentObject.
     * @param inputStream The content of the new ContentObject.
     * @return The coo of the new ContentObject.
     */
    String createContentObject(@NotNull @Valid DmsTarget dmsTarget, @NotNull @Valid DmsContentObjectRequest contentObjectRequest,
            @NotNull InputStream inputStream);

    /**
     * Find dms object via name and resource type.
     *
     * @param resourceType The type of the dms resource to search for.
     * @param objectName The name of the object to search for.
     * @param requestContext The context (username, joboe, jobposition) to make the search request with.
     * @return The COOs of all matching objects.
     */
    List<String> findObjectsByName(@NotNull DmsResourceType resourceType, @NotNull String objectName, @NotNull @Valid DmsRequestContext requestContext);
}
