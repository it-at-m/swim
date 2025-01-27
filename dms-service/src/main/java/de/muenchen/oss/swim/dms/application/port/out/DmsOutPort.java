package de.muenchen.oss.swim.dms.application.port.out;

import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import org.springframework.validation.annotation.Validated;

@Validated
public interface DmsOutPort {
    /**
     * Create ContentObject inside an Inbox.
     *
     * @param dmsTarget The target Inbox.
     * @param fileName The name of the new ContentObject.
     * @param inputStream The content of the new ContentObject.
     */
    void putFileInInbox(@NotNull @Valid DmsTarget dmsTarget, @NotBlank String fileName, @NotNull InputStream inputStream);

    /**
     * Create Incoming.
     * Either inside given Procedure {@link DmsTarget#coo} or default Procedure of OU
     * {@link DmsTarget#joboe}.
     *
     * @param dmsTarget The target Procedure.
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
     * Get the coo of an Incoming by name.
     *
     * @param dmsTarget The Procedure to search in.
     * @param procedureName The name of the Incoming.
     * @return The coo of the procedure. Null if it doesn't exist.
     */
    String getIncomingCooByName(@NotNull @Valid DmsTarget dmsTarget, @NotNull String procedureName);

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
