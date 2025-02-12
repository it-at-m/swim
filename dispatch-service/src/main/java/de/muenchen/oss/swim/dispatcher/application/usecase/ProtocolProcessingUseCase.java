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
import de.muenchen.oss.swim.dispatcher.domain.exception.ProtocolException;
import de.muenchen.oss.swim.dispatcher.domain.model.File;
import de.muenchen.oss.swim.dispatcher.domain.model.UseCase;
import de.muenchen.oss.swim.dispatcher.domain.model.protocol.ProtocolEntry;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProtocolProcessingUseCase implements ProtocolProcessingInPort {
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
            final Map<File, Map<String, String>> protocolFiles = fileSystemOutPort.getMatchingFiles(
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
            final List<File> folderFiles = new ArrayList<>(
                    fileSystemOutPort.getMatchingFiles(file.bucket(), file.getParentPath(), false, FILE_EXTENSION_PDF, Map.of(),
                            Map.of()).keySet());
            // load files in finished folder
            final String finishedPath = useCase.getFinishedPath(swimDispatcherProperties, file.getParentPath());
            folderFiles.addAll(fileSystemOutPort.getMatchingFiles(file.bucket(), finishedPath, false, FILE_EXTENSION_PDF, Map.of(), Map.of()).keySet());
            // parse files
            final Set<String> folderFileNames = folderFiles.stream().map(File::getFileName).collect(Collectors.toSet());
            // compare files with protocol
            final List<String> missingInProtocol = new ArrayList<>(folderFileNames);
            missingInProtocol.removeAll(protocolFileNames);
            final boolean isMissingInProtocol = !missingInProtocol.isEmpty();
            final List<String> missingFiles = new ArrayList<>(protocolFileNames);
            missingFiles.removeAll(folderFileNames);
            final boolean isMissingFiles = !missingFiles.isEmpty();
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
            final String matchState;
            if (isMissingInProtocol && isMissingFiles) {
                matchState = "missingInProtocolAndFiles";
            } else if (isMissingInProtocol) {
                matchState = "missingInProtocol";
            } else if (isMissingFiles) {
                matchState = "missingFiles";
            } else {
                matchState = "correct";
            }
            fileSystemOutPort.tagFile(file.bucket(), file.path(), Map.of(
                    swimDispatcherProperties.getProtocolStateTagKey(), swimDispatcherProperties.getProtocolProcessedStateTageValue(),
                    swimDispatcherProperties.getProtocolMatchTagKey(), matchState));
            // move protocol
            final String destPath = useCase.getFinishedProtocolPath(swimDispatcherProperties, file.path());
            fileSystemOutPort.moveFile(file.bucket(), file.path(), destPath);
        } catch (final ProtocolException | IOException | DataIntegrityViolationException e) {
            log.warn("Error file processing {} for use case {}", file.path(), useCase.getName(), e);
            fileHandlingHelper.markFileError(file, swimDispatcherProperties.getProtocolStateTagKey(), e);
            notificationOutPort.sendProtocolError(useCase.getMailAddresses(), useCase.getName(), file.path(), e);
        }
    }
}
