package de.muenchen.oss.swim.dispatcher.application.usecase;

import de.muenchen.oss.swim.dispatcher.application.port.in.DispatcherInPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileDispatchingOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.ReadProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.StoreProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.exception.FileSizeException;
import de.muenchen.oss.swim.dispatcher.domain.exception.MetadataException;
import de.muenchen.oss.swim.dispatcher.domain.exception.ProtocolException;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispatcherUseCase implements DispatcherInPort {
    private final SwimDispatcherProperties swimDispatcherProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final FileDispatchingOutPort fileDispatchingOutPort;
    private final ReadProtocolOutPort readProtocolOutPort;
    private final StoreProtocolOutPort storeProtocolOutPort;
    private final NotificationOutPort notificationOutPort;

    private static final String FILE_EXTENSION_PDF = "pdf";
    private static final String FILE_EXTENSION_CSV = "csv";

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
     * See {@link DispatcherUseCase#processFile(UseCase, File)}.
     *
     * @param useCase The bucket of the folder.
     * @param folder The full path of the folder.
     * @return Error which occurred during processing (Key: file path, value: error).
     */
    private @NotNull
    Map<String, Throwable> processDirectory(final UseCase useCase, final String folder, final boolean recursive) {
        final List<File> readyFiles = fileSystemOutPort.getMatchingFiles(
                useCase.getBucket(),
                folder,
                recursive,
                FILE_EXTENSION_PDF,
                useCase.getRequiredTags(),
                swimDispatcherProperties.getDispatchExcludeTags());
        // for each file
        log.info("Found {} ready to process files for use case {} in folder {}", readyFiles.size(), useCase.getName(), folder);
        final Map<String, Throwable> errors = new HashMap<>();
        for (final File file : readyFiles) {
            if (!useCase.isSensitiveFilename()) {
                log.info("Processing file {} for use case {}", file.path(), useCase.getName());
            }
            try {
                this.processFile(useCase, file);
            } catch (final MetadataException | FileSizeException e) {
                log.warn("Error while processing file {} for use case {}", file.path(), useCase.getName(), e);
                // mark file as failed
                markFileError(file, swimDispatcherProperties.getDispatchStateTagKey(), e);
                // store exception for later notification
                errors.put(file.path(), e);
            }
        }
        return errors;
    }

    /**
     * Process a single file.
     * Validate metadata exists if required by use case.
     * Dispatch file for further processing.
     * Mark file as dispatched.
     *
     * @param useCase The use case the file was found for.
     * @param file The file to be processed.
     * @throws FileSizeException If file is above configured
     *             {@link SwimDispatcherProperties#getMaxFileSize()}
     * @throws MetadataException If metadata file required but could not be loaded
     */
    protected void processFile(final UseCase useCase, final File file) throws FileSizeException, MetadataException {
        // check file size
        if (file.size() > swimDispatcherProperties.getMaxFileSize()) {
            final String message = String.format("File %s too large. %d > %d", file.path(), file.size(), swimDispatcherProperties.getMaxFileSize());
            throw new FileSizeException(message);
        }
        // check metadata file exists if required
        final String metadataPresignedUrl;
        if (useCase.isRequiresMetadata()) {
            // build metadata file path
            final String metadataPath = String.format("%s/%s.json", file.getParentPath(), file.getFileNameWithoutExtension());
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

    @Override
    public void triggerProtocolProcessing() {
        log.info("Starting protocol processing");
        for (final UseCase useCase : swimDispatcherProperties.getUseCases()) {
            // get protocols not already processed
            final List<File> protocolFiles = fileSystemOutPort.getMatchingFiles(
                    useCase.getBucket(),
                    useCase.getDispatchPath(swimDispatcherProperties),
                    useCase.isRecursive(),
                    FILE_EXTENSION_CSV,
                    Map.of(),
                    swimDispatcherProperties.getProtocolExcludeTags());
            // for each file
            log.info("Found {} protocol files for use case {}", protocolFiles.size(), useCase.getName());
            for (final File file : protocolFiles) {
                log.info("Processing protocol {} for use case {}", file.path(), useCase.getName());
                // skip file if name not matching parent folder
                if (!file.getParentName().equals(file.getFileNameWithoutExtension())) {
                    final String message = String.format("Found CSV not matching folder name: %s in bucket %s", file.path(), file.bucket());
                    final IllegalStateException exception = new IllegalStateException(message);
                    log.warn(message);
                    markFileError(file, swimDispatcherProperties.getProtocolStateTagKey(), exception);
                    notificationOutPort.sendProtocolError(useCase.getMailAddresses(), useCase.getName(), file.path(), exception);
                    continue;
                }
                // process protocol
                this.processProtocolFile(useCase, file);
            }
        }
        log.info("Finished protocol processing");
    }

    /**
     * Process a single protocol file.
     * Parse file and validate.
     *
     * @param useCase The use case the protocol was loaded for.
     * @param file The protocol file.
     */
    protected void processProtocolFile(final UseCase useCase, final File file) {
        try {
            // load protocol
            final List<ProtocolEntry> protocolEntries = readProtocolOutPort.loadProtocol(file.bucket(), file.path());
            final List<String> protocolFileNames = protocolEntries.stream().map(ProtocolEntry::fileName).toList();
            // load files in folder
            final List<File> folderFiles = new ArrayList<>(
                    fileSystemOutPort.getMatchingFiles(file.bucket(), file.getParentPath(), false, FILE_EXTENSION_PDF, Map.of(),
                            Map.of()));
            // load files in finished folder
            final String finishedPath = useCase.getFinishedPath(swimDispatcherProperties, file.getParentPath());
            folderFiles.addAll(fileSystemOutPort.getMatchingFiles(file.bucket(), finishedPath, false, FILE_EXTENSION_PDF, Map.of(), Map.of()));
            // parse files
            final Set<String> folderFileNames = folderFiles.stream().map(File::getFileName).collect(Collectors.toSet());
            // compare files with protocol
            final List<String> missingInProtocol = new ArrayList<>(folderFileNames);
            missingInProtocol.removeAll(protocolFileNames);
            final List<String> missingFiles = new ArrayList<>(protocolFileNames);
            missingFiles.removeAll(folderFileNames);
            // write protocol to db
            final String protocolName = useCase.getRawPath(swimDispatcherProperties, file.path());
            storeProtocolOutPort.deleteProtocol(useCase.getName(), protocolName);
            storeProtocolOutPort.storeProtocol(useCase.getName(), protocolName, protocolEntries);
            // send protocol
            try (InputStream inputStream = fileSystemOutPort.readFile(file.bucket(), file.path())) {
                notificationOutPort.sendProtocol(useCase.getMailAddresses(), useCase.getName(), protocolName, inputStream, missingFiles,
                        missingInProtocol);
            }
            // tag protocol as processed
            fileSystemOutPort.tagFile(file.bucket(), file.path(), Map.of(
                    swimDispatcherProperties.getProtocolStateTagKey(), swimDispatcherProperties.getProtocolProcessedStateTageValue()));
            // move protocol
            final String destPath = useCase.getFinishedPath(swimDispatcherProperties, file.path());
            fileSystemOutPort.moveFile(file.bucket(), file.path(), destPath);
        } catch (final ProtocolException | IOException | DataIntegrityViolationException e) {
            log.warn("Error file processing {} for use case {}", file.path(), useCase.getName(), e);
            markFileError(file, swimDispatcherProperties.getProtocolStateTagKey(), e);
            notificationOutPort.sendProtocolError(useCase.getMailAddresses(), useCase.getName(), file.path(), e);
        }
    }

    /**
     * Mark a file that has led to an exception while processing.
     *
     * @param file File that throw an error.
     * @param stateTagKey The key of the state tag.
     * @param e The exception that was thrown.
     */
    protected void markFileError(final File file, final String stateTagKey, final Exception e) {
        // escape illegal chars from message
        final String escapedMessage = e.getMessage().replaceAll("[^\\w .-]", " ");
        // shorten exception message for tag value max 256 chars
        final String shortenedExceptionMessage = escapedMessage.length() > 256 ? escapedMessage.substring(0, 256) : escapedMessage;
        fileSystemOutPort.tagFile(file.bucket(), file.path(), Map.of(
                stateTagKey, swimDispatcherProperties.getErrorStateValue(),
                swimDispatcherProperties.getErrorClassTagKey(), e.getClass().getName(),
                swimDispatcherProperties.getErrorMessageTagKey(), shortenedExceptionMessage));
    }
}
