package de.muenchen.oss.swim.dipa.domain.model.dipa;

import de.muenchen.oss.swim.dipa.domain.model.DipaRequestContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Context for identifying a HrSubfile.
 *
 * @param requestContext Context to make the request under.
 * @param persNr PersNr to identify a HrSubfile.
 * @param category Category to identify a HrSubfile.
 */
public record HrSubfileContext(
        @NotNull @Valid DipaRequestContext requestContext,
        @NotBlank String persNr,
        @NotBlank String category) {
}
