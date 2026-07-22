package de.muenchen.oss.swim.dms.domain.helper;

import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.MetadataHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class DmsMetadataHelper extends MetadataHelper {
    private final SwimDmsProperties swimDmsProperties;

    public DmsMetadataHelper(final SwimDmsProperties swimDmsProperties, final JsonMapper jsonMapper) {
        super(jsonMapper);
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
        final DmsTarget userInboxDmsTarget = this.dmsTargetFromMetadataKeys(swimDmsProperties.getMetadataUserInbox(), metadata);
        final DmsTarget groupInboxDmsTarget = this.dmsTargetFromMetadataKeys(swimDmsProperties.getMetadataGroupInbox(), metadata);
        // check combination of data is allowed and build DmsTarget
        return this.dmsTargetFromUserAndGroupInbox(userInboxDmsTarget, groupInboxDmsTarget);
    }

    /**
     * Extract incoming or ou work queue dms target from metadata file.
     * For incoming {@link DmsTarget#getCoo()} is set and for ou work queue empty.
     *
     * @param metadata Parsed metadata file.
     * @return The incoming or ou work queue target.
     */
    public DmsTarget resolveIncomingDmsTarget(@NotNull final Metadata metadata) {
        final SwimDmsProperties.MetadataRequestContextProperty metadataKeys = swimDmsProperties.getMetadataIncoming();
        return this.dmsTargetFromMetadataKeys(metadataKeys, metadata);
    }

    /**
     * Extract shadow file dms target from metadata file.
     *
     * @param metadata Parsed metadata file.
     * @return The dms target.
     */
    public DmsTarget resolveShadowFileDmsTarget(@NotNull final Metadata metadata) throws MetadataException {
        final SwimDmsProperties.MetadataRequestContextProperty metadataKeys = swimDmsProperties.getMetadataShadowFile();
        final DmsTarget dmsTarget = this.dmsTargetFromMetadataKeys(metadataKeys, metadata);
        if (StringUtils.isBlank(dmsTarget.getCoo())) {
            throw new MetadataException("%s must be set for shadow file".formatted(metadataKeys.cooKey()));
        }
        return dmsTarget;
    }

    /**
     * Extract dms target from metadata with given keys.
     *
     * @param metadataKeys The keys to use for extraction.
     * @param metadata Parsed metadata file.
     * @return The resolved dms target.
     */
    protected DmsTarget dmsTargetFromMetadataKeys(@NotNull final SwimDmsProperties.MetadataRequestContextProperty metadataKeys,
            @NotNull final Metadata metadata) {
        final Map<String, String> indexFields = metadata.indexFields();
        final String coo = indexFields.get(metadataKeys.cooKey());
        final String owner = indexFields.get(metadataKeys.userKey());
        final String jobOe = indexFields.get(metadataKeys.jobOeKey());
        final String jobPosition = indexFields.get(metadataKeys.jobPositionKey());
        return new DmsTarget(StringUtils.isNotBlank(coo) ? coo : null, owner, jobOe, jobPosition);
    }

    /**
     * Resolve correct DmsTarget from user and group inbox values.
     *
     * @param userInboxDmsTarget The values for the user inbox.
     * @param groupInboxDmsTarget The values for the group inbox.
     * @return The resolved DmsTarget coo and owner combination.
     * @throws MetadataException If the combination of user and group values isn't valid.
     */
    protected DmsTarget dmsTargetFromUserAndGroupInbox(final DmsTarget userInboxDmsTarget, final DmsTarget groupInboxDmsTarget) throws MetadataException {
        // check if user and group metadata provided
        if (userInboxDmsTarget.hasValues() && groupInboxDmsTarget.hasValues()) {
            throw new MetadataException("User and group inbox metadata provided");
        }
        // user inbox
        if (StringUtils.isNotBlank(userInboxDmsTarget.getCoo()) && StringUtils.isNotBlank(userInboxDmsTarget.getUsername())) {
            return userInboxDmsTarget;
        }
        // group inbox
        if (StringUtils.isNotBlank(groupInboxDmsTarget.getCoo()) && StringUtils.isNotBlank(groupInboxDmsTarget.getUsername())) {
            return groupInboxDmsTarget;
        }
        throw new MetadataException("Neither user nor group inbox metadata found");
    }
}
