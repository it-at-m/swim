package de.muenchen.oss.swim.dispatcher.application.usecase;

import static de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper.FILE_EXTENSION_PDF;
import static de.muenchen.oss.swim.dispatcher.domain.model.DispatchAction.DISPATCH;

import de.muenchen.oss.swim.dispatcher.application.port.in.DispatcherInPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.DispatchActionsHelper;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.GroupingHelper;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.ValidationHelper;
import de.muenchen.oss.swim.dispatcher.configuration.DispatchMeter;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.MetadataException;
import de.muenchen.oss.swim.dispatcher.domain.exception.UseCaseException;
import de.muenchen.oss.swim.dispatcher.domain.model.DispatchAction;
import de.muenchen.oss.swim.dispatcher.domain.model.ErrorContainer;
import de.muenchen.oss.swim.dispatcher.domain.model.FileGroup;
import de.muenchen.oss.swim.dispatcher.domain.model.FileWithMetadata;
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
    private final SwimDispatcherProperties swimDispatcherProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final NotificationOutPort notificationOutPort;
    private final FileHandlingHelper fileHandlingHelper;
    private final DispatchMeter dispatchMeter;
    private final GroupingHelper groupingHelper;
    private final DispatchActionsHelper dispatchActionsHelper;
    private final ValidationHelper validationHelper;

    @Override
    public void triggerDispatching() {
        log.info("Starting dispatching");
        for (final UseCase useCase : swimDispatcherProperties.getUseCases()) {
            final Map<String, Throwable> errors = new HashMap<>();
            try {
                // handle files directly in directory
                final String dispatchPath = useCase.getDispatchPath(swimDispatcherProperties);
                errors.putAll(this.processDirectory(useCase, dispatchPath, false));
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
            } catch (final Exception e) {
                log.error("Processing of use case {} failed", useCase.getName(), e);
                final Map<String, Throwable> enrichedErrors = new HashMap<>();
                enrichedErrors.put("!!! USE CASE PROCESSING !!!", new UseCaseException(
                        "Unexpected error during dispatching, use case was not processed completely. Error: %s".formatted(e.getMessage()), e));
                enrichedErrors.putAll(errors);
                notificationOutPort.sendDispatchErrors(useCase.getMailAddresses(), useCase.getName(), enrichedErrors);
            }
        }
        log.info("Finished dispatching");
    }

    /**
     * Process all files inside a folder recursively.
     * See {@link DispatcherUseCase#processFileGroup}.
     *
     * @param useCase The bucket of the folder.
     * @param folder The full path of the folder.
     * @return Error which occurred during processing (Key: file path, value: error).
     */
    private @NotNull
    Map<String, Throwable> processDirectory(final UseCase useCase, final String folder, final boolean recursive) {
        // find files
        final List<FileWithMetadata> readyFiles = fileSystemOutPort.getMatchingFilesWithTags(
                useCase.getBucket(),
                folder,
                recursive,
                FILE_EXTENSION_PDF,
                useCase.getRequiredTags(),
                swimDispatcherProperties.getDispatchExcludeTags());
        log.info("Found {} ready to process files for use case {} in folder {}", readyFiles.size(), useCase.getName(), folder);
        // group and validate files
        final Map<String, FileGroup> groupedFiles = groupingHelper.groupFiles(readyFiles);
        final ErrorContainer<Map<String, FileGroup>> validatedAndFilterGroupedFiles = validationHelper.validateAndFilterGroupedFiles(
                useCase, groupedFiles);
        final Map<String, Throwable> errors = new HashMap<>(validatedAndFilterGroupedFiles.errors());
        // for each file group
        for (final Map.Entry<String, FileGroup> entry : validatedAndFilterGroupedFiles.value().entrySet()) {
            final String baseFileName = entry.getKey();
            final FileGroup fileGroup = entry.getValue();
            final List<FileWithMetadata> files = fileGroup.getFiles();
            try {
                this.processFileGroup(useCase, baseFileName, fileGroup);
            } catch (final MetadataException | UseCaseException | RuntimeException e) {
                log.warn("Error while processing {} file(s) {} for use case {}", files.size(), baseFileName, useCase.getName(), e);
                // mark file as failed
                for (final FileWithMetadata file : files) {
                    fileHandlingHelper.markFileError(file.reference(), swimDispatcherProperties.getDispatchStateTagKey(), e);
                }
                // store exception for later notification
                for (final FileWithMetadata file : files) {
                    errors.put(file.reference().path(), e);
                }
            }
        }
        return errors;
    }

    /**
     * Process a file group by executing according action (dispatch, reroute, ignore, ...)
     * and mark files as finished.
     * If multiple files only dispatch is supported.
     *
     * @param useCase The use case the files were found for.
     * @param baseFileName The files name without the split suffix or file extension.
     * @param fileGroup The files to be processed.
     * @throws MetadataException If metadata file required but could not be loaded
     * @throws UseCaseException If use case can't be resolved in reroute action.
     */
    protected void processFileGroup(final UseCase useCase, final String baseFileName, final FileGroup fileGroup)
            throws MetadataException, UseCaseException {
        final List<FileWithMetadata> files = fileGroup.getFiles();
        if (!useCase.isSensitiveFilename()) {
            log.info("Processing {} file(s) {} for use case {}", files.size(), baseFileName, useCase.getName());
        }
        // resolve action
        final DispatchAction action = dispatchActionsHelper.resolveDispatchAction(files.getFirst().tags());
        // fail if multiple files but no dispatch
        if (action != DISPATCH && fileGroup.isMulti()) {
            throw new IllegalStateException("Other actions than DISPATCH are not allowed for multiple files");
        }
        // execute action
        String actionName = action.name();
        switch (action) {
        case DELETE, IGNORE:
            this.fileHandlingHelper.finishFile(useCase, files.getFirst().reference());
            break;
        case REROUTE:
            this.dispatchActionsHelper.rerouteFileToUseCase(useCase, files.getFirst().reference(), files.getFirst().tags());
            break;
        case DISPATCH:
            final String destinationBinding = dispatchActionsHelper.resolveDestinationBinding(useCase, fileGroup);
            this.dispatchActionsHelper.dispatchFileGroup(useCase, fileGroup, destinationBinding);
            // use destination binding as actionName for more specific metrics
            actionName = destinationBinding;
            break;
        }
        // update metric
        dispatchMeter.incrementDispatched(useCase.getName(), actionName);
    }
}
