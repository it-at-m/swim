package de.muenchen.oss.swim.dispatcher.domain.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dispatcher.domain.exception.MetadataException;
import de.muenchen.oss.swim.dispatcher.domain.model.Metadata;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetadataHelper {
    public static final String METADATA_VALUE_KEY = "Value";
    public static final String METADATA_KEY_KEY = "Name";
    public static final String METADATA_DOCUMENT_KEY = "Document";
    public static final String METADATA_INDEX_FIELDS_KEY = "IndexFields";

    private final ObjectMapper objectMapper;

    /**
     * Parse metadata file.
     *
     * @param inputStream The content of the metadata file.
     * @return The parsed metadata.
     * @throws MetadataException If the file can't be parsed.
     */
    public Metadata parseMetadataFile(@NotNull final InputStream inputStream) throws MetadataException {
        try {
            final JsonNode jsonNode = objectMapper.readTree(inputStream);
            return new Metadata(jsonNode, this.getIndexFields(jsonNode));
        } catch (final IOException e) {
            throw new MetadataException("Error while parsing metadata json", e);
        }
    }

    /**
     * Extract IndexFields as Map from metadata JSON.
     *
     * @param rootNode The parsed metadata file.
     * @return The IndexFields as Map.
     * @throws MetadataException If fields are missing.
     */
    protected Map<String, String> getIndexFields(@NotNull final JsonNode rootNode) throws MetadataException {
        final JsonNode documentNode = rootNode.get(METADATA_DOCUMENT_KEY);
        if (documentNode == null) {
            throw new MetadataException("Missing '" + METADATA_DOCUMENT_KEY + "' in metadata JSON");
        }
        final JsonNode indexFieldsNode = documentNode.get(METADATA_INDEX_FIELDS_KEY);
        if (indexFieldsNode == null || !indexFieldsNode.isArray()) {
            throw new MetadataException("Missing or invalid '" + METADATA_INDEX_FIELDS_KEY + "' in metadata JSON");
        }
        final Map<String, String> indexFields = new HashMap<>();
        for (final JsonNode indexField : indexFieldsNode) {
            final String key = indexField.path(METADATA_KEY_KEY).asText();
            final String value = indexField.path(METADATA_VALUE_KEY).asText();
            if (!key.isEmpty()) {
                indexFields.put(key, value);
            }
        }
        return indexFields;
    }
}
