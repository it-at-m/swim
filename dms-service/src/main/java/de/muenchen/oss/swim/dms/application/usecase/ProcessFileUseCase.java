package de.muenchen.oss.swim.dms.application.usecase;

import de.muenchen.oss.swim.dms.application.usecase.helper.DmsHelper;
import de.muenchen.oss.swim.dms.application.usecase.helper.TargetResolverHelper;
import de.muenchen.oss.swim.dms.configuration.DmsMeter;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.helper.DmsMetadataHelper;
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
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileReference;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import de.muenchen.oss.swim.libs.handlercore.domain.model.MultiFileEvent;
import de.muenchen.oss.swim.libs.handlercore.domain.model.PresignedFile;
import de.muenchen.oss.swim.libs.handlercore.domain.model.SingleFileEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase implements ProcessFileInPort {
    private final SwimDmsProperties swimDmsProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final FileEventOutPort fileEventOutPort;
    private final DmsMetadataHelper dmsMetadataHelper;
    private final TargetResolverHelper targetResolverHelper;
    private final DmsMeter dmsMeter;
    private final DmsHelper dmsHelper;

    @Override
    public void processEvent(final SingleFileEvent event)
            throws PresignedUrlException, UnknownUseCaseException, MetadataException {
        this.processEvent(MultiFileEvent.fromFileEvent(event));
    }

    @Override
    public void processEvent(final MultiFileEvent event) throws PresignedUrlException, UnknownUseCaseException, MetadataException {
        final UseCase useCase = swimDmsProperties.findUseCase(event.useCase());
        log.debug("Resolved use case: {}", useCase);
        final String firstFilePath = FileReference.fromPresignedUrl(event.files().getFirst().presignedUrl()).path();
        log.info("Processing {} file(s) with first file {} for use case {}", event.files().size(), firstFilePath, event.useCase());
        // load files
        final List<LoadedFile> files = new ArrayList<>();
        try {
            for (final PresignedFile presignedFile : event.files()) {
                files.add(this.loadFile(presignedFile));
            }
            // process
            this.processDmsResource(useCase, files);
            // mark file as finished
            fileEventOutPort.fileFinished(event);
            log.info("Finished {} files with first file {} for use case {}", event.files().size(), firstFilePath, event.useCase());
            // update metric
            dmsMeter.incrementProcessed(useCase.getName(), useCase.getType().name());
        } finally {
            this.closeLoadedFiles(files);
        }
    }

    /**
     * Resolves file metadata, content and metadata file (if present) for a PresignedFile.
     *
     * @param presignedFile The presigned URLs for a file.
     * @return The resolved file.
     * @throws PresignedUrlException If the presigned URL isn't valid.
     * @throws MetadataException If the metadata file couldn't be parsed.
     */
    protected LoadedFile loadFile(final PresignedFile presignedFile) throws PresignedUrlException, MetadataException {
        final FileReference fileReference = FileReference.fromPresignedUrl(presignedFile.presignedUrl());
        // load file
        final InputStream fileStream = fileSystemOutPort.getPresignedUrlFile(presignedFile.presignedUrl());
        // parse metadata file if present
        Metadata metadata = null;
        if (StringUtils.isNotBlank(presignedFile.metadataPresignedUrl())) {
            try (InputStream metadataFileStream = fileSystemOutPort.getPresignedUrlFile(presignedFile.metadataPresignedUrl())) {
                metadata = dmsMetadataHelper.parseMetadataFile(metadataFileStream);
            } catch (final IOException e) {
                closeFileStream(fileReference, fileStream);
                throw new PresignedUrlException("Error while handling metadata file InputStream", e);
            } catch (MetadataException | RuntimeException e) {
                closeFileStream(fileReference, fileStream);
                throw e;
            }
        }
        return new LoadedFile(fileReference, fileStream, metadata);
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
        final FileReference file = files.getFirst().fileReference();
        // resolve target resource type
        final UseCaseType targetResource = this.targetResolverHelper.resolveUseCaseType(useCase, metadata);
        // get target coo
        final DmsTarget dmsTarget = this.targetResolverHelper.resolveTargetCoo(targetResource, metadata, useCase, file);
        log.debug("Resolved dms target: {}", dmsTarget);
        // transfer to dms
        switch (targetResource) {
        // ContentObject in Inbox
        case INBOX_CONTENT_OBJECT -> dmsHelper.processInboxContentObject(useCase, dmsTarget, files);
        // Incoming in Inbox
        case INBOX_INCOMING -> dmsHelper.processInboxIncoming(useCase, dmsTarget, files);
        // Incoming in Procedure
        case PROCEDURE_INCOMING -> dmsHelper.processProcedureIncoming(useCase, dmsTarget, files);
        case METADATA_FILE -> throw new IllegalStateException("Target type metadata needs to be resolved to other types");
        }
    }

    private void closeLoadedFiles(final List<LoadedFile> files) {
        for (final LoadedFile file : files) {
            this.closeFileStream(file.fileReference(), file.content());
        }
    }

    private void closeFileStream(final FileReference fileReference, final InputStream fileStream) {
        try {
            fileStream.close();
        } catch (final IOException e) {
            log.warn("Failed to close file InputStream for {}", fileReference, e);
        }
    }
}
