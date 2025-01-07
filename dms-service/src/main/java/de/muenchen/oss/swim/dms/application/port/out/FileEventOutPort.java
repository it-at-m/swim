package de.muenchen.oss.swim.dms.application.port.out;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public interface FileEventOutPort {
    void fileFinished(@NotBlank String useCase, @NotBlank String presignedUrl, String metadataPresignedUrl);
}
