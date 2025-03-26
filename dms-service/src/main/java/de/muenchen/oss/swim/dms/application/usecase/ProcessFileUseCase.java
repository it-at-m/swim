package de.muenchen.oss.swim.dms.application.usecase;

import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.application.usecase.helper.TargetResolverHelper;
import de.muenchen.oss.swim.dms.configuration.DmsMeter;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.helper.DmsMetadataHelper;
import de.muenchen.oss.swim.dms.domain.helper.PatternHelper;
import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.dms.domain.model.UseCaseType;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase implements ProcessFileInPort {
    private final SwimDmsProperties swimDmsProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final DmsOutPort dmsOutPort;
    private final FileEventOutPort fileEventOutPort;
    private final DmsMetadataHelper dmsMetadataHelper;
    private final PatternHelper patternHelper;
    private final TargetResolverHelper targetResolverHelper;
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
            final UseCaseType targetResource;
            if (useCase.getType() == UseCaseType.METADATA_FILE) {
                targetResource = this.resolveTypeFromMetadataFile(metadata);
            } else {
                targetResource = useCase.getType();
            }
            // get target coo
            final DmsTarget dmsTarget = this.targetResolverHelper.resolveTargetCoo(targetResource, metadata, useCase, file);
            log.debug("Resolved dms target: {}", dmsTarget);
            // transfer to dms
            switch (targetResource) {
            // ContentObject in Inbox
            case INBOX_CONTENT_OBJECT -> this.processInboxContentObject(file, useCase, dmsTarget, fileStream, metadata);
            // Incoming in Inbox
            case INBOX_INCOMING -> this.processInboxIncoming(file, useCase, dmsTarget, fileStream, metadata);
            // Incoming in Procedure
            case PROCEDURE_INCOMING -> this.processProcedureIncoming(file, useCase, dmsTarget, fileStream, metadata);
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
     * Process {@link UseCaseType#PROCEDURE_INCOMING} files.
     *
     * @param file The file to process.
     * @param useCase The use case of the file.
     * @param dmsTarget The resolved dms target.
     * @param fileStream The content of the file.
     * @param metadata Parsed metadata file.
     */
    protected void processProcedureIncoming(final File file, final UseCase useCase, final DmsTarget dmsTarget,
            final InputStream fileStream, final Metadata metadata) throws MetadataException {
        final String filename = file.getFileName();
        final String filenameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
        // check target procedure name
        if (Strings.isNotBlank(useCase.getIncoming().getVerifyProcedureNamePattern())) {
            final String procedureName = this.dmsOutPort.getProcedureName(dmsTarget);
            final String resolvedPattern = this.patternHelper.applyPattern(useCase.getIncoming().getVerifyProcedureNamePattern(), filenameWithoutExtension,
                    metadata);
            if (!procedureName.toLowerCase(Locale.ROOT).contains(resolvedPattern.toLowerCase(Locale.ROOT))) {
                final String message = String.format("Procedure name %s doesn't contain resolved pattern %s", procedureName, resolvedPattern);
                throw new DmsException(message);
            }
        }
        // resolve Incoming parameters
        final DmsIncomingRequest incomingRequest = this.resolveIncomingParameters(file, useCase, metadata);
        // check if incoming already exists
        if (useCase.getIncoming().isReuseIncoming()) {
            final Optional<String> incomingCoo = this.dmsOutPort.getIncomingCooByName(dmsTarget, incomingRequest.name());
            if (incomingCoo.isPresent()) {
                // add ContentObject to Incoming
                final DmsTarget incomingDmsTarget = new DmsTarget(incomingCoo.get(), dmsTarget.getUsername(), dmsTarget.getJoboe(), dmsTarget.getJobposition());
                this.dmsOutPort.createContentObject(incomingDmsTarget, incomingRequest.contentObject(), fileStream);
                return;
            }
        }
        // create Incoming
        dmsOutPort.createProcedureIncoming(dmsTarget, incomingRequest, fileStream);
    }

    /**
     * Process {@link UseCaseType#INBOX_CONTENT_OBJECT} files.
     *
     * @param file The file to process.
     * @param useCase The use case of the file.
     * @param dmsTarget The resolved dms target.
     * @param fileStream The content of the file.
     * @param metadata Parsed metadata file.
     */
    protected void processInboxContentObject(final File file, final UseCase useCase, final DmsTarget dmsTarget,
            final InputStream fileStream, final Metadata metadata) {
        final DmsContentObjectRequest contentObjectRequest = this.resolveContentObjectParameters(file, useCase, metadata);
        // create ContentObject
        this.dmsOutPort.createContentObjectInInbox(dmsTarget, contentObjectRequest, fileStream);
    }

    /**
     * Process {@link UseCaseType#INBOX_INCOMING} files.
     *
     * @param file The file to process.
     * @param useCase The use case of the file.
     * @param dmsTarget The resolved dms target.
     * @param fileStream The content of the file.
     * @param metadata Parsed metadata file.
     */
    protected void processInboxIncoming(final File file, final UseCase useCase, final DmsTarget dmsTarget,
            final InputStream fileStream, final Metadata metadata) throws MetadataException {
        final DmsIncomingRequest incomingRequest = this.resolveIncomingParameters(file, useCase, metadata);
        // create Incoming
        this.dmsOutPort.createIncomingInInbox(dmsTarget, incomingRequest, fileStream);
    }

    /**
     * Resolve dms target resource type from metadata file.
     *
     * @param metadata Parsed metadata file.
     * @return The resolved type.
     * @throws MetadataException If metadata can't be parsed or has illegal values.
     */
    protected UseCaseType resolveTypeFromMetadataFile(final Metadata metadata) throws MetadataException {
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
     * Resolve parameters for new ContentObject.
     *
     * @param file The file to resolve the values for.
     * @param metadata Parsed metadata file.
     * @param useCase The use case.
     * @return Resolved parameters for new ContentObject.
     */
    protected DmsContentObjectRequest resolveContentObjectParameters(final File file, final UseCase useCase,
            final Metadata metadata) {
        // resolve ContentObject name
        final String contentObjectName = this.patternHelper.applyPattern(useCase.getContentObject().getFilenameOverwritePattern(), file.getFileName(),
                metadata);
        // resolve ContentObject subject
        final String contentObjectSubjectPattern = useCase.getContentObject().getSubjectPattern();
        final String contentObjectSubject = Strings.isNotBlank(contentObjectSubjectPattern)
                ? this.patternHelper.applyPattern(contentObjectSubjectPattern, file.getFileName(), metadata)
                : null;
        return new DmsContentObjectRequest(contentObjectName, contentObjectSubject);
    }

    /**
     * Resolve parameters for new Incoming.
     *
     * @param file The file to resolve the values for.
     * @param metadata Parsed metadata file.
     * @param useCase The use case.
     * @return Resolved parameters for new Incoming.
     */
    protected DmsIncomingRequest resolveIncomingParameters(final File file, final UseCase useCase,
            final Metadata metadata) throws MetadataException {
        // resolve ContentObject
        final DmsContentObjectRequest contentObjectRequest = this.resolveContentObjectParameters(file, useCase, metadata);
        // resolve name for Incoming
        final String incomingName;
        if (Strings.isBlank(useCase.getIncoming().getIncomingNamePattern())) {
            // use resolved ContentObject name (filename) if no pattern for Incoming name is defined
            // resolved in this case means the UseCase#filenameOverwritePattern is applied first
            // extension is removed
            incomingName = contentObjectRequest.name().substring(0, contentObjectRequest.name().lastIndexOf('.'));
        } else {
            // else apply pattern to original filename
            final String filename = file.getFileName();
            final String filenameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
            incomingName = this.patternHelper.applyPattern(useCase.getIncoming().getIncomingNamePattern(), filenameWithoutExtension, metadata);
        }
        // resolve subject for Incoming;
        final String incomingSubject;
        if (useCase.getIncoming().isMetadataSubject()) {
            incomingSubject = this.subjectFromMetadata(metadata);
        } else {
            incomingSubject = incomingName;
        }
        return new DmsIncomingRequest(incomingName, incomingSubject, contentObjectRequest);
    }
}
