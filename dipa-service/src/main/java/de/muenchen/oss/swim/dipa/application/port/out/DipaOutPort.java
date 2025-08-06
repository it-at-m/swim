package de.muenchen.oss.swim.dipa.application.port.out;

import de.muenchen.oss.swim.dipa.domain.model.dipa.HrSubfileContext;
import de.muenchen.oss.swim.dipa.domain.model.dipa.IncomingRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface DipaOutPort {
    /**
     * Create Incoming with ContentObject inside existing HrSubfile.
     *
     * @param context Context to make the request under. Defined target HrSubfile.
     * @param incomingRequest Properties of the new Incoming.
     * @return The ID of the created Incoming.
     */
    String createHrSubfileIncoming(
            @NotNull @Valid HrSubfileContext context,
            @NotNull @Valid IncomingRequest incomingRequest);
}
