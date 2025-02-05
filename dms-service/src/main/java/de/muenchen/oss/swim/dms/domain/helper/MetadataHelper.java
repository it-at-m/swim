package de.muenchen.oss.swim.dms.domain.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.MetadataException;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
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
     * Extract dms target from metadata file.
     *
     * @param inputStream InputStream of metadata file.
     * @return The dms target.
     * @throws MetadataException If file can't be parsed or required values are missing.
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public DmsTarget resolveDmsTarget(@NotNull final InputStream inputStream) throws MetadataException {
        try {
            final JsonNode rootNode = objectMapper.readTree(inputStream);
            final JsonNode documentNode = rootNode.get(METADATA_DOCUMENT_KEY);
            if (documentNode == null) {
                throw new MetadataException("Missing '" + METADATA_DOCUMENT_KEY + "' in metadata JSON");
            }
            final JsonNode indexFieldsNode = documentNode.get(METADATA_INDEX_FIELDS_KEY);
            if (indexFieldsNode == null || !indexFieldsNode.isArray()) {
                throw new MetadataException("Missing or invalid '" + METADATA_INDEX_FIELDS_KEY + "' in metadata JSON");
            }
            String userInboxCoo = null;
            String userInboxOwner = null;
            String groupInboxCoo = null;
            String groupInboxOwner = null;
            for (final JsonNode indexField : indexFieldsNode) {
                // user inbox coo
                if (swimDmsProperties.getMetadataUserInboxCooKey().equals(indexField.path(METADATA_KEY_KEY).asText())) {
                    userInboxCoo = indexField.path(METADATA_VALUE_KEY).asText();
                }
                // user inbox owner username
                else if (swimDmsProperties.getMetadataUserInboxUserKey().equals(indexField.path(METADATA_KEY_KEY).asText())) {
                    userInboxOwner = indexField.path(METADATA_VALUE_KEY).asText();
                }
                // group inbox coo
                else if (swimDmsProperties.getMetadataGroupInboxCooKey().equals(indexField.path(METADATA_KEY_KEY).asText())) {
                    groupInboxCoo = indexField.path(METADATA_VALUE_KEY).asText();
                }
                // group inbox owner username
                else if (swimDmsProperties.getMetadataGroupInboxUserKey().equals(indexField.path(METADATA_KEY_KEY).asText())) {
                    groupInboxOwner = indexField.path(METADATA_VALUE_KEY).asText();
                }
            }
            // check combination of data is allowed and build DmsTarget
            return this.dmsTargetFromUserAndGroupInbox(userInboxCoo, userInboxOwner, groupInboxCoo, groupInboxOwner);
        } catch (final IOException e) {
            throw new MetadataException("Error while parsing metadata json", e);
        }
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
