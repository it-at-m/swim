package de.muenchen.oss.swim.dispatcher.application.port.out;

import de.muenchen.oss.swim.dispatcher.domain.model.FileReference;
import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ReadProtocolOutPort {
    /**
     * Load parsed protocol.
     *
     * @param fileReference The reference to the protocol file to load.
     * @return The parsed protocol entries.
     */
    List<ProtocolEntry> loadProtocol(@NotNull @Validated FileReference fileReference);
}
