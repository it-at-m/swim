package de.muenchen.oss.swim.dispatcher.domain.model.protocol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ProtocolEntry(
        @NotBlank String fileName,
        @NotNull Integer pageCount,
        String department,
        String boxId,
        String paginationId,
        String documentType,
        String cooAddress,
        Map<String, Object> additionalProperties) {
}
