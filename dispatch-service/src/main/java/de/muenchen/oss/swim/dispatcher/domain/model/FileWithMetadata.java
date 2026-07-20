package de.muenchen.oss.swim.dispatcher.domain.model;

import java.time.ZonedDateTime;
import java.util.Map;

public record FileWithMetadata(
        FileReference reference,
        long size,
        ZonedDateTime lastModified,
        Map<String, String> tags) {
}
