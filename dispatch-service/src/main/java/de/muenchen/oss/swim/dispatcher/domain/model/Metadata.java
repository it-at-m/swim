package de.muenchen.oss.swim.dispatcher.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public record Metadata(
        JsonNode jsonNode,
        Map<String, String> indexFields) {
}
