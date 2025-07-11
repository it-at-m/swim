package de.muenchen.oss.swim.dms.application.usecase.helper;

import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.helper.DmsMetadataHelper;
import de.muenchen.oss.swim.dms.domain.model.DmsResourceType;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.dms.domain.model.UseCaseType;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TargetResolverHelper {
    private final SwimDmsProperties swimDmsProperties;
    private final DmsOutPort dmsOutPort;
    private final PatternHelper patternHelper;
    private final DmsMetadataHelper dmsMetadataHelper;

    /**
     * Resolve UseCase type. Either directly configured or via metadata file.
     *
     * @param useCase The UseCase to resolve the type for.
     * @param metadata Parsed metadata file.
     * @return The resolved UseCaseType.
     * @throws MetadataException If metadata can't be parsed or has illegal values.
     */
    public UseCaseType resolveUseCaseType(final UseCase useCase, final Metadata metadata) throws MetadataException {
        final UseCaseType targetResource;
        if (useCase.getType() == UseCaseType.METADATA_FILE) {
            targetResource = this.resolveTypeFromMetadataFile(metadata);
        } else {
            targetResource = useCase.getType();
        }
        return targetResource;
    }

    /**
     * Resolve target coo for useCase.
     * {@link UseCaseType}
     *
     * @param resourceType Target type the coo is resolved for.
     * @param metadata Parsed metadata file.
     * @param useCase The use case.
     * @param file The file to resolve the coo for.
     * @return The resolved coo.
     */
    public DmsTarget resolveTargetCoo(final UseCaseType resourceType, final Metadata metadata, final UseCase useCase, final File file)
            throws MetadataException {
        return switch (useCase.getCooSource().getType()) {
        case METADATA_FILE -> this.resolveMetadataTargetCoo(resourceType, metadata, useCase);
        case FILENAME -> {
            if (StringUtils.isBlank(useCase.getCooSource().getFilenameCooPattern())) {
                throw new IllegalArgumentException("Filename coo pattern is required");
            }
            final String targetCoo = this.patternHelper.applyPattern(useCase.getCooSource().getFilenameCooPattern(), file.getFileName(), metadata);
            yield new DmsTarget(targetCoo, useCase.getContext());
        }
        case FILENAME_MAP -> {
            // find first matching target coo from map
            final String targetCoo = useCase.getCooSource().getFilenameToCoo().entrySet().stream()
                    .filter(i -> Pattern.compile(
                            i.getKey(),
                            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                            .matcher(file.getFileName()).find())
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElseThrow(() -> new IllegalStateException("No matching filename map entry configured."));
            yield new DmsTarget(targetCoo, useCase.getContext());
        }
        case FILENAME_NAME -> this.resolveTargetCooViaName(useCase.getType().getTarget(), metadata, useCase, file);
        case STATIC -> new DmsTarget(useCase.getCooSource().getTargetCoo(), useCase.getContext());
        case OU_WORK_QUEUE -> new DmsTarget(null, useCase.getContext());
        };
    }

    /**
     * Resolve DmsTarget via metadata file.
     *
     * @param resourceType The type of the use case used for resolving.
     * @param metadata Parsed metadata file.
     * @param useCase UseCase of the file.
     * @return Resolved DmsTarget.
     */
    protected DmsTarget resolveMetadataTargetCoo(final UseCaseType resourceType, final Metadata metadata, final UseCase useCase) throws MetadataException {
        // validate metadata provided
        if (metadata == null) {
            throw new MetadataException("Target coo via metadata file: Metadata is required");
        }
        // extract coo and username from metadata
        final DmsTarget metadataTarget = switch (resourceType) {
        case INBOX_CONTENT_OBJECT, INBOX_INCOMING -> dmsMetadataHelper.resolveInboxDmsTarget(metadata);
        case PROCEDURE_INCOMING -> dmsMetadataHelper.resolveIncomingDmsTarget(metadata);
        case METADATA_FILE -> throw new IllegalStateException("Target type metadata needs to be resolved to other types");
        };
        // combine resolves target with use case
        return this.combineDmsTargetWithUseCase(metadataTarget, useCase);
    }

    /**
     * Resolve target coo via dms object name.
     *
     * @param resourceType The resource type of the target.
     * @param metadata The metadata used for resolving the name pattern.
     * @param useCase The use case to resolve the target for.
     * @param file The file to resolve the target for.
     * @return Dms target resolved via dms object name.
     */
    protected DmsTarget resolveTargetCooViaName(final DmsResourceType resourceType, final Metadata metadata, final UseCase useCase, final File file) {
        // validate required use case properties
        if (StringUtils.isBlank(useCase.getCooSource().getFilenameNamePattern())) {
            throw new IllegalArgumentException("DMS target coo via object name: Filename name pattern is required");
        }
        if (StringUtils.isBlank(useCase.getContext().getUsername())) {
            throw new IllegalStateException("DMS target coo via object name: Username is required");
        }
        // resolve lookup name
        final String objectName = this.patternHelper.applyPattern(useCase.getCooSource().getFilenameNamePattern(), file.getFileName(), metadata);
        // search for object name
        final DmsTarget requestContext = new DmsTarget(null, useCase.getContext());
        final List<String> coos = this.dmsOutPort.findObjectsByName(resourceType, objectName, requestContext);
        if (coos.size() != 1) {
            throw new IllegalStateException(
                    String.format("DMS target coo via object name: Found %d instead of exactly one object for pattern %s", coos.size(), objectName));
        }
        return new DmsTarget(coos.getFirst(), useCase.getContext());
    }

    /**
     * Combine resolved DmsTarget with UseCase values.
     * Uses resolved over UseCase if present.
     *
     * @param dmsTarget Resolved target.
     * @param useCase UseCase the target was resolved for.
     * @return Combined DmsTarget.
     * @throws IllegalStateException If username is neither defined via DmsTarget nor UseCase.
     */
    protected DmsTarget combineDmsTargetWithUseCase(final DmsTarget dmsTarget, final UseCase useCase) {
        // username
        final String username = StringUtils.isNotBlank(dmsTarget.getUsername()) ? dmsTarget.getUsername() : useCase.getContext().getUsername();
        if (StringUtils.isBlank(username)) {
            throw new IllegalStateException("Resolve dms target: Username neither defined via target nor use case but is required");
        }
        // joboe and jobposition
        final String joboe = StringUtils.isNotBlank(dmsTarget.getJoboe()) ? dmsTarget.getJoboe() : useCase.getContext().getJoboe();
        final String jobposition = StringUtils.isNotBlank(dmsTarget.getJobposition()) ? dmsTarget.getJobposition() : useCase.getContext().getJobposition();
        // return merged
        return new DmsTarget(dmsTarget.getCoo(), username, joboe, jobposition);
    }

    /**
     * Resolve dms target resource type from metadata file.
     *
     * @param metadata Parsed metadata file.
     * @return The resolved type.
     * @throws MetadataException If metadata can't be parsed or has illegal values.
     */
    public UseCaseType resolveTypeFromMetadataFile(final Metadata metadata) throws MetadataException {
        // validate metadata provided
        if (metadata == null) {
            throw new MetadataException("DMS target type via metadata file: Metadata is required");
        }
        // load value from metadata file
        final Map<String, String> indexFields = metadata.indexFields();
        final String metadataDmsTarget = indexFields.get(swimDmsProperties.getMetadataDmsTargetKey());
        // resolve type from value
        try {
            final UseCaseType resolvedType = UseCaseType.valueOf(metadataDmsTarget.toUpperCase(Locale.ROOT));
            if (resolvedType == UseCaseType.METADATA_FILE) {
                throw new MetadataException("DMS target type via metadata file: Target type can't be METADATA_FILE");
            }
            return resolvedType;
        } catch (final IllegalArgumentException e) {
            throw new MetadataException(
                    String.format("DMS target type via metadata file: Unexpected %s value: %s", swimDmsProperties.getMetadataDmsTargetKey(), metadataDmsTarget),
                    e);
        }
    }
}
