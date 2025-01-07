package de.muenchen.swim.dms.application.port.out;

import de.muenchen.swim.dms.domain.model.DmsTarget;
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
}
