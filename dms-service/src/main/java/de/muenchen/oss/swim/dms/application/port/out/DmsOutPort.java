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
     * Create Incoming inside a Procedure.
     *
     * @param dmsTarget The target Procedure.
     * @param incomingName The name of the new Incoming.
     * @param fileName The name of the ContentObject inside the Incoming.
     * @param inputStream The content of the new ContentObject.
     * @return The coo of the new Incoming.
     */
    String createIncoming(@NotNull @Valid DmsTarget dmsTarget, @NotBlank String incomingName, @NotBlank String fileName, @NotNull InputStream inputStream);
}
