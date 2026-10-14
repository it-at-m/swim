package de.muenchen.oss.swim.libs.handlercore.application.port.out;

import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
public interface FileEventOutPort {
    /**
     * Notify dispatcher that file(s) processing was finished successfully.
     *
     * @param event The event of the file(s) to finish.
     */
    void fileFinished(@Valid FileEvent event);
}
