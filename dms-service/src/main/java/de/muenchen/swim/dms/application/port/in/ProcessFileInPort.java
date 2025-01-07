package de.muenchen.swim.dms.application.port.in;

import de.muenchen.swim.dms.domain.model.File;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ProcessFileInPort {
    void processFile(@NotBlank String useCase, @Valid File file, @NotBlank String presignedUrl, String metadataPresignedUrl);
}
