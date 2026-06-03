package de.muenchen.oss.swim.dispatcher.application.port.out;

import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface FileDispatchingOutPort {
    /**
     * Dispatch multiple files for further joint processing.
     *
     * @param bindingName The name to send the notification to.
     * @param useCase The name of the use case the file was found for.
     * @param presignedFiles The presigned information of multiple files.
     */
    void dispatchFile(@NotBlank String bindingName, @NotBlank String useCase, @NotEmpty @Valid List<PresignedFile> presignedFiles);
}
