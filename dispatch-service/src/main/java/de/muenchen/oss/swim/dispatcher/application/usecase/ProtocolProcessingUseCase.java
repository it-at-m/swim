package de.muenchen.oss.swim.dispatcher.application.usecase;

import static de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper.FILE_EXTENSION_CSV;
import static de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper.FILE_EXTENSION_PDF;

import de.muenchen.oss.swim.dispatcher.application.port.in.ProtocolProcessingInPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.ReadProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.application.port.out.StoreProtocolOutPort;
import de.muenchen.oss.swim.dispatcher.application.usecase.helper.FileHandlingHelper;
import de.muenchen.oss.swim.dispatcher.configuration.SwimDispatcherProperties;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProtocolProcessingUseCase implements ProtocolProcessingInPort {
    public static final String MATCH_MISSING_IN_PROTOCOL_AND_FILES = "missingInProtocolAndFiles";
    public static final String MATCH_MISSING_IN_PROTOCOL = "missingInProtocol";
    public static final String MATCH_MISSING_FILES = "missingFiles";
    public static final String MATCH_CORRECT = "correct";

    private final SwimDispatcherProperties swimDispatcherProperties;
    private final FileSystemOutPort fileSystemOutPort;
    private final ReadProtocolOutPort readProtocolOutPort;
    private final StoreProtocolOutPort storeProtocolOutPort;
    private final NotificationOutPort notificationOutPort;
    private final FileHandlingHelper fileHandlingHelper;

    @Override
    public void triggerProtocolProcessing() {
        log.info("Starting protocol processing");
        for (final UseCase useCase : swimDispatcherProperties.getUseCases()) {
            // get protocols not already processed
            final Map<File, Map<String, String>> protocolFiles = fileSystemOutPort.getMatchingFilesWithTags(
                    useCase.getBucket(),
                    useCase.getDispatchPath(swimDispatcherProperties),
                    useCase.isRecursive(),
                    FILE_EXTENSION_CSV,
                    Map.of(),
                    swimDispatcherProperties.getProtocolExcludeTags());
            // for each file
            log.info("Found {} protocol files for use case {}", protocolFiles.size(), useCase.getName());
            for (final File file : protocolFiles.keySet()) {
                log.info("Processing protocol {} for use case {}", file.path(), useCase.getName());
                // skip file if name not matching parent folder
                if (!file.getParentName().equals(file.getFileNameWithoutExtension())) {
                    final String message = String.format("Found CSV not matching folder name: %s in bucket %s", file.path(), file.bucket());
                    final IllegalStateException exception = new IllegalStateException(message);
                    log.warn(message);
                    fileHandlingHelper.markFileError(file, swimDispatcherProperties.getProtocolStateTagKey(), exception);
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
            final Set<File> inProcessFiles = fileSystemOutPort
                    .getMatchingFilesWithTags(file.bucket(), file.getParentPath(), false, FILE_EXTENSION_PDF, Map.of(),
                            Map.of())
                    .keySet();
            final List<File> folderFiles = new ArrayList<>(inProcessFiles);
            // load files in finished folder
            final String finishedPath = useCase.getFinishedPath(swimDispatcherProperties, file.getParentPath());
            folderFiles.addAll(fileSystemOutPort.getMatchingFilesWithTags(file.bucket(), finishedPath, false, FILE_EXTENSION_PDF, Map.of(), Map.of()).keySet());
            // parse files
            final Pattern filenameIgnorePattern = StringUtils.isNotBlank(useCase.getProtocolIgnorePattern())
                    ? Pattern.compile(useCase.getProtocolIgnorePattern())
                    : null;
            final Set<String> folderFileNames = folderFiles.stream()
                    // filter ignored files
                    .filter(i -> {
                        // filter out files if pattern is set
                        if (filenameIgnorePattern != null) {
                            final String fileNameWithoutExtension = i.getFileNameWithoutExtension();
                            return !filenameIgnorePattern.matcher(fileNameWithoutExtension).matches();
                        }
                        return true;
                    })
                    // map to filename
                    .map(File::getFileName).collect(Collectors.toSet());
            // compare files with protocol
            final List<String> missingInProtocol = new ArrayList<>(folderFileNames);
            missingInProtocol.removeAll(protocolFileNames);
            final boolean isMissingInProtocol = !missingInProtocol.isEmpty();
            final List<String> missingFiles = new ArrayList<>(protocolFileNames);
            missingFiles.removeAll(folderFileNames);
            final boolean isMissingFiles = !missingFiles.isEmpty();
            // tag files if enabled and protocol correct
            if (useCase.isTagProtocolProcessed() && !isMissingFiles && !isMissingInProtocol) {
                for (final File fileToTag : inProcessFiles) {
                    fileSystemOutPort.tagFile(fileToTag.bucket(), fileToTag.path(), Map.of(
                            swimDispatcherProperties.getDispatchStateTagKey(), swimDispatcherProperties.getProtocolProcessedFilesStateTagValue()));
                }
            }
            // write protocol to db
            final String protocolName = useCase.getRawPath(swimDispatcherProperties, file.path());
            storeProtocolOutPort.deleteProtocol(useCase.getName(), protocolName);
            storeProtocolOutPort.storeProtocol(useCase.getName(), protocolName, protocolEntries);
            // send protocol
            try (InputStream inputStream = fileSystemOutPort.readFile(file.bucket(), file.path())) {
                notificationOutPort.sendProtocol(useCase.getMailAddresses(), useCase.getName(), protocolName, inputStream, missingFiles,
                        missingInProtocol);
            }
            // mark protocol as finished
            markProtocolAsFinished(useCase, file, isMissingInProtocol, isMissingFiles);
        } catch (final IOException | RuntimeException e) {
            log.warn("Error file processing {} for use case {}", file.path(), useCase.getName(), e);
            fileHandlingHelper.markFileError(file, swimDispatcherProperties.getProtocolStateTagKey(), e);
            notificationOutPort.sendProtocolError(useCase.getMailAddresses(), useCase.getName(), file.path(), e);
        }
    }

    /**
     * Mark protocol as finished (tag, move to processed dir)
     *
     * @param useCase The use case of the protocol.
     * @param file The protocol file.
     * @param isMissingInProtocol If there are missing files in the protocol.
     * @param isMissingFiles If there are files missing in the filesystem.
     */
    private void markProtocolAsFinished(final UseCase useCase, final File file, final boolean isMissingInProtocol, final boolean isMissingFiles) {
        // tag protocol as processed
        final String matchState;
        if (isMissingInProtocol && isMissingFiles) {
            matchState = MATCH_MISSING_IN_PROTOCOL_AND_FILES;
        } else if (isMissingInProtocol) {
            matchState = MATCH_MISSING_IN_PROTOCOL;
        } else if (isMissingFiles) {
            matchState = MATCH_MISSING_FILES;
        } else {
            matchState = MATCH_CORRECT;
        }
        fileSystemOutPort.tagFile(file.bucket(), file.path(), Map.of(
                swimDispatcherProperties.getProtocolStateTagKey(), swimDispatcherProperties.getProtocolProcessedStateTagValue(),
                swimDispatcherProperties.getProtocolMatchTagKey(), matchState));
        // move protocol
        final String destPath = useCase.getFinishedProtocolPath(swimDispatcherProperties, file.path());
        fileSystemOutPort.moveFile(file.bucket(), file.path(), destPath);
    }
}
