package de.muenchen.oss.swim.dms.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.configuration.DmsMeter;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.helper.DmsMetadataHelper;
import de.muenchen.oss.swim.dms.domain.helper.PatternHelper;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.libs.handlercore.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.libs.handlercore.domain.model.File;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase implements ProcessFileInPort {
    protected static final String METADATA_TARGET_TYPE_INBOX = "inbox";
    protected static final String METADATA_TARGET_TYPE_INCOMING = "incoming";

    private final SwimDmsProperties swimDmsProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final DmsOutPort dmsOutPort;
    private final FileEventOutPort fileEventOutPort;
    private final DmsMetadataHelper dmsMetadataHelper;
    private final PatternHelper patternHelper;
    private final DmsMeter dmsMeter;

    @Override
    public void processFile(final FileEvent event, final File file)
            throws PresignedUrlException, UnknownUseCaseException, MetadataException {
        log.info("Processing file {} for use case {}", file, event.useCase());
        final UseCase useCase = swimDmsProperties.findUseCase(event.useCase());
        log.debug("Resolved use case: {}", useCase);
        // load file
        try (InputStream fileStream = fileSystemOutPort.getPresignedUrlFile(event.presignedUrl())) {
            // parse metadata file if present
            JsonNode metadataJson = null;
            if (Strings.isNotBlank(event.metadataPresignedUrl())) {
                try (InputStream metadataFileStream = fileSystemOutPort.getPresignedUrlFile(event.metadataPresignedUrl())) {
                    metadataJson = dmsMetadataHelper.parseMetadataFile(metadataFileStream);
                }
            }
            // resolve target resource type
            final UseCase.Type targetResource;
            if (useCase.getType() == UseCase.Type.METADATA_FILE) {
                targetResource = this.resolveTypeFromMetadataFile(metadataJson);
            } else {
                targetResource = useCase.getType();
            }
            // get target coo
            final DmsTarget dmsTarget = this.resolveTargetCoo(targetResource, metadataJson, useCase, file);
            log.debug("Resolved dms target: {}", dmsTarget);
            // get ContentObject name
            final String contentObjectName = this.patternHelper.applyPattern(useCase.getFilenameOverwritePattern(), file.getFileName(), metadataJson);
            // transfer to dms
            switch (targetResource) {
            // to dms inbox
            case INBOX -> dmsOutPort.createContentObjectInInbox(dmsTarget, contentObjectName, fileStream);
            // create dms incoming
            case INCOMING_OBJECT -> this.processIncoming(file, useCase, dmsTarget, contentObjectName, fileStream, metadataJson);
            case METADATA_FILE -> throw new IllegalStateException("Target type metadata needs to be resolved to other types");
            }
        } catch (final IOException e) {
            throw new PresignedUrlException("Error while handling file InputStream", e);
        }
        // mark file as finished
        fileEventOutPort.fileFinished(event);
        log.info("File {} in use case {} finished", file, useCase.getName());
        // update metric
        dmsMeter.incrementProcessed(useCase.getName(), useCase.getType().name());
    }

    /**
     * Process {@link UseCase.Type#INCOMING_OBJECT} files.
     *
     * @param file The file to process.
     * @param useCase The use case of the file.
     * @param dmsTarget The resolved dms target.
     * @param contentObjectName The resolved name of the new ContentObject.
     * @param fileStream The content of the file.
     * @param metadataJson Parsed JsonNode of metadata file.
     */
    protected void processIncoming(final File file, final UseCase useCase, final DmsTarget dmsTarget, final String contentObjectName,
            final InputStream fileStream, final JsonNode metadataJson) throws MetadataException {
        // check target procedure name
        if (Strings.isNotBlank(useCase.getVerifyProcedureNamePattern())) {
            final String procedureName = this.dmsOutPort.getProcedureName(dmsTarget);
            final String resolvedPattern = this.patternHelper.applyPattern(useCase.getVerifyProcedureNamePattern(), file.getFileName(), metadataJson);
            if (!procedureName.toLowerCase(Locale.ROOT).contains(resolvedPattern.toLowerCase(Locale.ROOT))) {
                final String message = String.format("Procedure name %s doesn't contain resolved pattern %s", procedureName, resolvedPattern);
                throw new DmsException(message);
            }
        }
        // resolve name for Incoming
        final String incomingName;
        if (Strings.isBlank(useCase.getIncomingNamePattern())) {
            // use resolved ContentObject name (filename) if no pattern for Incoming name is defined
            // resolved in this case means the UseCase#filenameOverwritePattern is applied first
            incomingName = contentObjectName;
        } else {
            // else apply pattern to original filename
            incomingName = this.patternHelper.applyPattern(useCase.getIncomingNamePattern(), file.getFileName(), metadataJson);
        }
        // check if incoming already exists
        if (useCase.isReuseIncoming()) {
            final Optional<String> incomingCoo = this.dmsOutPort.getIncomingCooByName(dmsTarget, incomingName);
            if (incomingCoo.isPresent()) {
                // add ContentObject to Incoming
                final DmsTarget incomingDmsTarget = new DmsTarget(incomingCoo.get(), dmsTarget.userName(), dmsTarget.joboe(), dmsTarget.jobposition());
                this.dmsOutPort.createContentObject(incomingDmsTarget, contentObjectName, fileStream);
                return;
            }
        }
        // create Incoming
        dmsOutPort.createIncoming(dmsTarget, incomingName, contentObjectName, fileStream);
    }

    /**
     * Resolve target coo for useCase.
     * {@link UseCase.Type}
     *
     * @param resourceType Target type the coo is resolved for.
     * @param metadataJson Parsed JsonNode of metadata file.
     * @param useCase The use case.
     * @param file The file to resolve the coo for.
     * @return The resolved coo.
     */
    protected DmsTarget resolveTargetCoo(final UseCase.Type resourceType, final JsonNode metadataJson, final UseCase useCase, final File file)
            throws MetadataException {
        return switch (useCase.getCooSource()) {
        case METADATA_FILE -> this.resolveMetadataTargetCoo(resourceType, metadataJson, useCase);
        case FILENAME -> {
            if (Strings.isBlank(useCase.getFilenameCooPattern())) {
                throw new IllegalArgumentException("Filename coo pattern is required");
            }
            final String targetCoo = this.patternHelper.applyPattern(useCase.getFilenameCooPattern(), file.getFileName(), metadataJson);
            yield new DmsTarget(targetCoo, useCase.getUsername(), useCase.getJoboe(), useCase.getJobposition());
        }
        case FILENAME_MAP -> {
            // find first matching target coo from map
            final String targetCoo = useCase.getFilenameToCoo().entrySet().stream()
                    .filter(i -> Pattern.compile(i.getKey(), Pattern.CASE_INSENSITIVE).matcher(file.getFileName()).find())
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElseThrow(() -> new IllegalStateException("No matching filename map entry configured."));
            yield new DmsTarget(targetCoo, useCase.getUsername(), useCase.getJoboe(), useCase.getJobposition());
        }
        case STATIC -> new DmsTarget(useCase.getTargetCoo(), useCase.getUsername(), useCase.getJoboe(), useCase.getJobposition());
        case OU_WORK_QUEUE -> new DmsTarget(null, useCase.getUsername(), useCase.getJoboe(), useCase.getJobposition());
        };
    }

    /**
     * Resolve DmsTarget via metadata file.
     *
     * @param metadataJson Parsed JsonNode of metadata file.
     * @param useCase UseCase of the file.
     * @return Resolved DmsTarget.
     */
    protected DmsTarget resolveMetadataTargetCoo(final UseCase.Type resourceType, final JsonNode metadataJson, final UseCase useCase)
            throws MetadataException {
        // validate metadata json provided
        if (metadataJson == null) {
            throw new MetadataException("Metadata JSON is required");
        }
        // extract coo and username from metadata
        final DmsTarget metadataTarget = switch (resourceType) {
        case INBOX -> dmsMetadataHelper.resolveInboxDmsTarget(metadataJson);
        case INCOMING_OBJECT -> dmsMetadataHelper.resolveIncomingDmsTarget(metadataJson);
        case METADATA_FILE -> throw new IllegalStateException("Target type metadata needs to be resolved to other types");
        };
        // combine with use case joboe and jobposition
        return new DmsTarget(metadataTarget.coo(), metadataTarget.userName(), useCase.getJoboe(), useCase.getJobposition());
    }

    /**
     * Resolve dms target resource type from metadata file.
     *
     * @param metadataJson Parsed metadata json node.
     * @return The resolved type.
     * @throws MetadataException If metadata json can't be parsed or has illegal values.
     */
    protected UseCase.Type resolveTypeFromMetadataFile(final JsonNode metadataJson) throws MetadataException {
        // validate metadata json provided
        if (metadataJson == null) {
            throw new MetadataException("DMS target type via metadata file: Metadata JSON is required");
        }
        // load value from metadata file
        final Map<String, String> indexFields = this.dmsMetadataHelper.getIndexFields(metadataJson);
        final String metadataDmsTarget = indexFields.get(swimDmsProperties.getMetadataDmsTargetKey());
        // resolve type from value
        return switch (metadataDmsTarget) {
        case METADATA_TARGET_TYPE_INBOX -> UseCase.Type.INBOX;
        case METADATA_TARGET_TYPE_INCOMING -> UseCase.Type.INCOMING_OBJECT;
        case null, default ->
                throw new MetadataException(String.format("DMS target type via metadata file: Unexpected %s value: %s", swimDmsProperties.getMetadataDmsTargetKey(), metadataDmsTarget));
        };
    }
}
