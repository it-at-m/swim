package de.muenchen.oss.swim.dms.application.usecase.helper;

import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.domain.helper.DmsMetadataHelper;
import de.muenchen.oss.swim.dms.domain.helper.PatternHelper;
import de.muenchen.oss.swim.dms.domain.model.DmsResourceType;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.dms.domain.model.UseCaseType;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TargetResolverHelper {
    private final DmsOutPort dmsOutPort;
    private final PatternHelper patternHelper;
    private final DmsMetadataHelper dmsMetadataHelper;

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
                if (Strings.isBlank(useCase.getCooSource().getFilenameCooPattern())) {
                    throw new IllegalArgumentException("Filename coo pattern is required");
                }
                final String targetCoo = this.patternHelper.applyPattern(useCase.getCooSource().getFilenameCooPattern(), file.getFileName(), metadata);
                yield new DmsTarget(targetCoo, useCase.getContext());
            }
            case FILENAME_MAP -> {
                // find first matching target coo from map
                final String targetCoo = useCase.getCooSource().getFilenameToCoo().entrySet().stream()
                        .filter(i -> Pattern.compile(i.getKey(), Pattern.CASE_INSENSITIVE).matcher(file.getFileName()).find())
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
        final DmsTarget metadataTarget = switch (resourceType.getTarget()) {
            case INBOX -> dmsMetadataHelper.resolveInboxDmsTarget(metadata);
            case INCOMING -> dmsMetadataHelper.resolveIncomingDmsTarget(metadata);
            default -> throw new IllegalStateException(String.format("Target type %s can't be resolved via metadata file", resourceType.getTarget()));
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
        if (Strings.isBlank(useCase.getCooSource().getFilenameNamePattern())) {
            throw new IllegalArgumentException("DMS target coo via object name: Filename name pattern is required");
        }
        if (Strings.isBlank(useCase.getContext().getUsername())) {
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
     * Checks that only one of both defines job oe and position values.
     *
     * @param dmsTarget Resolved target.
     * @param useCase UseCase the target was resolved for.
     * @return Combined DmsTarget.
     * @throws IllegalStateException If both inputs define job oe or position.
     */
    protected DmsTarget combineDmsTargetWithUseCase(final DmsTarget dmsTarget, final UseCase useCase) {
        final boolean dmsTargetHasJob = Strings.isNotBlank(dmsTarget.getJoboe()) || Strings.isNotBlank(dmsTarget.getJobposition());
        final boolean useCaseHasJob = Strings.isNotBlank(useCase.getContext().getJoboe()) || Strings.isNotBlank(useCase.getContext().getJobposition());
        if (dmsTargetHasJob && useCaseHasJob) {
            throw new IllegalStateException("Resolve dms target: Job oe and position defined via resolve and via use case not allowed");
        }
        if (dmsTargetHasJob) {
            return dmsTarget;
        }
        return new DmsTarget(dmsTarget.getCoo(), dmsTarget.getUsername(), useCase.getContext().getJoboe(), useCase.getContext().getJobposition());
    }
}
