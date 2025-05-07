package de.muenchen.oss.swim.dipa.domain.model.dipa;

import de.muenchen.oss.swim.dipa.domain.model.DipaRequestContext;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Context for identifying a HrSubfile.
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class HrSubfileContext extends DipaRequestContext {
    /**
     * PersNr to identify a HrSubfile.
     */
    @NotBlank
    private final String persNr;
    /**
     * Category to identify a HrSubfile.
     */
    @NotBlank
    private final String category;

    public HrSubfileContext(final DipaRequestContext context, final String persNr, final String category) {
        super(context.getUsername());
        this.persNr = persNr;
        this.category = category;
    }
}
