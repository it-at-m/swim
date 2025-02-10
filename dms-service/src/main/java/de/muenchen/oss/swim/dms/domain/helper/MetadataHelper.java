package de.muenchen.oss.swim.dms.domain.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.MetadataException;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetadataHelper {
    public static final String METADATA_VALUE_KEY = "Value";
    public static final String METADATA_KEY_KEY = "Name";
    public static final String METADATA_DOCUMENT_KEY = "Document";
    public static final String METADATA_INDEX_FIELDS_KEY = "IndexFields";

    private final SwimDmsProperties swimDmsProperties;
    private final ObjectMapper objectMapper;

    /**
     * Parse metadata file to JsonNode.
     *
     * @param inputStream The content of the metadata file.
     * @return The parsed metadata.
     * @throws MetadataException If the file can't be parsed.
     */
    public JsonNode parseMetadataFile(@NotNull final InputStream inputStream) throws MetadataException {
        try {
            return objectMapper.readTree(inputStream);
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
    public Map<String, String> getIndexFields(@NotNull final JsonNode rootNode) throws MetadataException {
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
            indexFields.put(key, value);
        }
        return indexFields;
    }

    /**
     * Extract dms target from metadata file.
     *
     * @param rootNode Parsed JsonNode of metadata file.
     * @return The dms target.
     * @throws MetadataException If required values are missing.
     */
    public DmsTarget resolveDmsTarget(@NotNull final JsonNode rootNode) throws MetadataException {
        final Map<String, String> indexFields = this.getIndexFields(rootNode);
        String userInboxCoo = indexFields.get(swimDmsProperties.getMetadataUserInboxCooKey());
        String userInboxOwner = indexFields.get(swimDmsProperties.getMetadataUserInboxUserKey());
        String groupInboxCoo = indexFields.get(swimDmsProperties.getMetadataGroupInboxCooKey());
        String groupInboxOwner = indexFields.get(swimDmsProperties.getMetadataGroupInboxUserKey());
        // check combination of data is allowed and build DmsTarget
        return this.dmsTargetFromUserAndGroupInbox(userInboxCoo, userInboxOwner, groupInboxCoo, groupInboxOwner);
    }

    /**
     * Resolve correct DmsTarget from user and group inbox values.
     *
     * @param userInboxCoo The value for the user inbox coo.
     * @param userInboxOwner The value for the user inbox owner.
     * @param groupInboxCoo The value for the group inbox coo.
     * @param groupInboxOwner The value for the group inbox owner.
     * @return The resolved DmsTarget coo and owner combination.
     * @throws MetadataException If the combination of user and group values isn't valid.
     */
    protected DmsTarget dmsTargetFromUserAndGroupInbox(final String userInboxCoo, final String userInboxOwner, final String groupInboxCoo,
            final String groupInboxOwner) throws MetadataException {
        // check if user and group metadata provided
        final boolean hasUserValue = Strings.isNotBlank(userInboxCoo) || Strings.isNotBlank(userInboxOwner);
        final boolean hasGroupValue = Strings.isNotBlank(groupInboxCoo) || Strings.isNotBlank(groupInboxOwner);
        if (hasUserValue && hasGroupValue) {
            throw new MetadataException("User and group inbox metadata provided");
        }
        // user inbox
        if (Strings.isNotBlank(userInboxCoo) && Strings.isNotBlank(userInboxOwner)) {
            return new DmsTarget(userInboxCoo, userInboxOwner, null, null);
        }
        // group inbox
        if (Strings.isNotBlank(groupInboxCoo) && Strings.isNotBlank(groupInboxOwner)) {
            return new DmsTarget(groupInboxCoo, groupInboxOwner, null, null);
        }
        throw new MetadataException("Neither user nor group inbox metadata found");
    }
}
