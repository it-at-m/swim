package de.muenchen.oss.swim.dms.application.usecase;

import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.application.usecase.helper.TargetResolverHelper;
import de.muenchen.oss.swim.dms.configuration.DmsMeter;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.helper.DmsMetadataHelper;
import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.LoadedFile;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.dms.domain.model.UseCaseType;
import de.muenchen.oss.swim.libs.handlercore.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileReference;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import de.muenchen.oss.swim.libs.handlercore.domain.model.MultiFileEvent;
import de.muenchen.oss.swim.libs.handlercore.domain.model.PresignedFile;
import de.muenchen.oss.swim.libs.handlercore.domain.model.SingleFileEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase implements ProcessFileInPort {
    /**
     * Replacements for {@link UseCase#isDecodeGermanChars()}.
     * Requires {@link SwimDmsProperties#getDecodeGermanCharsPrefix()} as prefix.
     */
    private static final Map<String, String> UMLAUT_REPLACEMENTS = Map.of(
            "a", "ä",
            "u", "ü",
            "o", "ö",
            "s", "ß",
            "A", "Ä",
            "U", "Ü",
            "O", "Ö");

    private final SwimDmsProperties swimDmsProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final DmsOutPort dmsOutPort;
    private final FileEventOutPort fileEventOutPort;
    private final DmsMetadataHelper dmsMetadataHelper;
    private final PatternHelper patternHelper;
    private final TargetResolverHelper targetResolverHelper;
    private final DmsMeter dmsMeter;

    @Override
    public void processEvent(final SingleFileEvent event)
            throws PresignedUrlException, UnknownUseCaseException, MetadataException {
        this.processEvent(MultiFileEvent.fromFileEvent(event));
    }

    @Override
    public void processEvent(final MultiFileEvent event) throws PresignedUrlException, UnknownUseCaseException, MetadataException {
        final UseCase useCase = swimDmsProperties.findUseCase(event.useCase());
        log.debug("Resolved use case: {}", useCase);
        final String firstFileName = FileReference.fromPresignedUrl(event.files().getFirst().presignedUrl()).path();
        log.info("Processing {} file(s) with first file {} for use case {}", event.files().size(), firstFileName, event.useCase());
        // load files
        final List<LoadedFile> files = new ArrayList<>();
        for (final PresignedFile presignedFile : event.files()) {
            files.add(this.loadFile(useCase, presignedFile));
        }
        // process
        this.processDmsResource(useCase, files);
        // mark file as finished
        fileEventOutPort.fileFinished(event);
        log.info("Finished {} files with first file {} for use case {}", event.files().size(), firstFileName, event.useCase());
        // update metric
        dmsMeter.incrementProcessed(useCase.getName(), useCase.getType().name());
    }

    /**
     * Resolves file metadata, content and metadata file (if present) for a PresignedFile.
     *
     * @param useCase The use case of the file.
     * @param presignedFile The presigned URLs for a file.
     * @return The resolved file.
     * @throws PresignedUrlException If the presigned URL isn't valid.
     * @throws MetadataException If the metadata file couldn't be parsed.
     */
    protected LoadedFile loadFile(final UseCase useCase, final PresignedFile presignedFile) throws PresignedUrlException, MetadataException {
        final FileReference fileReference = FileReference.fromPresignedUrl(presignedFile.presignedUrl());
        // decode umlauts if enabled
        final FileReference decodedFile;
        if (useCase.isDecodeGermanChars()) {
            decodedFile = this.decodeGermanChars(fileReference);
        } else {
            decodedFile = null;
        }
        // load file
        final InputStream fileStream = fileSystemOutPort.getPresignedUrlFile(presignedFile.presignedUrl());
        // FIXME close InputStream at end
        // parse metadata file if present
        Metadata metadata = null;
        if (StringUtils.isNotBlank(presignedFile.metadataPresignedUrl())) {
            try (InputStream metadataFileStream = fileSystemOutPort.getPresignedUrlFile(presignedFile.metadataPresignedUrl())) {
                metadata = dmsMetadataHelper.parseMetadataFile(metadataFileStream);
            } catch (final IOException e) {
                throw new PresignedUrlException("Error while handling file InputStream", e);
            }
        }
        return new LoadedFile(fileReference, decodedFile, fileStream, metadata);
    }

    /**
     * Resolve target dms resource type and process.
     *
     * @param useCase The use case of the file.
     * @param files The files to process.
     */
    private void processDmsResource(final UseCase useCase, final List<LoadedFile> files) throws MetadataException {
        // use first file for resolving target
        final Metadata metadata = files.getFirst().metadata();
        final FileReference file = files.getFirst().decodedFileReference();
        // resolve target resource type
        final UseCaseType targetResource = this.targetResolverHelper.resolveUseCaseType(useCase, metadata);
        // get target coo
        final DmsTarget dmsTarget = this.targetResolverHelper.resolveTargetCoo(targetResource, metadata, useCase, file);
        log.debug("Resolved dms target: {}", dmsTarget);
        // transfer to dms
        switch (targetResource) {
        // ContentObject in Inbox
        case INBOX_CONTENT_OBJECT -> this.processInboxContentObject(useCase, dmsTarget, files);
        // Incoming in Inbox
        case INBOX_INCOMING -> this.processInboxIncoming(useCase, dmsTarget, files);
        // Incoming in Procedure
        case PROCEDURE_INCOMING -> this.processProcedureIncoming(useCase, dmsTarget, files);
        case METADATA_FILE -> throw new IllegalStateException("Target type metadata needs to be resolved to other types");
        }
    }

    /**
     * Process {@link UseCaseType#PROCEDURE_INCOMING} files.
     *
     * @param useCase The use case of the file.
     * @param dmsTarget The resolved dms target.
     * @param files The file to process.
     */
    protected void processProcedureIncoming(final UseCase useCase, final DmsTarget dmsTarget, final List<LoadedFile> files) throws MetadataException {
        // use first file for resolution
        final FileReference file = files.getFirst().decodedFileReference();
        final Metadata metadata = files.getFirst().metadata();
        // check target procedure name
        if (StringUtils.isNotBlank(useCase.getIncoming().getVerifyProcedureNamePattern())) {
            final String procedureName = this.dmsOutPort.getProcedureName(dmsTarget);
            final String resolvedPattern = this.patternHelper.applyPattern(useCase.getIncoming().getVerifyProcedureNamePattern(),
                    file.getFileNameWithoutExtension(),
                    metadata);
            if (!procedureName.toLowerCase(Locale.ROOT).contains(resolvedPattern.toLowerCase(Locale.ROOT))) {
                final String message = String.format("Procedure name %s doesn't contain resolved pattern %s", procedureName, resolvedPattern);
                throw new DmsException(message);
            }
        }
        // resolve ContentObjects parameters
        final List<DmsContentObjectRequest> contentObjects = new ArrayList<>();
        for (final LoadedFile lf : files) {
            contentObjects.add(this.resolveContentObjectParameters(lf.decodedFileReference(), useCase, lf.metadata(), lf.content()));
        }
        // resolve Incoming parameters (use first ContentObject)
        final DmsIncomingRequest incomingRequest = this.resolveIncomingParameters(file, useCase, metadata, contentObjects.getFirst());
        // check if incoming already exists
        if (useCase.getIncoming().isReuseIncoming()) {
            final Optional<String> incomingCoo = this.dmsOutPort.getIncomingCooByName(dmsTarget, incomingRequest.name());
            if (incomingCoo.isPresent()) {
                // add ContentObjects to Incoming
                final DmsTarget incomingDmsTarget = new DmsTarget(incomingCoo.get(), dmsTarget.getUsername(), dmsTarget.getJoboe(), dmsTarget.getJobposition());
                this.dmsOutPort.addContentObjectsToIncoming(incomingDmsTarget, contentObjects);
                return;
            }
        }
        // create Incoming
        dmsOutPort.createProcedureIncoming(dmsTarget, incomingRequest, contentObjects);
    }

    /**
     * Process {@link UseCaseType#INBOX_CONTENT_OBJECT} files.
     *
     * @param useCase The use case of the file.
     * @param dmsTarget The resolved dms target.
     * @param files The files to process.
     */
    protected void processInboxContentObject(final UseCase useCase, final DmsTarget dmsTarget, final List<LoadedFile> files) {
        // fail if more than one file
        if (files.size() > 1) {
            throw new IllegalArgumentException("InboxContentObject can only be created with a single file");
        }
        final LoadedFile loadedFile = files.getFirst();
        // resolve request context
        final DmsContentObjectRequest contentObjectRequest = this.resolveContentObjectParameters(
                loadedFile.decodedFileReference(), useCase, loadedFile.metadata(), loadedFile.content());
        // create ContentObject
        this.dmsOutPort.createContentObjectInInbox(dmsTarget, contentObjectRequest);
    }

    /**
     * Process {@link UseCaseType#INBOX_INCOMING} files.
     *
     * @param useCase The use case of the file.
     * @param dmsTarget The resolved dms target.
     * @param files The file to process.
     */
    protected void processInboxIncoming(final UseCase useCase, final DmsTarget dmsTarget, final List<LoadedFile> files) throws MetadataException {
        // use first file for resolution
        final FileReference file = files.getFirst().decodedFileReference();
        final Metadata metadata = files.getFirst().metadata();
        // resolve ContentObjects parameters
        final List<DmsContentObjectRequest> contentObjects = new ArrayList<>();
        for (final LoadedFile lf : files) {
            contentObjects.add(this.resolveContentObjectParameters(lf.decodedFileReference(), useCase, lf.metadata(), lf.content()));
        }
        // resolve request context
        final DmsIncomingRequest incomingRequest = this.resolveIncomingParameters(file, useCase, metadata, contentObjects.getFirst());
        // create Incoming
        this.dmsOutPort.createIncomingInInbox(dmsTarget, incomingRequest, contentObjects);
    }

    /**
     * Decode german special chars in path of a {@link FileReference}.
     * See {@link #UMLAUT_REPLACEMENTS} and {@link SwimDmsProperties#getDecodeGermanCharsPrefix()} for
     * replacements.
     *
     * @param file The file to decode the path in.
     * @return The file with the decoded path.
     */
    private FileReference decodeGermanChars(final FileReference file) {
        String decodedPath = file.path();
        for (final Map.Entry<String, String> entry : UMLAUT_REPLACEMENTS.entrySet()) {
            decodedPath = decodedPath.replace(
                    swimDmsProperties.getDecodeGermanCharsPrefix() + entry.getKey(),
                    entry.getValue());
        }
        return new FileReference(
                file.bucket(),
                decodedPath);
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
                        "%s (%s)",
                        i.getValue(),
                        i.getKey().replaceFirst("^" + swimDmsProperties.getMetadataSubjectPrefix(), "")))
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
    protected DmsContentObjectRequest resolveContentObjectParameters(final FileReference file, final UseCase useCase,
            final Metadata metadata, final InputStream content) {
        // resolve ContentObject name
        final String contentObjectName = String.format("%s.%s",
                this.patternHelper.applyPattern(useCase.getContentObject().getFilenameOverwritePattern(), file.getFileNameWithoutExtension(), metadata),
                file.getFileExtension());
        // resolve ContentObject subject
        final String contentObjectSubjectPattern = useCase.getContentObject().getSubjectPattern();
        final String contentObjectSubject = StringUtils.isNotBlank(contentObjectSubjectPattern)
                ? this.patternHelper.applyPattern(contentObjectSubjectPattern, file.getFileNameWithoutExtension(), metadata)
                : null;
        return new DmsContentObjectRequest(contentObjectName, contentObjectSubject, content);
    }

    /**
     * Resolve parameters for new Incoming.
     *
     * @param file The file to resolve the values for.
     * @param metadata Parsed metadata file.
     * @param useCase The use case.
     * @return Resolved parameters for new Incoming.
     */
    protected DmsIncomingRequest resolveIncomingParameters(final FileReference file, final UseCase useCase,
            final Metadata metadata, final DmsContentObjectRequest contentObjectRequest) throws MetadataException {
        // TODO use basename instead of filename?
        // resolve name for Incoming
        final String incomingName;
        if (StringUtils.isBlank(useCase.getIncoming().getIncomingNamePattern())) {
            // use resolved ContentObject name (filename) if no pattern for Incoming name is defined
            // resolved in this case means the UseCase#filenameOverwritePattern is applied first
            // extension is removed
            incomingName = contentObjectRequest.getNameWithoutExtension();
        } else {
            // else apply pattern to original filename
            final String patternIncomingName = this.patternHelper.applyPattern(useCase.getIncoming().getIncomingNamePattern(),
                    file.getFileNameWithoutExtension(), metadata);
            incomingName = StringUtils.isNotBlank(patternIncomingName) ? patternIncomingName :
            // fallback to default if empty name via pattern
                    contentObjectRequest.getNameWithoutExtension();
        }
        // resolve subject for Incoming
        final String incomingSubject;
        if (StringUtils.isNotBlank(useCase.getIncoming().getIncomingSubjectPattern())) {
            incomingSubject = this.patternHelper.applyPattern(useCase.getIncoming().getIncomingSubjectPattern(),
                    file.getFileNameWithoutExtension(), metadata);
        } else if (useCase.getIncoming().isMetadataSubject()) {
            incomingSubject = this.subjectFromMetadata(metadata);
        } else {
            incomingSubject = null;
        }
        return new DmsIncomingRequest(incomingName, incomingSubject);
    }
}
