package de.muenchen.swim.dispatcher.domain.model.protocol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ProtocolEntry(
        @NotBlank String fileName,
        @NotNull int pageCount,
        String paginationId,
        String documentType,
        String cooAddress,
        Map<String, Object> additionalProperties) {
}
