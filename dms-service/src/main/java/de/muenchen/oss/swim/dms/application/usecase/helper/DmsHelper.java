package de.muenchen.oss.swim.dms.application.usecase.helper;

import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.LoadedFile;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.dms.domain.model.UseCaseType;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileReference;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmsHelper {
    private final RequestResolverHelper requestResolverHelper;
    private final PatternHelper patternHelper;
    private final DmsOutPort dmsOutPort;

    /**
     * Process {@link UseCaseType#PROCEDURE_INCOMING} files.
     *
     * @param useCase The use case of the files.
     * @param dmsTarget The resolved dms target.
     * @param files The files to process.
     */
    public void processProcedureIncoming(final UseCase useCase, final DmsTarget dmsTarget, final List<LoadedFile> files) throws MetadataException {
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
            contentObjects.add(requestResolverHelper.resolveContentObjectParameters(lf.decodedFileReference(), useCase, lf.metadata(), lf.content()));
        }
        // resolve Incoming parameters (use first ContentObject)
        final DmsIncomingRequest incomingRequest = requestResolverHelper.resolveIncomingParameters(file, useCase, metadata, contentObjects.getFirst());
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
     * @param useCase The use case of the files.
     * @param dmsTarget The resolved dms target.
     * @param files The files to process.
     */
    public void processInboxContentObject(final UseCase useCase, final DmsTarget dmsTarget, final List<LoadedFile> files) {
        // fail if more than one file
        if (files.size() > 1) {
            throw new IllegalArgumentException("InboxContentObject can only be created with a single file");
        }
        final LoadedFile loadedFile = files.getFirst();
        // resolve request context
        final DmsContentObjectRequest contentObjectRequest = requestResolverHelper.resolveContentObjectParameters(
                loadedFile.decodedFileReference(), useCase, loadedFile.metadata(), loadedFile.content());
        // create ContentObject
        this.dmsOutPort.createContentObjectInInbox(dmsTarget, contentObjectRequest);
    }

    /**
     * Process {@link UseCaseType#INBOX_INCOMING} files.
     *
     * @param useCase The use case of the files.
     * @param dmsTarget The resolved dms target.
     * @param files The file to process.
     */
    public void processInboxIncoming(final UseCase useCase, final DmsTarget dmsTarget, final List<LoadedFile> files) throws MetadataException {
        // use first file for resolution
        final FileReference file = files.getFirst().decodedFileReference();
        final Metadata metadata = files.getFirst().metadata();
        // resolve ContentObjects parameters
        final List<DmsContentObjectRequest> contentObjects = new ArrayList<>();
        for (final LoadedFile lf : files) {
            contentObjects.add(requestResolverHelper.resolveContentObjectParameters(lf.decodedFileReference(), useCase, lf.metadata(), lf.content()));
        }
        // resolve Incoming parameters (use first ContentObject)
        final DmsIncomingRequest incomingRequest = requestResolverHelper.resolveIncomingParameters(file, useCase, metadata, contentObjects.getFirst());
        // create Incoming
        this.dmsOutPort.createIncomingInInbox(dmsTarget, incomingRequest, contentObjects);
    }
}
