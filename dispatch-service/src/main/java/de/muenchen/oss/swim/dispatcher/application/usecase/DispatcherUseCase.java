package de.muenchen.oss.swim.dispatcher.application.usecase;

import static de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper.FILE_EXTENSION_PDF;

import de.muenchen.oss.swim.dispatcher.application.port.in.DispatcherInPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileDispatchingOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper;
import de.muenchen.oss.swim.dispatcher.configuration.DispatchMeter;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSizeException;
import de.muenchen.oss.swim.dispatcher.domain.exception.MetadataException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispatcherUseCase implements DispatcherInPort {
    protected static final String ACTION_DISPATCH = "dispatch";
    protected static final String ACTION_REROUTE = "reroute";
    protected static final String ACTION_DELETE = "delete";
    protected static final String ACTION_IGNORE = "ignore";
    protected static final String ACTION_REROUTE_DESTINATION_TAG_KEY = "SWIM_Reroute_Destination";

    private final SwimDispatcherProperties swimDispatcherProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final FileDispatchingOutPort fileDispatchingOutPort;
    private final NotificationOutPort notificationOutPort;
    private final FileHandlingHelper fileHandlingHelper;
    private final DispatchMeter dispatchMeter;

    @Override
    public void triggerDispatching() {
        log.info("Starting dispatching");
        for (final UseCase useCase : swimDispatcherProperties.getUseCases()) {
            // handle files directly in directory
            final String dispatchPath = useCase.getDispatchPath(swimDispatcherProperties);
            final Map<String, Throwable> errors = new HashMap<>(
                    this.processDirectory(useCase, dispatchPath, false));
            // handle recursive by directory
            if (useCase.isRecursive()) {
                // get folders
                final List<String> folders = fileSystemOutPort.getSubDirectories(useCase.getBucket(), dispatchPath);
                // dispatch files per folder if not in finished folder
                for (final String folder : folders) {
                    if (!folder.contains(swimDispatcherProperties.getFinishedFolder())) {
                        errors.putAll(this.processDirectory(useCase, folder, true));
                    }
                }
            }
            // send errors
            if (!errors.isEmpty()) {
                notificationOutPort.sendDispatchErrors(useCase.getMailAddresses(), useCase.getName(), errors);
            }
        }
        log.info("Finished dispatching");
    }

    /**
     * Process all files inside a folder recursively.
     * See {@link DispatcherUseCase#processFile}.
     *
     * @param useCase The bucket of the folder.
     * @param folder The full path of the folder.
     * @return Error which occurred during processing (Key: file path, value: error).
     */
    private @NotNull
    Map<String, Throwable> processDirectory(final UseCase useCase, final String folder, final boolean recursive) {
        final Map<File, Map<String, String>> readyFiles = fileSystemOutPort.getMatchingFiles(
                useCase.getBucket(),
                folder,
                recursive,
                FILE_EXTENSION_PDF,
                useCase.getRequiredTags(),
                swimDispatcherProperties.getDispatchExcludeTags());
        // for each file
        log.info("Found {} ready to process files for use case {} in folder {}", readyFiles.size(), useCase.getName(), folder);
        final Map<String, Throwable> errors = new HashMap<>();
        for (final Map.Entry<File, Map<String, String>> entry : readyFiles.entrySet()) {
            final File file = entry.getKey();
            final Map<String, String> tags = entry.getValue();
            if (!useCase.isSensitiveFilename()) {
                log.info("Processing file {} for use case {}", file.path(), useCase.getName());
            }
            try {
                this.processFile(useCase, file, tags);
            } catch (final MetadataException | FileSizeException | UseCaseException e) {
                log.warn("Error while processing file {} for use case {}", file.path(), useCase.getName(), e);
                // mark file as failed
                fileHandlingHelper.markFileError(file, swimDispatcherProperties.getDispatchStateTagKey(), e);
                // store exception for later notification
                errors.put(file.path(), e);
            }
        }
        return errors;
    }

    /**
     * Process a single file by executing according action (dispatch, reroute, ignore, ...)
     * and mark file as finished.
     *
     * @param useCase The use case the file was found for.
     * @param file The file to be processed.
     * @throws FileSizeException If file is above configured
     *             {@link SwimDispatcherProperties#getMaxFileSize()}
     * @throws MetadataException If metadata file required but could not be loaded
     * @throws UseCaseException If use case can't be resolved in reroute action.
     */
    protected void processFile(final UseCase useCase, final File file, final Map<String, String> tags)
            throws FileSizeException, MetadataException, UseCaseException {
        // check file size
        if (file.size() > swimDispatcherProperties.getMaxFileSize()) {
            final String message = String.format("File %s too large. %d > %d", file.path(), file.size(), swimDispatcherProperties.getMaxFileSize());
            throw new FileSizeException(message);
        }
        // execute action
        final String action = tags.getOrDefault(swimDispatcherProperties.getDispatchActionTagKey(), ACTION_DISPATCH);
        switch (action) {
        case ACTION_DELETE, ACTION_IGNORE:
            this.finishFile(useCase, file);
            break;
        case ACTION_REROUTE:
            this.rerouteFileToUseCase(useCase, file, tags);
            break;
        default:
            this.dispatchFile(useCase, file);
        }
        // update metric
        final String destination = ACTION_DISPATCH.equals(action) ? useCase.getDestinationBinding() : action;
        dispatchMeter.incrementDispatched(useCase.getName(), destination);
    }

    /**
     * Dispatch a file to the use case destination binding.
     *
     * @param useCase The use case of the file.
     * @param file The file.
     * @throws MetadataException If metadata file required but could not be loaded.
     */
    protected void dispatchFile(final UseCase useCase, final File file) throws MetadataException {
        // check metadata file exists if required
        final String metadataPresignedUrl;
        if (useCase.isRequiresMetadata()) {
            // build metadata file path
            final String metadataPath = file.getMetadataFilePath();
            // continue if not existing
            if (!fileSystemOutPort.fileExists(file.bucket(), metadataPath)) {
                final String message = String.format("Metadata file %s missing", metadataPath);
                throw new MetadataException(message);
            }
            metadataPresignedUrl = fileSystemOutPort.getPresignedUrl(file.bucket(), metadataPath);
        } else {
            metadataPresignedUrl = null;
        }
        // dispatch file
        final String presignedUrl = fileSystemOutPort.getPresignedUrl(file.bucket(), file.path());
        fileDispatchingOutPort.dispatchFile(useCase.getDestinationBinding(), useCase.getName(), presignedUrl, metadataPresignedUrl);
        // mark file as dispatched
        fileSystemOutPort.tagFile(file.bucket(), file.path(), Map.of(
                swimDispatcherProperties.getDispatchStateTagKey(),
                swimDispatcherProperties.getDispatchedStateTagValue()));
    }

    /**
     * Tag file and move to finished directory.
     *
     * @param useCase The use case of the file.
     * @param file The file to finish.
     */
    protected void finishFile(final UseCase useCase, final File file) {
        // finish metadata file if required and exists
        if (useCase.isRequiresMetadata() && this.fileSystemOutPort.fileExists(file.bucket(), file.getMetadataFilePath())) {
            this.fileHandlingHelper.finishFile(useCase, file.bucket(), file.getMetadataFilePath());
        }
        // finish file
        this.fileHandlingHelper.finishFile(useCase, file.bucket(), file.path());
    }

    /**
     * Reroute a file to another use case.
     *
     * @param useCase The use case of the file.
     * @param file The file to reroute
     * @param tags The tags of the file. Used for resolving target.
     * @throws UseCaseException If target use case can't be resolved.
     */
    protected void rerouteFileToUseCase(final UseCase useCase, final File file, final Map<String, String> tags) throws UseCaseException {
        // resolve target use case
        final String targetUseCaseName = tags.get(ACTION_REROUTE_DESTINATION_TAG_KEY);
        if (targetUseCaseName == null) {
            final String message = String.format("Action reroute but no target use case found for file %s in use case %s ", file.path(), useCase.getName());
            throw new IllegalStateException(message);
        }
        final UseCase targetUseCase = swimDispatcherProperties.findUseCase(targetUseCaseName);
        // copy file to target use case
        final String rawPath = useCase.getRawPath(swimDispatcherProperties, file.path());
        final String destPath = String.format("%s/from_%s/%s", targetUseCase.getDispatchPath(swimDispatcherProperties), useCase.getName(), rawPath);
        fileSystemOutPort.copyFile(file.bucket(), file.path(), targetUseCase.getBucket(), destPath);
        // finish file
        this.finishFile(useCase, file);
    }

}
