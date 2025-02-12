package de.muenchen.oss.swim.dispatcher.application.port.out;

import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public interface StoreProtocolOutPort {
    /**
     * Store protocol entries.
     *
     * @param useCase The name of the use case the protocol was found for.
     * @param protocolName The name of the protocol.
     * @param entries The entries of the protocol.
     */
    void storeProtocol(@NotBlank String useCase, @NotBlank String protocolName, @Valid List<ProtocolEntry> entries);

    /**
     * Delete entries for existing protocol.
     *
     * @param useCase The use case name of the protocol.
     * @param protocolName The name of the protocol.
     */
    void deleteProtocol(@NotBlank String useCase, @NotBlank String protocolName);
}
