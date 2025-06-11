package de.muenchen.oss.swim.dms.domain.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.MetadataHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DmsMetadataHelper extends MetadataHelper {
    private final SwimDmsProperties swimDmsProperties;

    public DmsMetadataHelper(@Autowired final SwimDmsProperties swimDmsProperties, @Autowired final ObjectMapper objectMapper) {
        super(objectMapper);
        this.swimDmsProperties = swimDmsProperties;
    }

    /**
     * Extract inbox dms target from metadata file.
     *
     * @param metadata Parsed metadata file.
     * @return The dms target.
     * @throws MetadataException If required values are missing.
     */
    public DmsTarget resolveInboxDmsTarget(@NotNull final Metadata metadata) throws MetadataException {
        final Map<String, String> indexFields = metadata.indexFields();
        final String userInboxCoo = indexFields.get(swimDmsProperties.getMetadataUserInboxCooKey());
        final String userInboxOwner = indexFields.get(swimDmsProperties.getMetadataUserInboxUserKey());
        final String groupInboxCoo = indexFields.get(swimDmsProperties.getMetadataGroupInboxCooKey());
        final String groupInboxOwner = indexFields.get(swimDmsProperties.getMetadataGroupInboxUserKey());
        // check combination of data is allowed and build DmsTarget
        return this.dmsTargetFromUserAndGroupInbox(userInboxCoo, userInboxOwner, groupInboxCoo, groupInboxOwner);
    }

    /**
     * Extract incoming or ou work queue dms target from metadata file.
     * For incoming {@link DmsTarget#getCoo()} is set and for ou work queue empty.
     *
     * @param metadata Parsed metadata file.
     * @return The incoming or ou work queue target.
     */
    public DmsTarget resolveIncomingDmsTarget(@NotNull final Metadata metadata) {
        final Map<String, String> indexFields = metadata.indexFields();
        final String incomingCoo = indexFields.get(swimDmsProperties.getMetadataIncomingCooKey());
        final String incomingOwner = indexFields.get(swimDmsProperties.getMetadataIncomingUserKey());
        final String incomingJoboe = indexFields.get(swimDmsProperties.getMetadataIncomingJoboeKey());
        final String incomingJobposition = indexFields.get(swimDmsProperties.getMetadataIncomingJobpositionKey());
        return new DmsTarget(StringUtils.isNotBlank(incomingCoo) ? incomingCoo : null, incomingOwner, incomingJoboe, incomingJobposition);
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
        final boolean hasUserValue = StringUtils.isNotBlank(userInboxCoo) || StringUtils.isNotBlank(userInboxOwner);
        final boolean hasGroupValue = StringUtils.isNotBlank(groupInboxCoo) || StringUtils.isNotBlank(groupInboxOwner);
        if (hasUserValue && hasGroupValue) {
            throw new MetadataException("User and group inbox metadata provided");
        }
        // user inbox
        if (StringUtils.isNotBlank(userInboxCoo) && StringUtils.isNotBlank(userInboxOwner)) {
            return new DmsTarget(userInboxCoo, userInboxOwner, null, null);
        }
        // group inbox
        if (StringUtils.isNotBlank(groupInboxCoo) && StringUtils.isNotBlank(groupInboxOwner)) {
            return new DmsTarget(groupInboxCoo, groupInboxOwner, null, null);
        }
        throw new MetadataException("Neither user nor group inbox metadata found");
    }
}
