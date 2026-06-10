package de.muenchen.oss.swim.dispatcher.domain.model;

import java.util.Map;

public record FileWithMetadata(
        FileReference reference,
        long size,
        Map<String, String> tags) {
}
