package de.muenchen.oss.swim.dms.application.usecase.helper;

import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsProcedureRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.LoadedFile;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.dms.domain.model.UseCaseType;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileReference;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter SHADOW_PROCEDURE_NAME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter SHADOW_INCOMING_NAME_PATTERN = DateTimeFormatter.ofPattern("dd");

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
        final FileReference file = files.getFirst().fileReference();
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
            contentObjects.add(requestResolverHelper.resolveContentObjectParameters(lf.fileReference(), useCase, lf.metadata(), lf.content()));
        }
        // resolve Incoming parameters (use first ContentObject)
        final DmsIncomingRequest incomingRequest = requestResolverHelper.resolveIncomingParameters(file, useCase, metadata, contentObjects.getFirst());
        // find or create Incoming and create ContentObject
        createOrReuseIncomingWithContentObject(useCase.getIncoming().isReuseIncoming(), dmsTarget, incomingRequest, contentObjects);
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
                loadedFile.fileReference(), useCase, loadedFile.metadata(), loadedFile.content());
        // create ContentObject
        this.dmsOutPort.createInboxContentObject(dmsTarget, contentObjectRequest);
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
        final FileReference file = files.getFirst().fileReference();
        final Metadata metadata = files.getFirst().metadata();
        // resolve ContentObjects parameters
        final List<DmsContentObjectRequest> contentObjects = new ArrayList<>();
        for (final LoadedFile lf : files) {
            contentObjects.add(requestResolverHelper.resolveContentObjectParameters(lf.fileReference(), useCase, lf.metadata(), lf.content()));
        }
        // resolve Incoming parameters (use first ContentObject)
        final DmsIncomingRequest incomingRequest = requestResolverHelper.resolveIncomingParameters(file, useCase, metadata, contentObjects.getFirst());
        // create Incoming
        this.dmsOutPort.createInboxIncoming(dmsTarget, incomingRequest, contentObjects);
    }

    /**
     * Process {@link UseCaseType#SHADOW_FILE } files.
     *
     * @param useCase The use case of the files.
     * @param dmsTarget The resolved dms target.
     * @param files The files to process.
     */
    public void processShadowFile(final UseCase useCase, final DmsTarget dmsTarget, final List<LoadedFile> files) {
        // fail if more than one file
        if (files.size() != 1) {
            throw new IllegalArgumentException("Shadow file can only be created with a single file");
        }
        final LoadedFile file = files.getFirst();
        final LocalDate currentDate = LocalDate.now();
        final String procedureName = currentDate.format(SHADOW_PROCEDURE_NAME_PATTERN);
        // search for Procedure
        final Optional<String> existingProcedureCoo = this.dmsOutPort.getProcedureCooByName(dmsTarget, procedureName);
        // create Procedure if not exists
        final String procedureCoo;
        if (existingProcedureCoo.isEmpty()) {
            final DmsProcedureRequest procedureRequest = new DmsProcedureRequest(procedureName);
            procedureCoo = this.dmsOutPort.createFileProcedure(dmsTarget, procedureRequest);
        } else {
            procedureCoo = existingProcedureCoo.get();
        }
        final DmsTarget procedureDmsTarget = new DmsTarget(procedureCoo, dmsTarget);
        // create ContentObject and Incoming (if not exist else reuse)
        final String incomingName = currentDate.format(SHADOW_INCOMING_NAME_PATTERN);
        final DmsIncomingRequest incomingRequest = new DmsIncomingRequest(incomingName, null);
        final DmsContentObjectRequest contentObjectRequest = new DmsContentObjectRequest(file.decodedFileReference().getFileName(), null, file.content());
        this.createOrReuseIncomingWithContentObject(true, procedureDmsTarget, incomingRequest, List.of(contentObjectRequest));
    }

    /**
     * Create an ContentObject inside an Incoming.
     * Searches for Incoming name and reuses
     *
     * @param reuseIncoming Weither to reuse Incomings.
     * @param dmsTarget The resolved dms target.
     * @param incomingRequest The parameters for the Incoming.
     * @param contentObjects The parameters for the ContentObjects.
     */
    private void createOrReuseIncomingWithContentObject(final boolean reuseIncoming, final DmsTarget dmsTarget,
            final DmsIncomingRequest incomingRequest,
            final List<DmsContentObjectRequest> contentObjects) {
        // check if incoming already exists (if enabled)
        if (reuseIncoming) {
            final Optional<String> incomingCoo = this.dmsOutPort.getIncomingCooByNamePrefix(dmsTarget, incomingRequest.name());
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
}
