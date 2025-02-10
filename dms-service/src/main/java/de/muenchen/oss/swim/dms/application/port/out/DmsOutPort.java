package de.muenchen.oss.swim.dms.application.port.out;

import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;

@Validated
public interface DmsOutPort {
    /**
     * Create ContentObject inside an Inbox.
     *
     * @param dmsTarget The target Inbox.
     * @param contentObjectName The name of the new ContentObject.
     * @param inputStream The content of the new ContentObject.
     */
    void createContentObjectInInbox(@NotNull @Valid DmsTarget dmsTarget, @NotBlank String contentObjectName, @NotNull InputStream inputStream);

    /**
     * Create Incoming.
     * Either inside given Procedure {@link DmsTarget#coo()} or OU work queue of
     * {@link DmsTarget#userName()}.
     *
     * @param dmsTarget The target. If {@link DmsTarget#coo()} is defined: Procedure, if not: OU work
     *            queue.
     * @param incomingName The name of the new Incoming.
     * @param contentObjectName The name of the ContentObject inside the Incoming.
     * @param inputStream The content of the new ContentObject.
     * @return The coo of the new Incoming.
     */
    String createIncoming(@NotNull @Valid DmsTarget dmsTarget, @NotBlank String incomingName, @NotBlank String contentObjectName,
            @NotNull InputStream inputStream);

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
     * @param contentObjectName The name of the new ContentObject.
     * @param inputStream The content of the new ContentObject.
     * @return The coo of the new ContentObject.
     */
    String createContentObject(@NotNull @Valid DmsTarget dmsTarget, @NotNull String contentObjectName, @NotNull InputStream inputStream);
}
