package de.muenchen.oss.swim.dms.application.usecase;

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
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
            Metadata metadata = null;
            if (Strings.isNotBlank(event.metadataPresignedUrl())) {
                try (InputStream metadataFileStream = fileSystemOutPort.getPresignedUrlFile(event.metadataPresignedUrl())) {
                    metadata = dmsMetadataHelper.parseMetadataFile(metadataFileStream);
                }
            }
            // resolve target resource type
            final UseCase.Type targetResource;
            if (useCase.getType() == UseCase.Type.METADATA_FILE) {
                targetResource = this.resolveTypeFromMetadataFile(metadata);
            } else {
                targetResource = useCase.getType();
            }
            // get target coo
            final DmsTarget dmsTarget = this.resolveTargetCoo(targetResource, metadata, useCase, file);
            log.debug("Resolved dms target: {}", dmsTarget);
            // get ContentObject name
            final String contentObjectName = this.patternHelper.applyPattern(useCase.getFilenameOverwritePattern(), file.getFileName(), metadata);
            // transfer to dms
            switch (targetResource) {
            // to dms inbox
            case INBOX -> dmsOutPort.createContentObjectInInbox(dmsTarget, contentObjectName, fileStream);
            // create dms incoming
            case INCOMING_OBJECT -> this.processIncoming(file, useCase, dmsTarget, contentObjectName, fileStream, metadata);
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
     * @param metadata Parsed metadata file.
     */
    protected void processIncoming(final File file, final UseCase useCase, final DmsTarget dmsTarget, final String contentObjectName,
            final InputStream fileStream, final Metadata metadata) throws MetadataException {
        final String contentObjectNameWithoutExtension = contentObjectName.substring(0, contentObjectName.lastIndexOf('.'));
        final String filename = file.getFileName();
        final String filenameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
        // check target procedure name
        if (Strings.isNotBlank(useCase.getVerifyProcedureNamePattern())) {
            final String procedureName = this.dmsOutPort.getProcedureName(dmsTarget);
            final String resolvedPattern = this.patternHelper.applyPattern(useCase.getVerifyProcedureNamePattern(), filenameWithoutExtension, metadata);
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
            incomingName = contentObjectNameWithoutExtension;
        } else {
            // else apply pattern to original filename
            incomingName = this.patternHelper.applyPattern(useCase.getIncomingNamePattern(), filenameWithoutExtension, metadata);
        }
        // resolve subject for Incoming;
        final String incomingSubject;
        if (useCase.isMetadataSubject()) {
            incomingSubject = this.subjectFromMetadata(metadata);
        } else {
            incomingSubject = incomingName;
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
        dmsOutPort.createIncoming(dmsTarget, incomingName, incomingSubject, contentObjectName, fileStream);
    }

    /**
     * Resolve target coo for useCase.
     * {@link UseCase.Type}
     *
     * @param resourceType Target type the coo is resolved for.
     * @param metadata Parsed metadata file.
     * @param useCase The use case.
     * @param file The file to resolve the coo for.
     * @return The resolved coo.
     */
    protected DmsTarget resolveTargetCoo(final UseCase.Type resourceType, final Metadata metadata, final UseCase useCase, final File file)
            throws MetadataException {
        return switch (useCase.getCooSource()) {
        case METADATA_FILE -> this.resolveMetadataTargetCoo(resourceType, metadata, useCase);
        case FILENAME -> {
            if (Strings.isBlank(useCase.getFilenameCooPattern())) {
                throw new IllegalArgumentException("Filename coo pattern is required");
            }
            final String targetCoo = this.patternHelper.applyPattern(useCase.getFilenameCooPattern(), file.getFileName(), metadata);
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
     * @param metadata Parsed metadata file.
     * @param useCase UseCase of the file.
     * @return Resolved DmsTarget.
     */
    protected DmsTarget resolveMetadataTargetCoo(final UseCase.Type resourceType, final Metadata metadata, final UseCase useCase) throws MetadataException {
        // validate metadata provided
        if (metadata == null) {
            throw new MetadataException("Target coo via metadata file: Metadata is required");
        }
        // extract coo and username from metadata
        final DmsTarget metadataTarget = switch (resourceType) {
        case INBOX -> dmsMetadataHelper.resolveInboxDmsTarget(metadata);
        case INCOMING_OBJECT -> dmsMetadataHelper.resolveIncomingDmsTarget(metadata);
        case METADATA_FILE -> throw new IllegalStateException("Target type metadata needs to be resolved to other types");
        };
        // combine resolves target with use case
        return this.combineDmsTargetWithUseCase(metadataTarget, useCase);
    }

    /**
     * Resolve dms target resource type from metadata file.
     *
     * @param metadata Parsed metadata file.
     * @return The resolved type.
     * @throws MetadataException If metadata can't be parsed or has illegal values.
     */
    protected UseCase.Type resolveTypeFromMetadataFile(final Metadata metadata) throws MetadataException {
        // validate metadata provided
        if (metadata == null) {
            throw new MetadataException("DMS target type via metadata file: Metadata is required");
        }
        // load value from metadata file
        final Map<String, String> indexFields = metadata.indexFields();
        final String metadataDmsTarget = indexFields.get(swimDmsProperties.getMetadataDmsTargetKey());
        // resolve type from value
        return switch (metadataDmsTarget) {
        case METADATA_TARGET_TYPE_INBOX -> UseCase.Type.INBOX;
        case METADATA_TARGET_TYPE_INCOMING -> UseCase.Type.INCOMING_OBJECT;
        case null, default ->
                throw new MetadataException(String.format("DMS target type via metadata file: Unexpected %s value: %s", swimDmsProperties.getMetadataDmsTargetKey(), metadataDmsTarget));
        };
    }

    /**
     * Build subject from metadata file.
     *
     * @param metadata Parsed metadata file.
     * @return Constructed subject.
     */
    protected String subjectFromMetadata(final Metadata metadata) throws MetadataException {
        // validate metadata provided
        if (metadata == null) {
            throw new MetadataException("Metadata is required");
        }
        // map index fields with prefix to subject
        final Map<String, String> indexFields = metadata.indexFields();
        return indexFields.entrySet().stream()
                // filter for prefix
                .filter(i -> i.getKey().startsWith(swimDmsProperties.getMetadataSubjectPrefix()))
                // sort
                .sorted(Map.Entry.comparingByKey())
                // build subject string
                .map(i -> String.format(
                        "%s: %s",
                        i.getKey().replaceFirst("^" + swimDmsProperties.getMetadataSubjectPrefix(), ""),
                        i.getValue()))
                .collect(Collectors.joining("\n"));
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
        final boolean dmsTargetHasJob = Strings.isNotBlank(dmsTarget.joboe()) || Strings.isNotBlank(dmsTarget.jobposition());
        final boolean useCaseHasJob = Strings.isNotBlank(useCase.getJoboe()) || Strings.isNotBlank(useCase.getJobposition());
        if (dmsTargetHasJob && useCaseHasJob) {
            throw new IllegalStateException("Resolve dms target: Job oe and position defined via resolve and via use case not allowed");
        }
        if (dmsTargetHasJob) {
            return dmsTarget;
        }
        return new DmsTarget(dmsTarget.coo(), dmsTarget.userName(), useCase.getJoboe(), useCase.getJobposition());
    }
}
