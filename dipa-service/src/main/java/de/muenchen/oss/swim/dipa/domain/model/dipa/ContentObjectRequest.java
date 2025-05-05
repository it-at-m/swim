package de.muenchen.oss.swim.dipa.domain.model.dipa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;

public record ContentObjectRequest(
        @NotBlank String name,
        @NotNull InputStream content
) {
}
