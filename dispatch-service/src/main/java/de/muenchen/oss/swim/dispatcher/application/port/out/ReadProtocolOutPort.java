package de.muenchen.oss.swim.dispatcher.application.port.out;

import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ReadProtocolOutPort {
    /**
     * Load parsed protocol.
     *
     * @param bucket The bucket of the protocol file.
     * @param path The path of the protocol file.
     * @return The parsed protocol entries.
     */
    List<ProtocolEntry> loadProtocol(@NotBlank String tenant, @NotBlank String bucket, @NotBlank String path);
}
