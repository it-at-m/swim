package de.muenchen.oss.swim.dms.application.usecase;

import de.muenchen.oss.swim.dms.application.port.in.ProcessFileInPort;
import de.muenchen.oss.swim.dms.application.port.out.DmsOutPort;
import de.muenchen.oss.swim.dms.application.port.out.FileEventOutPort;
import de.muenchen.oss.swim.dms.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import de.muenchen.oss.swim.dms.domain.exception.MetadataException;
import de.muenchen.oss.swim.dms.domain.exception.PresignedUrlException;
import de.muenchen.oss.swim.dms.domain.exception.UnknownUseCaseException;
import de.muenchen.oss.swim.dms.domain.helper.MetadataHelper;
import de.muenchen.oss.swim.dms.domain.model.DmsTarget;
import de.muenchen.oss.swim.dms.domain.model.File;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase implements ProcessFileInPort {
    public static final String PATTERN_JOINER = "-";

    private final SwimDmsProperties swimDmsProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final DmsOutPort dmsOutPort;
    private final FileEventOutPort fileEventOutPort;
    private final MetadataHelper metadataHelper;

    @Override
    public void processFile(final String useCaseName, final File file, final String presignedUrl, final String metadataPresignedUrl)
            throws PresignedUrlException, UnknownUseCaseException, MetadataException {
        log.info("Processing file {} for use case {}", file, useCaseName);
        final UseCase useCase = swimDmsProperties.findUseCase(useCaseName);
        log.debug("Resolved use case: {}", useCase);
        // load file
        try (InputStream fileStream = fileSystemOutPort.getPresignedUrlFile(presignedUrl)) {
            // get target coo
            final DmsTarget dmsTarget = resolveTargetCoo(metadataPresignedUrl, useCase, file);
            log.debug("Resolved dms target: {}", dmsTarget);
            // get ContentObject name
            final String contentObjectName = this.applyOverwritePattern(useCase.getFilenameOverwritePattern(), file.getFileName(), PATTERN_JOINER);
            // transfer to dms
            switch (useCase.getType()) {
            // to dms inbox
            case INBOX -> dmsOutPort.createContentObjectInInbox(dmsTarget, contentObjectName, fileStream);
            // create dms incoming
            case INCOMING_OBJECT -> this.processIncoming(file, useCase, dmsTarget, contentObjectName, fileStream);
            }
        } catch (final IOException e) {
            throw new PresignedUrlException("Error while handling file InputStream", e);
        }
        // mark file as finished
        fileEventOutPort.fileFinished(useCaseName, presignedUrl, metadataPresignedUrl);
    }

    protected void processIncoming(final File file, final UseCase useCase, final DmsTarget dmsTarget, final String contentObjectName, final InputStream fileStream) {
        // check target procedure name
        if (Strings.isNotBlank(useCase.getVerifyProcedureNamePattern())) {
            final String procedureName = this.dmsOutPort.getProcedureName(dmsTarget);
            final String resolvedPattern = this.applyOverwritePattern(useCase.getVerifyProcedureNamePattern(), file.getFileName(), "");
            if (!procedureName.toLowerCase(Locale.ROOT).contains(resolvedPattern.toLowerCase(Locale.ROOT))) {
                final String message = String.format("Procedure name %s doesn't contain resolved pattern %s", procedureName, resolvedPattern);
                throw new DmsException(message);
            }
        }
        // resolve name for Incoming
        final String incomingName;
        if (Strings.isBlank(useCase.getIncomingNamePattern())) {
            // use overwritten filename if no pattern for Incoming name is defined
            incomingName = contentObjectName;
        } else {
            // else apply pattern to original filename
            incomingName = this.applyOverwritePattern(useCase.getIncomingNamePattern(), file.getFileName(), PATTERN_JOINER);
        }
        // check if incoming already exists
        if (useCase.isReuseIncoming()) {
            final String incomingCoo = this.dmsOutPort.getIncomingCooByName(dmsTarget, incomingName);
            if (incomingCoo != null) {
                // add ContentObject to Incoming
                final DmsTarget incomingDmsTarget = new DmsTarget(incomingCoo, dmsTarget.userName(), dmsTarget.joboe(), dmsTarget.jobposition());
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
     * @param metadataPresignedUrl Presigned url for metadata file.
     * @param useCase The use case.
     * @return The resolved coo.
     */
    protected DmsTarget resolveTargetCoo(final String metadataPresignedUrl, final UseCase useCase, final File file)
            throws MetadataException, PresignedUrlException {
        return switch (useCase.getCooSource()) {
        case METADATA_FILE -> this.resolveMetadataTargetCoo(metadataPresignedUrl, useCase);
        case FILENAME -> {
            if (Strings.isBlank(useCase.getFilenameCooPattern())) {
                throw new IllegalArgumentException("Filename coo pattern is required");
            }
            final String targetCoo = this.applyOverwritePattern(useCase.getFilenameCooPattern(), file.getFileName(), "");
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
     * @param metadataPresignedUrl Presigned url of the metadata file.
     * @param useCase UseCase of the file.
     * @return Resolved DmsTarget.
     */
    protected DmsTarget resolveMetadataTargetCoo(final String metadataPresignedUrl, final UseCase useCase) throws MetadataException, PresignedUrlException {
        // validate metadata presigned url provided
        if (Strings.isBlank(metadataPresignedUrl)) {
            throw new MetadataException("Metadata presigned url empty but required");
        }
        // get metadata file
        try (InputStream metadataStream = fileSystemOutPort.getPresignedUrlFile(metadataPresignedUrl)) {
            // extract coo and username from metadata
            final DmsTarget metadataTarget = metadataHelper.resolveDmsTarget(metadataStream);
            // combine with use case joboe and jobposition
            return new DmsTarget(metadataTarget.coo(), metadataTarget.userName(), useCase.getJoboe(), useCase.getJobposition());
        } catch (final IOException e) {
            throw new MetadataException("Error while processing metadata file", e);
        }
    }

    /**
     * Apply pattern to input String by joining all matching groups.
     *
     * @param pattern Pattern to apply.
     * @param input Input to apply pattern to.
     * @param joiner Sequence for joining matching groups.
     * @return Result string.
     */
    protected String applyOverwritePattern(final String pattern, final String input, final String joiner) {
        if (Strings.isBlank(pattern)) {
            return input;
        }
        final Matcher matcher = Pattern.compile(pattern).matcher(input);
        if (matcher.find()) {
            final List<String> groups = new ArrayList<>();
            for (int i = 1; i <= matcher.groupCount(); i++) {
                groups.add(matcher.group(i));
            }
            return String.join(joiner, groups);
        } else {
            throw new IllegalStateException("Overwrite pattern not matching input");
        }
    }
}
