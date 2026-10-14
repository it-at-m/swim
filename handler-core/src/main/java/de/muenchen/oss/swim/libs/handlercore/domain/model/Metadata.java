package de.muenchen.oss.swim.libs.handlercore.domain.model;

import java.util.Map;
import tools.jackson.databind.JsonNode;

public record Metadata(
        JsonNode jsonNode,
        Map<String, String> indexFields) {
}
