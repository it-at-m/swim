package de.muenchen.oss.swim.dipa.domain.model.dipa;

import de.muenchen.oss.swim.dipa.domain.model.DipaRequestContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HrSubfileContext(
        @NotNull @Valid DipaRequestContext requestContext,
        @NotBlank String persNr,
        @NotBlank String category) {
}
